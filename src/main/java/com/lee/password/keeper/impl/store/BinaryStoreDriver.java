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
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.lee.password.keeper.api.Entity;
import com.lee.password.keeper.api.Entity.Type;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.Result.Code;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.StoreException;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.impl.InternalEntity;
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
	private Map<PasswordKey, BinaryPassword> passwordMap;
	private SortedMap<Long, List<BinaryPassword>> websiteIdPwdMap;
	private Map<String, List<BinaryPassword>> usernamePwdMap;
	private SortedMap<Long, BinaryWebsite> websiteIdMap;
	private Map<String, BinaryWebsite> websiteKeywordMap;
	
	/** change operation queue **/
	private Deque<ChangedOperation<? extends InternalEntity>> undoQueue;
	private Deque<ChangedOperation<? extends InternalEntity>> redoQueue;
	
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
			StoreException se = null;
			if(e instanceof StoreException) {
				se = (StoreException) e;
			}else {
				se = new StoreException(
					String.format("failed to init binary store driver from data directory {%s}: %s", dataDir, e.getMessage()),
					e);
			}
			throw se;
		}
	}
	
	private static File createIfNotExisted(String dataDir) {
		File dir = makeDataDir(dataDir);
		File dataFile = new File(dir, DATA_FILE);
		if(!dataFile.exists()) {
			try {
				dataFile.createNewFile();
			} catch (IOException e) {
				throw new StoreException("failed to create data file: "+dataFile);
			}
		}
		return dataFile;
	}
	
	private static File makeDataDir(String dataDir) {
		File dir = new File(dataDir);
		if(!dir.exists()) {
			if(!dir.mkdirs()) {
				throw new StoreException(dataDir + " create failed");
			}
		}else {
			if(!dir.isDirectory()) {
				throw new StoreException(dataDir + " is not a directory");
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
			
			this.passwordMap = new HashMap<PasswordKey, BinaryPassword>();
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
		this.undoQueue = new LinkedList<ChangedOperation<? extends InternalEntity>>();
		this.redoQueue = new LinkedList<ChangedOperation<? extends InternalEntity>>();
	}
	
	/** load all the data from file to memory **/
	private void loadStore() throws Exception {
		matchMagicAndLoadMetadata();
		loadWebsites();
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
			throw new StoreException("incorrect magic number size from store path: "+storePath);
		}
		if(!Arrays.equals(magic, MAGIC)) {
			throw new StoreException("wrong magic number from store path: "+storePath);
		}
	}
	
	private void loadMetadata(DataInputStream dis) {
		try {
			this.passwordCount = dis.readInt();
			this.passwordOffset = dis.readLong();
			this.websiteCount = dis.readInt();
			this.websiteOffset = dis.readLong();
		}catch(IOException e) {
			throw new StoreException("failed to load meta data from store path: "+storePath);
		}
	}
	
	private void loadWebsites() throws IOException {
		long position = this.websiteOffset;
		int count = this.websiteCount;
		this.websiteIdMap = new TreeMap<Long, BinaryWebsite>();
		this.websiteKeywordMap = new HashMap<String, BinaryWebsite>();		
		if(count == 0) { return; }
		
		long size = count * BinaryWebsite.occupiedSize();
		ByteBuffer websitesBuffer = storeChannel.map(MapMode.READ_ONLY, position, size)
				.order(ByteOrder.BIG_ENDIAN)
				.asReadOnlyBuffer();
		
		long lastWebsiteId = Long.MIN_VALUE;
		int totalPasswordCount = 0;
		for(int i=0; i<count; i++) {
			BinaryWebsite website = BinaryWebsite.load(websitesBuffer);
			long websiteId = website.websiteId();
			if(lastWebsiteId > websiteId) {
				// website data placed order by website id asc in store file
				throw new StoreException(String.format("incorrect website data order begining with %d from store path: %s",
						websiteId, storePath));
			}
			if(!put(website)) {
				throw new StoreException(String.format("conflict website with website id=%d from store path: %s",
						websiteId, storePath));
			}
			lastWebsiteId = websiteId;
			totalPasswordCount += website.count();
		}
		
		if(totalPasswordCount != passwordCount) {
			throw new StoreException("inconsistent password count from store path: "+storePath);
		}
	}
	
	private void loadPasswords() throws IOException {
		long position = this.passwordOffset;
		int count = this.passwordCount;
		this.passwordMap = new HashMap<PasswordKey, BinaryPassword>(count);
		this.websiteIdPwdMap = new TreeMap<Long, List<BinaryPassword>>();
		this.usernamePwdMap = new HashMap<String, List<BinaryPassword>>();
		if(count == 0) { return; }
		
		long entrySize = BinaryPassword.occupiedSize(this.secretBlockSize);
		long size = count * entrySize;
		ByteBuffer passwordsBuffer = storeChannel.map(MapMode.READ_ONLY, position, size)
				.order(ByteOrder.BIG_ENDIAN)
				.asReadOnlyBuffer();
		
		int offset = 0;
		for(Entry<Long, BinaryWebsite> websiteEntry : websiteIdMap.entrySet()) {
			BinaryWebsite website = websiteEntry.getValue();
			if(website.offset() != offset) {
				// password data placed order by website id asc in store file
				throw new StoreException(String.format("incorrect password data order for website id=%d from store path: %s",
						website.websiteId(), storePath));
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
				throw new StoreException(String.format("inconsistent website id=%d from store path: %s", websiteId, storePath));
			}
			if(!put(password)) {
				throw new StoreException(String.format("conflict passwords with website id=%d from store path: %s",
						websiteId, storePath));
			}
		}
	}
	
	private boolean put(BinaryPassword password) {
		long webisteId = password.websiteId();
		String username = password.username();
		PasswordKey passwordKey = new PasswordKey(webisteId, username);
		BinaryPassword old = passwordMap.put(passwordKey, password);
		if(old != null) {
			passwordMap.put(passwordKey, old);
			return false;
		}
		List<BinaryPassword> websiteIdPwdList = websiteIdPwdMap.get(webisteId);
		if(websiteIdPwdList == null) {
			websiteIdPwdList = new ArrayList<BinaryPassword>(3);
			websiteIdPwdMap.put(webisteId, websiteIdPwdList);
		}
		websiteIdPwdList.add(password);
		
		List<BinaryPassword> usernamePwdList = usernamePwdMap.get(username);
		if(usernamePwdList == null) {
			usernamePwdList = new ArrayList<BinaryPassword>();
			usernamePwdMap.put(username, usernamePwdList);
		}
		usernamePwdList.add(password);
		return true;
	}
	
	/** release all resources **/
	private void release() {
		try {
			if(cryptoDriver != null) { cryptoDriver.close(); cryptoDriver = null; }
			if(storeLock != null) { storeLock.release(); storeLock = null; }
			if(storeChannel != null) { storeChannel.close(); storeChannel = null; }
			if(storeMappedFile != null) { storeMappedFile.close(); storeMappedFile = null; }
		}catch(Exception e) {
			throw new StoreException("failed to release resoures of "+storePath, e);
		}
	}
	
	@Override
	public Result<Website> insertWebsite(Website website) {
		BinaryWebsite biWebsite = null;
		try {
			Result<BinaryWebsite> result = selectBy(website);
			if(result.isSuccess()) {
				return new Result<Website>(Code.FAIL, "already exists this website");
			}
			biWebsite = BinaryWebsite.cast(website);
		}catch(Exception e) {
			return new Result<Website>(Code.FAIL, e.getMessage());
		}
		
		if(appendInsertOperation(biWebsite)) {
			website.id(biWebsite.websiteId());
			return new Result<Website>(Code.SUCCESS, "success", website);
		}else {
			return new Result<Website>(Code.FAIL, "insert binary website internal error");
		}
	}
	
	private boolean appendInsertOperation(BinaryWebsite newWebsite) {
		if(!put(newWebsite)) { return false; }
		websiteCount++;
		undoQueue.offer(new ChangedOperation<BinaryWebsite>(null, OP.INSERT, newWebsite.copy()));
		return true;
	}
	
	private boolean put(BinaryWebsite newWebsite) {
		long websiteId = newWebsite.websiteId();
		BinaryWebsite oldWebsite = websiteIdMap.put(websiteId, newWebsite);
		if(oldWebsite != null) {
			websiteIdMap.put(websiteId, oldWebsite);
			return false;
		}
		String keyword = newWebsite.keyword();
		oldWebsite = websiteKeywordMap.put(keyword, newWebsite);
		if(oldWebsite != null) {
			websiteIdMap.remove(websiteId);
			websiteKeywordMap.put(keyword, oldWebsite);
			return false;
		}
		return true;
	}

	@Override
	public Result<Website> deleteWebsite(Website website) {
		Result<BinaryWebsite> result = selectBy(website);
		if(!result.isSuccess()) {
			return new Result<Website>(Code.FAIL, result.msg);
		}
		if(appendDeleteOperation(result.result)) {
			return new Result<Website>(Code.SUCCESS, "success", website);
		}else {
			return new Result<Website>(Code.FAIL, "delete binary website internal error");
		}
	}
	
	private boolean appendDeleteOperation(BinaryWebsite oldWebsite) {
		if(!remove(oldWebsite)) { return false; }
		websiteCount--;
		undoQueue.offer(new ChangedOperation<BinaryWebsite>(oldWebsite.copy(), OP.DELETE, null));
		return true;
	}
	
	private boolean remove(BinaryWebsite oldWebsite) {
		long websiteId = oldWebsite.websiteId();
		BinaryWebsite one = websiteIdMap.remove(websiteId);
		if(one == null) { return false; }
		String keyword = oldWebsite.keyword();
		BinaryWebsite another = websiteKeywordMap.remove(keyword);
		if(another == null) {
			websiteIdMap.put(websiteId, one);
			return false;
		}
		if(another != one) {
			websiteKeywordMap.put(keyword, another);
			websiteIdMap.put(websiteId, one);
			return false;
		}
		return true;
	}

	@Override
	public Result<Website> updateWebsite(Website website) {
		String keyword = website.keyword();
		Long websiteId = website.id();
		String url = website.url();
		if(!website.hasId() && !website.hasKeyword()) {
			return new Result<Website>(Code.FAIL, "website without mapping with keyword or id");
		}
		BinaryWebsite oldBiWebsite = null;
		BinaryWebsite newBiWebsite = null;
		if(website.hasId()) {
			BinaryWebsite biWebsite2 = websiteIdMap.get(websiteId);
			if(biWebsite2 == null) { return new Result<Website>(Code.FAIL, "website mapping with id not found"); }
			if(website.hasKeyword()) {
				BinaryWebsite biWebsite = websiteKeywordMap.get(keyword);
				if(biWebsite == null) {
					oldBiWebsite = biWebsite2.copy();
					newBiWebsite = biWebsite2.copy();
					newBiWebsite.changeKeyword(keyword);
					oldBiWebsite.markKeywordChanged();
					if(url != null && !url.isEmpty() && !biWebsite2.url().equals(url)) {
						newBiWebsite.changeUrl(url);
						oldBiWebsite.markUrlChanged();
					}
				}else {
					if(biWebsite != biWebsite2) {
						return new Result<Website>(Code.FAIL, "conflict with existed keyword");
					}
					if(url == null || url.isEmpty() || biWebsite2.url().equals(url)) {
						return new Result<Website>(Code.FAIL, "nothing changed for website, don't need update");
					}
					oldBiWebsite = biWebsite2.copy();
					newBiWebsite = biWebsite2.copy();
					newBiWebsite.changeUrl(url);
					oldBiWebsite.markUrlChanged();
				}
			}else {
				if(url == null || url.isEmpty() || biWebsite2.url().equals(url)) {
					return new Result<Website>(Code.FAIL, "nothing changed for website, don't need update");
				}
				oldBiWebsite = biWebsite2.copy();
				newBiWebsite = biWebsite2.copy();
				newBiWebsite.changeUrl(url);
				oldBiWebsite.markUrlChanged();
			}
		}else {
			BinaryWebsite biWebsite = websiteKeywordMap.get(keyword);
			if(biWebsite == null) { return new Result<Website>(Code.FAIL, "website mapping with keyword not found"); }
			if(url == null || url.isEmpty() || biWebsite.url().equals(url)) {
				return new Result<Website>(Code.FAIL, "nothing changed for website, don't need update");
			}
			oldBiWebsite = biWebsite.copy();
			newBiWebsite = biWebsite.copy();
			newBiWebsite.changeUrl(url);
			oldBiWebsite.markUrlChanged();
		}
		if(appendUpdateOperation(oldBiWebsite, newBiWebsite)) {
			return new Result<Website>(Code.SUCCESS, "success", website);
		}else {
			return new Result<Website>(Code.FAIL, "update binary website internal error");
		}
	}
	
	private boolean appendUpdateOperation(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) {
		if(!replace(oldWebsite, newWebsite)) { return false; }
		undoQueue.offer(new ChangedOperation<BinaryWebsite>(oldWebsite, OP.UPDATE, newWebsite));
		return true;
	}
	
	private boolean replace(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) {
		long websiteId = oldWebsite.websiteId();
		BinaryWebsite biWebsite = websiteIdMap.get(websiteId);
		if(biWebsite == null) { return false; }
		if(newWebsite.isKeywordChanged()) {
			String keyword = oldWebsite.keyword();
			BinaryWebsite one = websiteKeywordMap.remove(keyword);
			if(one != biWebsite) {
				websiteKeywordMap.put(keyword, one);
				return false;
			}
			String newKeyword = newWebsite.keyword();
			BinaryWebsite another = websiteKeywordMap.put(newKeyword, biWebsite);
			if(another != null) {
				websiteKeywordMap.put(newKeyword, another);
				websiteKeywordMap.put(keyword, one);
				return false;
			}
			biWebsite.keyword(newKeyword);
		}
		if(newWebsite.isUrlChanged()) { biWebsite.url(newWebsite.url()); }
		return true;
	}

	@Override
	public Result<Website> selectWebsite(Website website) {
		Result<BinaryWebsite> result = selectBy(website);
		if(!result.isSuccess()) {
			return new Result<Website>(Code.FAIL, result.msg);
		}
		Website detailWebsite = result.result.transform();
		return new Result<Website>(Code.SUCCESS, "success", detailWebsite);
	}
	
	private Result<BinaryWebsite> selectBy(Website website) {
		String keyword = website.keyword();
		long websiteId = website.id();
		if(!website.hasId() && !website.hasKeyword()) {
			return new Result<BinaryWebsite>(Code.FAIL, "website without keyword or id");
		}
		
		BinaryWebsite biWebsite = null;
		if(website.hasId()) {
			biWebsite = websiteIdMap.get(websiteId);
			if(biWebsite == null) { return new Result<BinaryWebsite>(Code.FAIL, "website mapping with id not found"); }
			if(website.hasKeyword()) {
				BinaryWebsite biWebsite2 = websiteKeywordMap.get(keyword);
				if(biWebsite != biWebsite2) {
					return new Result<BinaryWebsite>(Code.FAIL, "inconsistent website mapping with keyword and id");
				}
			}
		}else {
			biWebsite = websiteKeywordMap.get(keyword);
			if(biWebsite == null) {
				return new Result<BinaryWebsite>(Code.FAIL, "website mapping with keyword not found");
			}
		}
		return new Result<BinaryWebsite>(Code.SUCCESS, "success", biWebsite);
	}
	
	@Override
	public Result<Integer> websiteCount() {
		return new Result<Integer>(Code.SUCCESS, "success", websiteCount);
	}

	@Override
	public Result<List<Website>> listWebsite() {
		int length = websiteKeywordMap.size();
		List<Website> websiteList = new ArrayList<Website>(length);
		for(BinaryWebsite biWebiste : websiteKeywordMap.values()) {
			websiteList.add(biWebiste.transform());
		}
		return new Result<List<Website>>(Code.SUCCESS, "success", websiteList);
	}

	@Override
	public Result<Password.Header> insertPassword(Password entry, CryptoKey encryptKey) {
		BinaryPassword biPassword = null;
		try {
			Result<BinaryPassword> biPasswordResult = selectBy(entry.header());
			if(biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(Code.FAIL, "an existed entry mapping for this header");
			}
			biPasswordResult = encrypt(entry, encryptKey);
			if(!biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(biPasswordResult.code, biPasswordResult.msg);
			}
			biPassword = biPasswordResult.result;
		}catch(Exception e) {
			return new Result<Password.Header>(Code.FAIL, e.getMessage());
		}
		
		if(appendInsertOperation(biPassword)) {
			return new Result<Password.Header>(Code.SUCCESS, "success", entry.header());
		}else {
			return new Result<Password.Header>(Code.FAIL, "insert binary password internal error");
		}
	}
	
	private Result<BinaryPassword> encrypt(Password password, CryptoKey encryptKey) {
		Password.Header header = password.header();
		Password.Secret secret = password.secret();
		String pwd = secret.password();
		String kvp = secret.keyValuePairs();
		if(pwd == null || pwd.isEmpty()) { return new Result<BinaryPassword>(Code.FAIL, "pasword is empty"); }
		if(kvp == null) { kvp = ""; }
		BinaryPassword biPassword = new BinaryPassword(header.websiteId(), header.username(), header.timestamp());
		Result<byte[]> encryptedResult = cryptoDriver.encrypt(pwd.getBytes(CHARSET), encryptKey);
		if(!encryptedResult.isSuccess()) {
			return new Result<BinaryPassword>(Code.FAIL, "password encrypt failed: "+encryptedResult.msg);
		}
		biPassword.encryptedPassword(encryptedResult.result);
		encryptedResult = cryptoDriver.encrypt(kvp.getBytes(CHARSET), encryptKey);
		if(!encryptedResult.isSuccess()) {
			return new Result<BinaryPassword>(Code.FAIL, "key value pair encrypt failed: "+encryptedResult.msg);
		}
		biPassword.encryptedKeyValuePairs(encryptedResult.result);
		return new Result<BinaryPassword>(Code.SUCCESS, "success", biPassword);
	}
	
	private boolean appendInsertOperation(BinaryPassword password) {
		if(!put(password)) { return false; }
		passwordCount++;
		undoQueue.offer(new ChangedOperation<BinaryPassword>(null, OP.INSERT, password.copy()));
		return true;
	}

	@Override
	public Result<Password.Header> deletePassword(Password.Header header) {
		Result<BinaryPassword> biPasswordResult = selectBy(header);
		if(!biPasswordResult.isSuccess()) {
			return new Result<Password.Header>(Code.FAIL, biPasswordResult.msg);
		}
		if(appendDeleteOperation(biPasswordResult.result)) {
			return new Result<Password.Header>(Code.SUCCESS, "success", header);
		}else {
			return new Result<Password.Header>(Code.FAIL, "delete binary password internal error");
		}
	}
	
	private boolean appendDeleteOperation(BinaryPassword password) {
		if(!remove(password)) { return false; }
		passwordCount--;
		undoQueue.offer(new ChangedOperation<BinaryPassword>(password.copy(), OP.DELETE, null));
		return true;
	}
	
	private boolean remove(BinaryPassword password) {
		long websiteId = password.websiteId();
		String username = password.username();
		PasswordKey passwordKey = new PasswordKey(websiteId, username);
		BinaryPassword old = passwordMap.remove(passwordKey);
		if(old == null) { return false; }
		List<BinaryPassword> websiteIdPasswords = websiteIdPwdMap.get(websiteId);
		if(websiteIdPasswords == null) {
			passwordMap.put(passwordKey, old);
			return false;
		}
		int i = 0;
		BinaryPassword one = null;
		for(BinaryPassword pwd : websiteIdPasswords) {
			if(BinaryPassword.hasEqualUsername(password, pwd)) {
				one = websiteIdPasswords.remove(i);
				break;
			}
			i++;
		}
		if(one == null) {
			passwordMap.put(passwordKey, old);
			return false;
		}
		if(one != old) {
			passwordMap.put(passwordKey, old);
			websiteIdPasswords.add(one);
			return false;
		}
		
		List<BinaryPassword> usernamPasswords = usernamePwdMap.get(username);
		if(usernamPasswords == null) {
			passwordMap.put(passwordKey, old);
			websiteIdPasswords.add(one);
			return false;
		}
		i = 0;
		BinaryPassword another = null;
		for(BinaryPassword pwd : usernamPasswords) {
			if(BinaryPassword.hasEqualWebsiteId(password, pwd)) {
				another = usernamPasswords.remove(i);
				break;
			}
			i++;
		}
		if(another == null) {
			passwordMap.put(passwordKey, old);
			websiteIdPasswords.add(one);
			return false;
		}
		if(another != old) {
			passwordMap.put(passwordKey, old);
			websiteIdPasswords.add(one);
			usernamPasswords.add(another);
			return false;
		}
		return true;
	}

	@Override
	public Result<Password.Header> updatePassword(Password entry, CryptoKey encryptKey) {
		BinaryPassword newPassword = null;
		BinaryPassword oldPassword = null;
		BinaryPassword existedPassword = null;
		try {
			Result<BinaryPassword> biPasswordResult = selectBy(entry.header());
			if(!biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(Code.FAIL, biPasswordResult.msg);
			}
			existedPassword = biPasswordResult.result;
			biPasswordResult = encrypt(entry, encryptKey);
			if(!biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(biPasswordResult.code, biPasswordResult.msg);
			}
			newPassword = biPasswordResult.result;
		}catch(Exception e) {
			return new Result<Password.Header>(Code.FAIL, e.getMessage());
		}
		
		if(BinaryPassword.hasEqualPassword(newPassword, existedPassword)) {
			if(BinaryPassword.hasEqualKeyValuePair(newPassword, existedPassword)) {
				return new Result<Password.Header>(Code.FAIL, "nothing changed for password, don't need update");
			}else {
				oldPassword = existedPassword.copy();
				oldPassword.markEncryptedKeyValuePairsChanged();
				newPassword.markEncryptedKeyValuePairsChanged();
			}
		}else {
			oldPassword = existedPassword.copy();
			oldPassword.markEncryptedPasswordChanged();
			newPassword.markEncryptedPasswordChanged();
			if(!BinaryPassword.hasEqualKeyValuePair(newPassword, existedPassword)) {
				oldPassword.markEncryptedKeyValuePairsChanged();
				newPassword.markEncryptedKeyValuePairsChanged();
			}
		}
		if(appendUpdateOperation(existedPassword, oldPassword, newPassword)) {
			return new Result<Password.Header>(Code.SUCCESS, "success", entry.header());
		}else {
			return new Result<Password.Header>(Code.FAIL, "update binary password internal error");
		}
	}
	
	private boolean appendUpdateOperation(BinaryPassword existedPassword, BinaryPassword oldPassword, BinaryPassword newPassword) {
		replace(existedPassword, newPassword);
		undoQueue.offer(new ChangedOperation<BinaryPassword>(oldPassword, OP.UPDATE, newPassword));
		return true;
	}
	
	private void replace(BinaryPassword existedPassword, BinaryPassword newPassword) {
		if(newPassword.isEncryptedPasswordChanged()) {
			existedPassword.encryptedKeyValuePairs(newPassword.encryptedPassword());
		}
		if(newPassword.isEncryptedKeyValuePairsChanged()) {
			existedPassword.encryptedKeyValuePairs(newPassword.encryptedKeyValuePairs());
		}
	}

	@Override
	public Result<Password> selectPassword(Password.Header header, CryptoKey decryptKey) {
		Result<BinaryPassword> result = selectBy(header);
		if(!result.isSuccess()) {
			return new Result<Password>(Code.FAIL, result.msg);
		}
		return decrypt(result.result, decryptKey);
	}
	
	private Result<Password> decrypt(BinaryPassword biPassword, CryptoKey decryptKey) {
		Password password = new Password(biPassword.websiteId(), biPassword.username());
		
		Result<byte[]> decryptedResult = cryptoDriver.decrypt(biPassword.encryptedPassword(), decryptKey);
		if(!decryptedResult.isSuccess()) {
			return new Result<Password>(Code.FAIL, "password decrypt failed: "+decryptedResult.msg);
		}
		password.password(new String(decryptedResult.result, CHARSET));
		decryptedResult = cryptoDriver.decrypt(biPassword.encryptedKeyValuePairs(), decryptKey);
		if(!decryptedResult.isSuccess()) {
			return new Result<Password>(Code.FAIL, "key value pair decrypt failed: "+decryptedResult.msg);
		}
		password.keyValuePairs(new String(decryptedResult.result, CHARSET));
		return new Result<Password>(Code.SUCCESS, "success", password);
	}
	
	private Result<BinaryPassword> selectBy(Password.Header header) {
		long websiteId = header.websiteId();
		String username = header.username();
		if(!header.hasId() || !header.hasUsername()) {
			return new Result<BinaryPassword>(Code.FAIL, "password header without username and id");
		}
		
		BinaryPassword biPassword = passwordMap.get(new PasswordKey(websiteId, username));
		return biPassword == null ?
				new Result<BinaryPassword>(Code.FAIL, "password mapping with username and id not found") :
				new Result<BinaryPassword>(Code.SUCCESS, "success", biPassword);
	}
	
	@Override
	public Result<Integer> passwordCount() {
		return new Result<Integer>(Code.SUCCESS, "success", passwordCount);
	}
	
	@Override
	public Result<Integer> passwordCount(long websiteId) {
		List<BinaryPassword> biPasswordList = websiteIdPwdMap.get(websiteId);
		int count = biPasswordList == null ? 0 : biPasswordList.size();
		return new Result<Integer>(Code.SUCCESS, "success", count);
	}
	
	@Override
	public Result<Integer> passwordCount(String username) {
		List<BinaryPassword> biPasswordList = usernamePwdMap.get(username);
		int count = biPasswordList == null ? 0 : biPasswordList.size();
		return new Result<Integer>(Code.SUCCESS, "success", count);
	}

	@Override
	public Result<List<Password.Header>> listPassword(long websiteId) {
		List<BinaryPassword> biPasswordList = websiteIdPwdMap.get(websiteId);
		if(biPasswordList == null || biPasswordList.isEmpty()) {
			return new Result<List<Password.Header>>(Code.SUCCESS, "no password list mapping with webiste id");
		}
		List<Password.Header> list = new ArrayList<Password.Header>(biPasswordList.size());
		for(BinaryPassword biPassword : biPasswordList) {
			Password.Header header = new Password.Header(biPassword.websiteId(), biPassword.username());
			list.add(header);
		}
		return new Result<List<Password.Header>>(Code.SUCCESS, "success", list);
	}

	@Override
	public Result<List<Password.Header>> listPassword(String username) {
		List<BinaryPassword> biPasswordList = usernamePwdMap.get(username);
		if(biPasswordList == null || biPasswordList.isEmpty()) {
			return new Result<List<Password.Header>>(Code.SUCCESS, "no password list mapping with username");
		}
		List<Password.Header> list = new ArrayList<Password.Header>(biPasswordList.size());
		for(BinaryPassword biPassword : biPasswordList) {
			Password.Header header = new Password.Header(biPassword.websiteId(), biPassword.username());
			list.add(header);
		}
		return new Result<List<Password.Header>>(Code.SUCCESS, "success", list);
	}
	
	@Override
	public Result<Integer> canUndoTimes() {
		return new Result<Integer>(Code.SUCCESS, "success", undoQueue.size());
	}
	
	@Override
	public Result<Integer> canRedoTimes() {
		return new Result<Integer>(Code.SUCCESS, "success", redoQueue.size());
	}

	@Override
	public Result<Entity> undo() {
		ChangedOperation<? extends InternalEntity> changeOperation = undoQueue.pollLast();
		if(changeOperation == null) {
			return new Result<Entity>(Code.FAIL, "no history change operation afater last flush");
		}
		
		OP op = changeOperation.op();
		switch(op) {
		case INSERT: return undoInsert(changeOperation);
		case DELETE: return undoDelete(changeOperation);
		case UPDATE: return undoUpdate(changeOperation);
		default:
			undoQueue.offer(changeOperation);
			return new Result<Entity>(Code.FAIL, "unsupported op type of last changed operation: "+op);
		}
	}
	
	private Result<Entity> undoInsert(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity internalEntity = last.after();
		Entity entity = null;
		if(internalEntity.type() == Type.WEBSITE) {
			BinaryWebsite biWebsite = (BinaryWebsite) internalEntity;
			entity = biWebsite.transform();
			if(!undoInsertWebsite(biWebsite)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last insert website operation failed", entity);
			}
		}else {
			BinaryPassword biPassword = (BinaryPassword) internalEntity;
			entity = biPassword.transformWithSecret();
			if(!undoInsertPassword(biPassword)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last insert password operation failed", entity);
			}
		}
		redoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean undoInsertWebsite(BinaryWebsite biWebsite) { return remove(biWebsite); }
	
	private boolean undoInsertPassword(BinaryPassword biPassword) { return remove(biPassword); }

	private Result<Entity> undoDelete(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity internalEntity = last.before();
		Entity entity = null;
		if(internalEntity.type() == Type.WEBSITE) {
			BinaryWebsite biWebsite = (BinaryWebsite) internalEntity;
			entity = biWebsite.transform();
			if(!undoDeleteWebsite(biWebsite)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last delete website operation failed", entity);
			}
		}else {
			BinaryPassword biPassword = (BinaryPassword) internalEntity;
			entity = biPassword.transformWithSecret();
			if(!undoDeletePassword(biPassword)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last delete password operation failed", entity);
			}
		}
		redoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean undoDeleteWebsite(BinaryWebsite biWebsite) { return put(biWebsite); }
	
	private boolean undoDeletePassword(BinaryPassword biPassword) { return put(biPassword); }
	
	private Result<Entity> undoUpdate(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity before = last.before();
		InternalEntity after = last.before();
		Entity entity = null;
		if(after.type() == Type.WEBSITE) {
			BinaryWebsite oldWebsite = (BinaryWebsite) before;
			BinaryWebsite newWebsite = (BinaryWebsite) after;
			entity = oldWebsite.transform();
			if(!undoUpdateWebsite(oldWebsite, newWebsite)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last update website operation failed", entity);
			}
		}else {
			BinaryPassword oldPassword = (BinaryPassword) before;
			BinaryPassword newPassword = (BinaryPassword) after;
			entity = oldPassword.transformWithSecret();
			if(!undoUpdatePassword(oldPassword, newPassword)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last update password operation failed", entity);
			}
		}
		redoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean undoUpdateWebsite(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) { return replace(newWebsite, oldWebsite); }
	
	private boolean undoUpdatePassword(BinaryPassword oldPassword, BinaryPassword newPassword) {
		PasswordKey passwordKey = new PasswordKey(oldPassword.websiteId(), oldPassword.username());
		BinaryPassword existedPassword = passwordMap.get(passwordKey);
		if(existedPassword == null) { return false; }
		replace(existedPassword, oldPassword);
		return true;
	}

	@Override
	public Result<Entity> redo() {
		ChangedOperation<? extends InternalEntity> changeOperation = redoQueue.pollLast();
		if(changeOperation == null) {
			return new Result<Entity>(Code.FAIL, "no history undo operation");
		}
		
		OP op = changeOperation.op();
		switch(op) {
		case INSERT: return redoInsert(changeOperation);
		case DELETE: return redoDelete(changeOperation);
		case UPDATE: return redoUpdate(changeOperation);
		default:
			redoQueue.offer(changeOperation);
			return new Result<Entity>(Code.FAIL, "unsupported op type of last undo operation: "+op);
		}
	}
	
	private Result<Entity> redoInsert(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity internalEntity = last.after();
		Entity entity = null;
		if(internalEntity.type() == Type.WEBSITE) {
			BinaryWebsite biWebsite = (BinaryWebsite) internalEntity;
			entity = biWebsite.transform();
			if(!redoInsertWebsite(biWebsite)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last insert website operation failed", entity);
			}
		}else {
			BinaryPassword biPassword = (BinaryPassword) internalEntity;
			entity = biPassword.transformWithSecret();
			if(!redoInsertPassword(biPassword)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last insert password operation failed", entity);
			}
		}
		undoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean redoInsertWebsite(BinaryWebsite biWebsite) { return put(biWebsite); }
	
	private boolean redoInsertPassword(BinaryPassword biPassword) { return put(biPassword); }
	
	private Result<Entity> redoDelete(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity internalEntity = last.before();
		Entity entity = null;
		if(internalEntity.type() == Type.WEBSITE) {
			BinaryWebsite biWebsite = (BinaryWebsite) internalEntity;
			entity = biWebsite.transform();
			if(!redoDeleteWebsite(biWebsite)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last delete website operation failed", entity);
			}
		}else {
			BinaryPassword biPassword = (BinaryPassword) internalEntity;
			entity = biPassword.transformWithSecret();
			if(!redoDeletePassword(biPassword)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last delete password operation failed", entity);
			}
		}
		undoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean redoDeleteWebsite(BinaryWebsite biWebsite) { return remove(biWebsite); }
	
	private boolean redoDeletePassword(BinaryPassword biPassword) { return remove(biPassword); }

	private Result<Entity> redoUpdate(ChangedOperation<? extends InternalEntity> last) {
		InternalEntity before = last.before();
		InternalEntity after = last.before();
		Entity entity = null;
		if(after.type() == Type.WEBSITE) {
			BinaryWebsite oldWebsite = (BinaryWebsite) before;
			BinaryWebsite newWebsite = (BinaryWebsite) after;
			entity = oldWebsite.transform();
			if(!redoUpdateWebsite(oldWebsite, newWebsite)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last update website operation failed", entity);
			}
		}else {
			BinaryPassword oldPassword = (BinaryPassword) before;
			BinaryPassword newPassword = (BinaryPassword) after;
			entity = oldPassword.transformWithSecret();
			if(!redoUpdatePassword(oldPassword, newPassword)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last update password operation failed", entity);
			}
		}
		undoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean redoUpdateWebsite(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) { return replace(oldWebsite, newWebsite); }
	
	private boolean redoUpdatePassword(BinaryPassword oldPassword, BinaryPassword newPassword) {
		PasswordKey passwordKey = new PasswordKey(oldPassword.websiteId(), oldPassword.username());
		BinaryPassword existedPassword = passwordMap.get(passwordKey);
		if(existedPassword == null) { return false; }
		replace(existedPassword, newPassword);
		return true;
	}
	
	@Override
	public Result<Integer> needFlushCount() {
		return new Result<Integer>(Code.SUCCESS, "success", undoQueue.size());
	}

	@Override
	public Result<Throwable> flush() {
		ChangedOperation<? extends InternalEntity> first = null;
		while((first = undoQueue.poll()) != null) {
			OP op = first.op();
			/*
			 * for simplicity, flush changed operation sequentially.
			 * to reduce the I/O operation, you can merge all changed operations
			 * of the same entry first, and then flush to underlying storage. 
			 */
			switch(op) {
			case INSERT:
				InternalEntity inserted = first.after();
				try {
					if(inserted.type() == Type.WEBSITE) {
						flushInsertWebsite((BinaryWebsite) inserted);
					}else {
						flushInsertPassword((BinaryPassword) inserted);
					}
				}catch(Exception e) {
					return new Result<Throwable>(Code.FAIL, "flush insert internal error", e);
				}
				break;
			case DELETE:
				InternalEntity deleted = first.before();
				try {
					if(deleted.type() == Type.WEBSITE) {
						flushDeleteWebsite((BinaryWebsite) deleted);
					}else {
						flushDeletePassword((BinaryPassword) deleted);
					}
				}catch(Exception e) {
					return new Result<Throwable>(Code.FAIL, "flush delete internal error", e);
				}
				break;
			case UPDATE:
				InternalEntity before = first.before();
				InternalEntity after = first.after();
				try {
					if(before.type() == Type.WEBSITE) {
						flushUpdateWebsite((BinaryWebsite) before, (BinaryWebsite) after);
					}else {
						flushUpdatePassword((BinaryPassword) before, (BinaryPassword) after);
					}
				}catch(Exception e) {
					return new Result<Throwable>(Code.FAIL, "flush update internal error", e);
				}
				break;
			default:
				undoQueue.offerFirst(first);
				return new Result<Throwable>(Code.FAIL, "unsupported op type of flush operation: "+op);
			}
		}
		return new Result<Throwable>(Code.SUCCESS, "success");
	}
	
	private void flushInsertWebsite(BinaryWebsite target) {
		
	}
	
	private void flushInsertPassword(BinaryPassword target) {
		
	}
	
	private void flushDeleteWebsite(BinaryWebsite target) {
		
	}
	
	private void flushDeletePassword(BinaryPassword target) {
		
	}
	
	private void flushUpdateWebsite(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) {
		
	}
	
	private void flushUpdatePassword(BinaryPassword oldPassword, BinaryPassword newPassword) {
		
	}

	@Override
	public Result<Throwable> close() {
		Result<Throwable> result = flush();
		if(!result.isSuccess()) { return result; }
		if(!isClosed) {
			try {
				release();
			}catch(Exception e) {
				return new Result<Throwable>(Code.FAIL, "release resources failed", e);
			}
			isClosed = true;
		}
		return new Result<Throwable>(Code.SUCCESS, "success");
	}
	
	private static final class PasswordKey {
		final long websiteId;
		final String username;
		PasswordKey(long websiteId, String username) {
			this.websiteId = websiteId;
			this.username = username;
		}
		
		@Override public int hashCode() {
			long h = websiteId + 31 * username.hashCode();
			return (int) ((h >>> Integer.SIZE) & h);
		}
		@Override public boolean equals(Object obj) {
			if(obj == this) { return true; }
			if(!(obj instanceof PasswordKey)) { return false; }
			PasswordKey another = (PasswordKey) obj;
			return this.websiteId == another.websiteId
				 && this.username.equals(another.username);
		}
	}
}
