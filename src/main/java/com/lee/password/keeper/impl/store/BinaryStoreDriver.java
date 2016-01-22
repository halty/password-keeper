package com.lee.password.keeper.impl.store;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.impl.store.binary.BinaryPassword;
import com.lee.password.keeper.impl.store.binary.BinaryWebsite;
import com.lee.password.keeper.impl.store.binary.ChangedOperation;
import com.lee.password.keeper.impl.store.binary.ChangedOperation.OP;

public class BinaryStoreDriver implements StoreDriver {

	private static final String DATA_FILE = "binary.store.pk";
	
	private static final byte[] MAGIC = "bpsd".getBytes(CHARSET);
	
	/** secret block size defined by private crypto key **/
	private final int secretBlockSize;
	
	/** encryption/decryption driver  **/
	private CryptoDriver cryptoDriver;
	
	/** storage resources **/
	private File storePath;
	private RandomAccessFile storeMappedFile;
	private FileChannel storeChannel;
	private FileLock storeLock;
	
	/** metadata **/
	private static final long META_DATA_LEN = 4 + 8 + 4 + 8;
	/** the number of password **/
	private int passwordCount;
	/** the offset of password in channel **/
	private long passwordOffset;
	/** the number of website **/
	private int websiteCount;
	/** the offset of website in channel **/
	private long websiteOffset;
	
	/** index password and website **/
	private SortedMap<Long, List<BinaryPassword>> websiteIdPwdMap;
	private Map<String, List<BinaryPassword>> usernamePwdMap;
	private SortedMap<Long, BinaryWebsite> websiteIdMap;
	private Map<String, BinaryWebsite> websiteKeywordMap;
	
	/** change operation queue **/
	private Deque<ChangedOperation<?>> undoQueue;
	private Deque<ChangedOperation<?>> redoQueue;
	
	/** closed flag **/
	private boolean isClosed;
	
	public BinaryStoreDriver(String dataDir, CryptoDriver cryptoDriver, int secretBlockSize) {
		try {
			this.secretBlockSize = secretBlockSize;
			this.cryptoDriver = cryptoDriver;
			this.storePath = createIfNotExisted(dataDir);
			this.storeMappedFile = new RandomAccessFile(storePath, "rw");
			this.storeChannel = storeMappedFile.getChannel();
			this.storeLock = storeChannel.lock();
			
			init();
		}catch(Exception e) {
			release();
			throw new IllegalStateException(
					String.format("failed to init binary store driver from data directory {%s}: %s", dataDir, e.getMessage()),
					e);
		}
	}
	
	private static File createIfNotExisted(String dataDir) {
		File dir = initDataDir(dataDir);
		File dataFile = new File(dir, DATA_FILE);
		if(!dataFile.exists()) {
			try {
				dataFile.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException("failed to create data file: "+dataFile);
			}
		}
		return dataFile;
	}
	
	private static File initDataDir(String dataDir) {
		File dir = new File(dataDir);
		if(!dir.exists()) {
			if(!dir.mkdirs()) {
				throw new IllegalArgumentException(dataDir + " create failed");
			}
		}else {
			if(!dir.isDirectory()) {
				throw new IllegalArgumentException(dataDir + " is not a directory");
			}
		}
		return dir;
	}
	
	private void init() throws Exception {
		if(storePath.length() == 0) { // created new file
			initStore();
		}else {
			loadStore();
		}
		initUndoAndRedoDeque();
		isClosed = false;
	}
	
	/** init an data file with metadata **/
	private void initStore() throws Exception {
		FileOutputStream fos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(storePath);
			dos = new DataOutputStream(new BufferedOutputStream(fos));
			long offset = MAGIC.length + META_DATA_LEN;
			dos.write(MAGIC);
			dos.writeInt(0);	// passwordCount
			dos.writeLong(offset);	// passwordOffset
			dos.writeInt(0);	// websiteCount
			dos.writeLong(offset);	// websiteOffset
			dos.flush();
			
			this.passwordCount = 0;
			this.passwordOffset = offset;
			this.websiteCount = 0;
			this.websiteOffset = offset;
			
			this.websiteIdPwdMap = new TreeMap<Long, List<BinaryPassword>>();
			this.usernamePwdMap = new HashMap<String, List<BinaryPassword>>();
			this.websiteIdMap = new TreeMap<Long, BinaryWebsite>();
			this.websiteKeywordMap = new HashMap<String, BinaryWebsite>();
		}finally {
			try {
				if(dos != null) { dos.close(); }
				if(fos != null) { fos.close(); }
			}catch(Exception e) {/** can't do anything **/ throw e; }
		}
	}
	
	private void initUndoAndRedoDeque() {
		this.undoQueue = new LinkedList<ChangedOperation<?>>();
		this.redoQueue = new LinkedList<ChangedOperation<?>>();
	}
	
	/** load all the data from file to memory **/
	private void loadStore() throws Exception {
		matchMagicAndLoadMetadata();
		loadWebsite();
		loadPasswords();
	}
	
	private void matchMagicAndLoadMetadata() throws Exception {
		FileInputStream fis = null;
		DataInputStream dis = null;
		try {
			fis = new FileInputStream(storePath);
			dis = new DataInputStream(new BufferedInputStream(fis));
			matchMagic(dis);
			loadMetadata(dis);
		}finally {
			try {
				if(dis != null) { dis.close(); }
				if(fis != null) { fis.close(); }
			}catch(Exception e) {/** can't do anything **/ throw e; }
		}
	}
	
	private void matchMagic(DataInputStream dis) throws IOException {
		int expectedLen = MAGIC.length;
		byte[] magic = new byte[expectedLen];
		int len = dis.read(magic);
		if(len != expectedLen) {
			throw new IllegalStateException("incorrect magic number size from store path: "+storePath);
		}
		if(!Arrays.equals(magic, MAGIC)) {
			throw new IllegalStateException("wrong magic number from store path: "+storePath);
		}
	}
	
	private void loadMetadata(DataInputStream dis) throws IOException {
		this.passwordCount = dis.readInt();
		this.passwordOffset = dis.readLong();
		this.websiteCount = dis.readInt();
		this.websiteOffset = dis.readLong();
	}
	
	private void loadWebsite() throws IOException {
		long position = this.websiteOffset;
		int count = this.websiteCount;
		long size = count * BinaryWebsite.occupiedSize();
		ByteBuffer websitesBuffer = storeChannel.map(MapMode.READ_ONLY, position, size).asReadOnlyBuffer();
		
		long lastWebsiteId = Long.MIN_VALUE;
		int totalPasswordCount = 0;
		for(int i=0; i<count; i++) {
			BinaryWebsite website = BinaryWebsite.load(websitesBuffer);
			long websiteId = website.websiteId();
			if(lastWebsiteId > websiteId) {
				// website data placed order by website id asc in store file
				throw new IllegalStateException("incorrect website data order from store path: "+storePath);
			}
			websiteIdMap.put(websiteId, website);
			websiteKeywordMap.put(website.keyword(), website);
			lastWebsiteId = websiteId;
			totalPasswordCount += website.count();
		}
		
		if(totalPasswordCount != passwordCount) {
			throw new IllegalStateException("inconsistent password count from store path: "+storePath);
		}
	}
	
	private void loadPasswords() throws IOException {
		long position = this.passwordOffset;
		int count = this.passwordCount;
		long entrySize = BinaryPassword.occupiedSize(this.secretBlockSize);
		long size = count * entrySize;
		ByteBuffer passwordsBuffer = storeChannel.map(MapMode.READ_ONLY, position, size).asReadOnlyBuffer();
		
		int offset = 0;
		for(Entry<Long, BinaryWebsite> websiteEntry : websiteIdMap.entrySet()) {
			BinaryWebsite website = websiteEntry.getValue();
			if(website.offset() != offset) {
				// password data placed order by website id asc in store file
				throw new IllegalStateException("incorrect password data order from store path: "+storePath);
			}
			loadPasswordsBy(website, passwordsBuffer);
			offset += website.count() * entrySize;
		}
	}
	
	private void loadPasswordsBy(BinaryWebsite website, ByteBuffer passwordsBuffer) {
		long websiteId = website.websiteId();
		int count = website.count();
		for(int i=0; i<count; i++) {
			BinaryPassword password = BinaryPassword.load(passwordsBuffer, secretBlockSize);
			/* generally, one person who may not register multiple account on the same website,
			 * so all the password data belong to the same website don't placed in order.
			 */
			if(password.websiteId() != websiteId) {
				throw new IllegalStateException("inconsistent website id from store path: "+storePath);
			}
			put(password);
		}
	}
	
	private void put(BinaryPassword password) {
		long webisteId = password.websiteId();
		List<BinaryPassword> websiteIdPwdList = websiteIdPwdMap.get(webisteId);
		if(websiteIdPwdList == null) {
			websiteIdPwdList = new ArrayList<BinaryPassword>(3);
			websiteIdPwdMap.put(webisteId, websiteIdPwdList);
		}
		websiteIdPwdList.add(password);
		
		String username = password.username();
		List<BinaryPassword> usernamePwdList = usernamePwdMap.get(username);
		if(usernamePwdList == null) {
			usernamePwdList = new ArrayList<BinaryPassword>();
			usernamePwdMap.put(username, usernamePwdList);
		}
		usernamePwdList.add(password);
	}
	
	/** release all resources **/
	private void release() {
		try {
			if(cryptoDriver != null) { cryptoDriver.close(); cryptoDriver = null; }
			if(storeLock != null) { storeLock.release(); storeLock = null; }
			if(storeChannel != null) { storeChannel.close(); storeChannel = null; }
			if(storeMappedFile != null) { storeMappedFile.close(); storeMappedFile = null; }
		}catch(Exception e) {
			throw new IllegalStateException("failed to release resoures of "+storePath, e);
		}
	}
	
	@Override
	public Result<Website> insertWebsite(Website website) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Website> deleteWebsite(Website website) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Website> updateWebsite(Website website) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Website> selectWebsite(Website website) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<List<Website>> listWebsite() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Password> insertPassword(Password entry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Password> deletePassword(Header header) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Password> updatePassword(Password entry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Password> selectPassword(Header header) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<List<Header>> listPassword(int websiteId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<List<Header>> listPassword(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<?> undo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<?> redo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Throwable> flush() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Throwable> close() {
		// TODO Auto-generated method stub
		return null;
	}
}
