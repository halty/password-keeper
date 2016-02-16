package com.lee.password.keeper.impl.store;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	
	private static final int META_DATA_LEN = 4 + 8 + 4 + 8;	
	
	/** secret block size defined by private crypto key **/
	private final int secretBlockSize;
	
	// encryption/decryption driver
	private CryptoDriver cryptoDriver;
	
	// storage resources
	private File storePath;
	private RandomAccessFile storeMappedFile;
	private FileChannel storeChannel;
	private FileLock storeLock;
	
	// metadata
	/** the number of password **/
	private int passwordCount;
	/** the offset of password in channel **/
	private long passwordOffset;
	/** the number of website **/
	private int websiteCount;
	/** the offset of website in channel **/
	private long websiteOffset;
	
	// index password and website
	private Map<PasswordKey, BinaryPassword> passwordMap;
	private Map<Long, List<BinaryPassword>> websiteIdPwdMap;
	private Map<String, List<BinaryPassword>> usernamePwdMap;
	private Map<Long, BinaryWebsite> websiteIdMap;
	private Map<String, BinaryWebsite> websiteKeywordMap;
	
	// change operation queue
	private Deque<ChangedOperation<? extends InternalEntity>> undoQueue;
	private Deque<ChangedOperation<? extends InternalEntity>> redoQueue;
	
	// sync buffer
	/** metadata buffer layout: passwordCount(4) + passwordOffset(8) + websiteCount(4) + websiteOffset(8) **/
	private ByteBuffer metadataBuffer;
	private BinaryWebsite[] sortedWebsitesBuffer;
	
	// flush I/O buffer
	private ByteBuffer intBuffer;
	private ByteBuffer longBuffer;
	private ByteBuffer keywordBuffer;
	private ByteBuffer urlPortionWebsiteBuffer;
	private ByteBuffer webPortionWebsiteBuffer;
	private ByteBuffer pwdPortionWebsiteBuffer;
	private ByteBuffer websiteBuffer;
	private ByteBuffer usernameBuffer;
	private ByteBuffer keyValuePairBuffer;
	private ByteBuffer pwdPortionPasswordBuffer;
	private ByteBuffer pwdAndKvpPortionPasswordBuffer;
	private ByteBuffer passwordBuffer;
	
	/** closed flag **/
	private boolean isClosed;
	
	public BinaryStoreDriver(String dataDir, CryptoDriver cryptoDriver, int secretBlockSize, boolean lockStore) {
		try {
			this.secretBlockSize = secretBlockSize;
			this.cryptoDriver = cryptoDriver;
			this.storePath = createIfNotExisted(dataDir);
			this.storeMappedFile = new RandomAccessFile(storePath, "rw");
			this.storeChannel = storeMappedFile.getChannel();
			if(lockStore) { this.storeLock = storeChannel.lock(); }
			
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
		initUndoAndRedoDeque();
		initFlushIOBuffer();
		if(storePath.length() == 0) { // created new file
			initStore();
		}else {
			loadStore();
		}
		isClosed = false;
	}
	
	private void initUndoAndRedoDeque() {
		this.undoQueue = new LinkedList<ChangedOperation<? extends InternalEntity>>();
		this.redoQueue = new LinkedList<ChangedOperation<? extends InternalEntity>>();
	}
	
	private void initFlushIOBuffer() {
		intBuffer = byteOrder(ByteBuffer.allocate(4));
		longBuffer = byteOrder(ByteBuffer.allocate(8));
		keywordBuffer = byteOrder(ByteBuffer.allocate(BinaryWebsite.keywordSize()));
		urlPortionWebsiteBuffer = byteOrder(ByteBuffer.allocate(BinaryWebsite.urlPortionSize()));
		webPortionWebsiteBuffer = byteOrder(ByteBuffer.allocate(BinaryWebsite.webPortionSize()));
		pwdPortionWebsiteBuffer = byteOrder(ByteBuffer.allocate(BinaryWebsite.pwdPortionSize()));
		websiteBuffer = byteOrder(ByteBuffer.allocate(BinaryWebsite.occupiedSize()));
		usernameBuffer = byteOrder(ByteBuffer.allocate(BinaryPassword.maxUsernameSize()));
		keyValuePairBuffer = byteOrder(ByteBuffer.allocate(BinaryPassword.keyValuePairSize(secretBlockSize)));
		pwdPortionPasswordBuffer = byteOrder(ByteBuffer.allocate(BinaryPassword.pwdPortionSize(secretBlockSize)));
		pwdAndKvpPortionPasswordBuffer = byteOrder(ByteBuffer.allocate(BinaryPassword.pwdAndKvpPortionSize(secretBlockSize)));
		passwordBuffer = byteOrder(ByteBuffer.allocate(BinaryPassword.occupiedSize(secretBlockSize)));
	}
	
	/** denote the byte order **/
	private ByteBuffer byteOrder(ByteBuffer buf) { return buf.order(ByteOrder.BIG_ENDIAN); }
	
	/** init an data file with metadata **/
	private void initStore() throws Exception {
		long offset = MAGIC.length + META_DATA_LEN;
		this.passwordCount = 0;
		this.passwordOffset = offset;
		this.websiteCount = 0;
		this.websiteOffset = offset;
		this.metadataBuffer = initMetaBuffer(passwordCount, passwordOffset, websiteCount, websiteOffset);
		metadataBuffer.clear();
		
		storeChannel.position(0);
		storeChannel.write(byteOrder(ByteBuffer.wrap(MAGIC)));
		storeChannel.write(metadataBuffer);
		storeChannel.force(true);
		
		this.passwordMap = new HashMap<PasswordKey, BinaryPassword>();
		this.websiteIdPwdMap = new HashMap<Long, List<BinaryPassword>>();
		this.usernamePwdMap = new HashMap<String, List<BinaryPassword>>();
		this.websiteIdMap = new HashMap<Long, BinaryWebsite>();
		this.websiteKeywordMap = new HashMap<String, BinaryWebsite>();
		this.sortedWebsitesBuffer = new BinaryWebsite[10];
	}
	
	private ByteBuffer initMetaBuffer(int passwordCount, long passwordOffset, int websiteCount, long websiteOffset) {
		ByteBuffer buf = byteOrder(ByteBuffer.allocate(META_DATA_LEN))
				.putInt(passwordCount)
				.putLong(passwordOffset)
				.putInt(websiteCount)
				.putLong(websiteOffset);
		return buf;
	}
	
	/** load all the data from file to memory **/
	private void loadStore() throws Exception {
		matchMagic();
		loadMetadata();
		loadWebsites();
		loadPasswords();
	}
	
	private void matchMagic() throws IOException {
		int expectedLen = MAGIC.length;
		byte[] magic = new byte[expectedLen];
		int len = storeChannel.read(byteOrder(ByteBuffer.wrap(magic)), 0);
		if(len != expectedLen) {
			throw new StoreException("incorrect magic number size from store path: "+storePath);
		}
		if(!Arrays.equals(magic, MAGIC)) {
			throw new StoreException("wrong magic number from store path: "+storePath);
		}
	}
	
	private void loadMetadata() {
		try {
			this.metadataBuffer = byteOrder(ByteBuffer.allocate(META_DATA_LEN));
			metadataBuffer.clear();
			int len = storeChannel.read(metadataBuffer, MAGIC.length);
			if(len != META_DATA_LEN) {
				throw new StoreException("incorrect metadata size from store path: "+storePath);
			}
			metadataBuffer.flip();
			this.passwordCount = metadataBuffer.getInt();
			this.passwordOffset = metadataBuffer.getLong();
			this.websiteCount = metadataBuffer.getInt();
			this.websiteOffset = metadataBuffer.getLong();
		}catch(IOException e) {
			throw new StoreException("failed to load meta data from store path: "+storePath);
		}
	}
	
	private void loadWebsites() throws IOException {
		long position = this.websiteOffset;
		int count = this.websiteCount;
		this.websiteIdMap = new HashMap<Long, BinaryWebsite>(count);
		this.websiteKeywordMap = new HashMap<String, BinaryWebsite>(count);
		this.sortedWebsitesBuffer = new BinaryWebsite[10];
		if(count == 0) { return; }
		
		/**
		 * mapping is more expensive than reading or writing, it is only worth
		 * mapping relatively large files.
		 * generally, pasword store is a small file, so we use read or write methods here.
		 */
		ByteBuffer websiteBuffer = this.websiteBuffer;
		int size = BinaryWebsite.occupiedSize();
		long lastWebsiteId = Long.MIN_VALUE;
		int totalPasswordCount = 0;
		for(int i=0; i<count; i++) {
			websiteBuffer.clear();
			int readBytes = storeChannel.read(websiteBuffer, position);
			if(readBytes != size) {
				throw new StoreException(String.format("%d bytes starting at %d is not enough for website data from store path: %s",
						readBytes, position, storePath));
			}
			websiteBuffer.flip();
			position += size;
			
			BinaryWebsite website = BinaryWebsite.read(websiteBuffer);
			long websiteId = website.websiteId();
			if(lastWebsiteId > websiteId) {
				// website data placed order by website id asc in store file
				throw new StoreException(String.format("incorrect website data order begining with %d from store path: %s",
						websiteId, storePath));
			}
			if(!put(website, false)) {
				throw new StoreException(String.format("conflict website with website id=%d from store path: %s",
						websiteId, storePath));
			}
			addToSortedWebsitesBuffer(website.copy(), i);
			lastWebsiteId = websiteId;
			totalPasswordCount += website.count();
		}
		
		if(totalPasswordCount != passwordCount) {
			throw new StoreException("inconsistent password count from store path: "+storePath);
		}
	}
	
	private void addToSortedWebsitesBuffer(BinaryWebsite newWebsite, int existedCount) {
		long targetWebsiteId = newWebsite.websiteId();
		BinaryWebsite[] array = sortedWebsitesBuffer;
		int insertingIndex = findInsertIndex(targetWebsiteId, array, 0, existedCount-1);
		addToSortedWebsitesBuffer(newWebsite, insertingIndex, existedCount);
	}
	
	private int findInsertIndex(long targetWebsiteId, BinaryWebsite[] array, int begin, int end) {
		while(begin <= end) {
			int mid = (begin + end) / 2;
			long websiteId = array[mid].websiteId();
			if(targetWebsiteId == websiteId) {
				begin = mid + 1;
				break;
			}else if(targetWebsiteId < websiteId) {
				end = mid -1;
			}else {
				begin = mid + 1;
			}
		}
		return begin;
	}
	
	private void addToSortedWebsitesBuffer(BinaryWebsite newWebsite, int index, int limit) {
		if(index < 0 || index > limit) {
			throw new IndexOutOfBoundsException(String.format("inserting index %d is out of bounds %d for sorted website buffer",
					index, limit));
		}
		if(limit >= sortedWebsitesBuffer.length) {
			int newCount = limit * 3 / 2 + 1;
			BinaryWebsite[] array = new BinaryWebsite[newCount];
			System.arraycopy(sortedWebsitesBuffer, 0, array, 0, limit);
			sortedWebsitesBuffer = array;
		}
		if(index == limit) {
			sortedWebsitesBuffer[limit] = newWebsite;
		}else {
			BinaryWebsite[] array = sortedWebsitesBuffer;
			System.arraycopy(array, index, array, index+1, limit-index);
			array[index] = newWebsite;
		}
	}
	
	private void loadPasswords() throws IOException {
		int websiteCount = this.websiteCount;
		long position = this.passwordOffset;
		int count = this.passwordCount;
		this.passwordMap = new HashMap<PasswordKey, BinaryPassword>(count);
		this.websiteIdPwdMap = new HashMap<Long, List<BinaryPassword>>();
		this.usernamePwdMap = new HashMap<String, List<BinaryPassword>>();
		if(count == 0) { return; }
		
		BinaryWebsite[] sortedView = this.sortedWebsitesBuffer;
		int size = BinaryPassword.occupiedSize(this.secretBlockSize);
		int offset = 0;
		for(int i=0; i<websiteCount; i++) {
			BinaryWebsite website = sortedView[i];
			if(website.offset() != offset) {
				// password data placed order by website id asc in store file
				throw new StoreException(String.format("incorrect password data order for website id=%d from store path: %s",
						website.websiteId(), storePath));
			}
			loadPasswordsBy(size, website, position+offset);
			offset += website.count() * size;
		}
	}
	
	private void loadPasswordsBy(int passwordSize, BinaryWebsite website, long position) throws IOException {
		long websiteId = website.websiteId();
		int count = website.count();
		ByteBuffer passwordBuffer = this.passwordBuffer;
		for(int i=0; i<count; i++) {
			passwordBuffer.clear();
			int readBytes = storeChannel.read(passwordBuffer, position);
			if(readBytes != passwordSize) {
				throw new StoreException(String.format("%d bytes starting at %d is not enough for password data from store path: %s",
						readBytes, position, storePath));
			}
			passwordBuffer.flip();
			position += passwordSize;
			
			BinaryPassword password = BinaryPassword.read(passwordBuffer, secretBlockSize);
			/* generally, one person who may not register multiple account on the same website,
			 * so all the password data belong to the same website don't placed in order.
			 */
			if(password.websiteId() != websiteId) {
				throw new StoreException(String.format("inconsistent website id=%d from store path: %s", websiteId, storePath));
			}
			if(!put(password, false)) {
				throw new StoreException(String.format("conflict passwords with website id=%d from store path: %s",
						websiteId, storePath));
			}
		}
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
	public Result<String> storePath() { return new Result<String>(Code.SUCCESS, "success", storePath.getAbsolutePath()); }
	
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
		if(!put(newWebsite, true)) { return false; }
		undoQueue.offer(new ChangedOperation<BinaryWebsite>(null, OP.INSERT, newWebsite.copy()));
		return true;
	}
	
	private boolean put(BinaryWebsite newWebsite, boolean increaseCountIfSuccess) {
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
		if(increaseCountIfSuccess) { websiteCount++; }
		return true;
	}

	@Override
	public Result<Website> deleteWebsite(Website website) {
		Result<BinaryWebsite> result = selectBy(website);
		if(!result.isSuccess()) {
			return new Result<Website>(Code.FAIL, result.msg);
		}
		BinaryWebsite biWebsite = result.result;
		List<BinaryPassword> biPasswordList = websiteIdPwdMap.get(biWebsite.websiteId());
		if(biPasswordList != null && !biPasswordList.isEmpty()) {
			return new Result<Website>(Code.FAIL, "remained password list mapping with webiste id");
		}
		if(appendDeleteOperation(biWebsite)) {
			return new Result<Website>(Code.SUCCESS, "success", biWebsite.transform());
		}else {
			return new Result<Website>(Code.FAIL, "delete binary website internal error");
		}
	}
	
	private boolean appendDeleteOperation(BinaryWebsite oldWebsite) {
		if(!remove(oldWebsite)) { return false; }
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
		websiteCount--;
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
				website.keyword(biWebsite2.keyword());
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
			website.id(biWebsite.websiteId());
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
	
	private Result<BinaryWebsite> selectBy(long websiteId) {
		BinaryWebsite biWebsite = websiteIdMap.get(websiteId);
		if(biWebsite == null) {
			return new Result<BinaryWebsite>(Code.FAIL, "website mapping with id not found");
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
			Result<BinaryWebsite> biWebsiteResult = selectBy(entry.header().websiteId());
			if(!biWebsiteResult.isSuccess()) {
				return new Result<Password.Header>(Code.FAIL, biWebsiteResult.msg);
			}
			Result<BinaryPassword> biPasswordResult = selectBy(entry.header());
			if(biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(Code.FAIL, "an existed entry mapping for this header");
			}
			biPasswordResult = encrypt(entry, encryptKey, true);
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
	
	private Result<BinaryPassword> encrypt(Password password, CryptoKey encryptKey, boolean forInsert) {
		Password.Header header = password.header();
		Password.Secret secret = password.secret();
		String pwd = secret.password();
		String kvp = secret.keyValuePairs();
		
		BinaryPassword biPassword = new BinaryPassword(header.websiteId(), header.username(), header.timestamp());
		if(pwd == null || pwd.isEmpty()) {
			if(forInsert) { return new Result<BinaryPassword>(Code.FAIL, "pasword is empty"); }
		}else {
			Result<byte[]> encryptedResult = cryptoDriver.encrypt(pwd.getBytes(CHARSET), encryptKey);
			if(!encryptedResult.isSuccess()) {
				return new Result<BinaryPassword>(Code.FAIL, "password encrypt failed: "+encryptedResult.msg);
			}
			biPassword.encryptedPassword(encryptedResult.result);
		}
		if(kvp == null) {
			if(!forInsert) { return new Result<BinaryPassword>(Code.SUCCESS, "success", biPassword); }
			kvp = "";
		}
		Result<byte[]> encryptedResult = cryptoDriver.encrypt(kvp.getBytes(CHARSET), encryptKey);
		if(!encryptedResult.isSuccess()) {
			return new Result<BinaryPassword>(Code.FAIL, "key value pair encrypt failed: "+encryptedResult.msg);
		}
		biPassword.encryptedKeyValuePairs(encryptedResult.result);
		return new Result<BinaryPassword>(Code.SUCCESS, "success", biPassword);
	}
	
	private boolean appendInsertOperation(BinaryPassword password) {
		if(!put(password, true)) { return false; }
		undoQueue.offer(new ChangedOperation<BinaryPassword>(null, OP.INSERT, password.copy()));
		return true;
	}
	
	private boolean put(BinaryPassword password, boolean increaseCountIfSuccess) {
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
		if(increaseCountIfSuccess) { passwordCount++; }
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
		passwordCount--;
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
			biPasswordResult = encrypt(entry, encryptKey, false);
			if(!biPasswordResult.isSuccess()) {
				return new Result<Password.Header>(biPasswordResult.code, biPasswordResult.msg);
			}
			newPassword = biPasswordResult.result;
		}catch(Exception e) {
			return new Result<Password.Header>(Code.FAIL, e.getMessage());
		}
		
		if(newPassword.encryptedPassword() == null || BinaryPassword.hasEqualPassword(newPassword, existedPassword)) {
			if(newPassword.encryptedKeyValuePairs() == null || BinaryPassword.hasEqualKeyValuePair(newPassword, existedPassword)) {
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
			if(newPassword.encryptedKeyValuePairs() != null && !BinaryPassword.hasEqualKeyValuePair(newPassword, existedPassword)) {
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
			existedPassword.encryptedPassword(newPassword.encryptedPassword());
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
			return new Result<List<Password.Header>>(Code.FAIL, "no password list mapping with webiste id");
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
			return new Result<List<Password.Header>>(Code.FAIL, "no password list mapping with username");
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
			entity = biPassword.transformWithoutSecret();
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
			entity = biPassword.transformWithoutSecret();
			if(!undoDeletePassword(biPassword)) {
				undoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "undo last delete password operation failed", entity);
			}
		}
		redoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean undoDeleteWebsite(BinaryWebsite biWebsite) { return put(biWebsite.copy(), true); }
	
	private boolean undoDeletePassword(BinaryPassword biPassword) { return put(biPassword.copy(), true); }
	
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
			entity = oldPassword.transformWithoutSecret();
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
	public Result<Integer> canRedoTimes() {
		return new Result<Integer>(Code.SUCCESS, "success", redoQueue.size());
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
			entity = biPassword.transformWithoutSecret();
			if(!redoInsertPassword(biPassword)) {
				redoQueue.offer(last);
				return new Result<Entity>(Code.FAIL, "redo last insert password operation failed", entity);
			}
		}
		undoQueue.offer(last);
		return new Result<Entity>(Code.SUCCESS, "success", entity);
	}
	
	private boolean redoInsertWebsite(BinaryWebsite biWebsite) { return put(biWebsite.copy(), true); }
	
	private boolean redoInsertPassword(BinaryPassword biPassword) { return put(biPassword.copy(), true); }
	
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
			entity = biPassword.transformWithoutSecret();
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
			entity = oldPassword.transformWithoutSecret();
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
		int toBeFlushedCount = undoQueue.size();
		try {
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
							writeInsertWebsite((BinaryWebsite) inserted);
						}else {
							writeInsertPassword((BinaryPassword) inserted);
						}
					}catch(Exception e) {
						return new Result<Throwable>(Code.FAIL, "flush insert internal error", e);
					}
					break;
				case DELETE:
					InternalEntity deleted = first.before();
					try {
						if(deleted.type() == Type.WEBSITE) {
							writeDeleteWebsite((BinaryWebsite) deleted);
						}else {
							writeDeletePassword((BinaryPassword) deleted);
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
							writeUpdateWebsite((BinaryWebsite) before, (BinaryWebsite) after);
						}else {
							writeUpdatePassword((BinaryPassword) before, (BinaryPassword) after);
						}
					}catch(Exception e) {
						return new Result<Throwable>(Code.FAIL, "flush update internal error", e);
					}
					break;
				default:
					undoQueue.offerFirst(first);
					return new Result<Throwable>(Code.FAIL, "unsupported op type of flush operation: "+op);
				}
				// just for test
				try { flushChanged(); }catch(IOException e) { throw new StoreException("flush changed failed", e); }
			}
			return new Result<Throwable>(Code.SUCCESS, "success");
		}finally {
			try {
				if(undoQueue.size() < toBeFlushedCount) { flushChanged(); }
			}catch(IOException e) {
				return new Result<Throwable>(Code.FAIL, "failed to force flush changed to store path: "+storePath, e);
			}
		}
	}
	
	private void writeInsertWebsite(BinaryWebsite target) {
		BinaryWebsite[] array = sortedWebsitesBuffer;
		long targetWebsiteId = target.websiteId();
		int actualCount = readWebsiteCount();
		int offsetCount = actualCount == 0 ? 0 : findInsertIndex(targetWebsiteId, array, 0, actualCount-1);

		long position = readWebsiteOffset();
		int size = BinaryWebsite.occupiedSize();
		try {
			// validateWebsiteInsertOffset(targetWebsiteId, position, size, actualCount, offsetCount);
			long insertingPosition = position + offsetCount * size;
			ByteBuffer buf = websiteBuffer;
			buf.clear();
			BinaryWebsite.write(buf, target);
			buf.flip();
			expandAndFill(insertingPosition, buf, size);
			addToSortedWebsitesBuffer(target, offsetCount, actualCount);
			writeWebsiteCount(actualCount + 1);
		}catch(IOException e) {
			throw new StoreException(String.format("failed to insert website with id=%d to store path: %s",
					targetWebsiteId, storePath), e);
		}
	}
	
	@SuppressWarnings("unused")
	private void validateWebsiteInsertOffset(long targetWebsiteId, long position,
			int size, int actualCount, int offsetCount) throws IOException {
		if(actualCount == 0) {
			if(offsetCount != 0) {
				throw new StoreException(String.format("incorrect insert offset=%d for website with id=%d from store path: %s",
						offsetCount, targetWebsiteId, storePath));
			}
			return;
		}
		ByteBuffer buf = longBuffer;
		int length = buf.capacity();
		if(offsetCount == 0) { // first
			buf.clear();
			int readBytes = storeChannel.read(buf, position);
			if(readBytes != length) {
				throw new StoreException("failed to read website id from store path: "+storePath);
			}
			buf.flip();
			if(targetWebsiteId > buf.getLong()) {
				throw new StoreException(String.format("incorrect insert offset=%d for website with id=%d from store path: %s",
						offsetCount, targetWebsiteId, storePath));
			}
		}else if(offsetCount == actualCount) { // end
			buf.clear();
			int readBytes = storeChannel.read(buf, position+(offsetCount-1)*size);
			if(readBytes != length) {
				throw new StoreException("failed to read website id from store path: "+storePath);
			}
			buf.flip();
			if(targetWebsiteId < buf.getLong()) {
				throw new StoreException(String.format("incorrect insert offset=%d for website with id=%d from store path: %s",
						offsetCount, targetWebsiteId, storePath));
			}
		}else {
			buf.clear();
			int readBytes = storeChannel.read(buf, position+offsetCount*size);
			if(readBytes != length) {
				throw new StoreException("failed to read website id from store path: "+storePath);
			}
			buf.flip();
			if(targetWebsiteId > buf.getLong()) {
				throw new StoreException(String.format("incorrect insert offset=%d for website with id=%d from store path: %s",
						offsetCount, targetWebsiteId, storePath));
			}
			buf.clear();
			readBytes = storeChannel.read(buf, position+(offsetCount-1)*size);
			if(readBytes != length) {
				throw new StoreException("failed to read website id from store path: "+storePath);
			}
			buf.flip();
			if(targetWebsiteId < buf.getLong()) {
				throw new StoreException(String.format("incorrect insert offset=%d for website with id=%d from store path: %s",
						offsetCount, targetWebsiteId, storePath));
			}
		}
	}
	
	private void expandAndFill(long position, ByteBuffer buf, int increamentSize) throws IOException {
		long fileSize = storeChannel.size();
		int movedBytes = (int) (fileSize - position);
		if(movedBytes > 0) {
			transfer(position, movedBytes, position + increamentSize);
		}
		int writeBytes = storeChannel.write(buf, position);
		if(writeBytes != increamentSize) {
			throw new StoreException(String.format("failed to write %d bytes starting at %d to store path: %s",
					increamentSize, position, storePath));
		}
	}
	
	private void transfer(long srcPosition, int transferBytes, long destPosition) throws IOException {
		/**
		 * when using transferFrom() or transferTo() on a FileChannel to transfer data from one region of
		 * the file to another, the call hangs on the OS X platform, whether the source region overlap with
		 * destination region or not, a similar bug report for openjdk see {@link https://bugs.openjdk.java.net/browse/JDK-8140241};
		 * while on Windows platform, the call doesn't hang, but causes data overwritten if two regions overlaps;
		 * other platforms not tested;
		 * 
		 * although transfer operation is potentially much more efficient, a simple loop that reads from
		 * this channel and writes to itself was also choosen for corectness guarantee.
		 */
		ByteBuffer movedBuf = ByteBuffer.allocate((int)transferBytes);
		int readBytes = storeChannel.read(movedBuf, srcPosition);
		if(readBytes != transferBytes) {
			throw new StoreException(String.format("for expand, failed to read %d bytes starting at %d from store path: %s",
					transferBytes, srcPosition, storePath));
		}
		movedBuf.flip();
		int writeBytes = storeChannel.write(movedBuf, destPosition);
		if(writeBytes != transferBytes) {
			throw new StoreException(String.format("for expand, failed to write %d bytes starting at %d to store path: %s",
					transferBytes, destPosition, storePath));
		}
	}

	private int readPasswordCount() { return readIntFromMetadataBuffer(0); }
	private long readPasswordOffset() { return readLongFromMetadataBuffer(4); }
	private int readWebsiteCount() { return readIntFromMetadataBuffer(12); }
	private long readWebsiteOffset() { return readLongFromMetadataBuffer(16); }
	
	private int readIntFromMetadataBuffer(int offset) {
		ByteBuffer buf = metadataBuffer;
		buf.clear().position(offset);
		return buf.getInt();
	}
	
	private long readLongFromMetadataBuffer(int offset) {
		ByteBuffer buf = metadataBuffer;
		buf.clear().position(offset);
		return buf.getLong();
	}
	
	private void writePasswordCount(int passwordCount) { writeIntToMetadataBuffer(0, passwordCount); }
	@SuppressWarnings("unused")
	private void writePasswordOffset(long passwordOffset) { writeLongToMetadataBuffer(4, passwordOffset); }
	private void writeWebsiteCount(int websiteCount) { writeIntToMetadataBuffer(12, websiteCount); }
	private void writeWebsiteOffset(long websiteOffset) { writeLongToMetadataBuffer(16, websiteOffset); }
	
	private void writeIntToMetadataBuffer(int offset, int value) {
		ByteBuffer buf = metadataBuffer;
		buf.clear().position(offset);
		buf.putInt(value);
		
		writeInt(MAGIC.length + offset, value);
	}
	
	private void writeInt(long position, int value) {
		ByteBuffer buf = intBuffer;
		buf.clear();
		buf.putInt(value);
		buf.flip();
		write(buf, position);
	}
	
	private void write(ByteBuffer buf, long position) {
		int length = buf.capacity();
		try {
			if(storeChannel.write(buf, position) != length) {
				throw new StoreException(String.format("failed to write %d bytes at position %d to store path: %s",
						length, position, storePath));
			}
		}catch(IOException e) {
			throw new StoreException(String.format("failed to write %d bytes at position %d to store path: %s",
					length, position, storePath), e);
		}
	}
	
	private void writeLongToMetadataBuffer(int offset, long value) {
		ByteBuffer buf = metadataBuffer;
		buf.clear().position(offset);
		buf.putLong(value);
		
		writeLong(MAGIC.length + offset, value);
	}
	
	private void writeLong(long position, long value) {
		ByteBuffer buf = longBuffer;
		buf.clear();
		buf.putLong(value);
		buf.flip();
		write(buf, position);
	}
	
	private void writeInsertPassword(BinaryPassword target) {
		BinaryWebsite[] array = sortedWebsitesBuffer;
		long targetWebsiteId = target.websiteId();
		int actualWebsiteCount = readWebsiteCount();
		long position = readPasswordOffset();
		int actualCount = readPasswordCount();
		int size = BinaryPassword.occupiedSize(secretBlockSize);
		ByteBuffer buf = passwordBuffer;
		buf.clear();
		BinaryPassword.write(buf, secretBlockSize, target);
		buf.flip();

		try {
			int curIndex = findExactIndex(targetWebsiteId, array, 0, actualWebsiteCount-1);
			if(curIndex == -1) {
				throw new StoreException(String.format("while insert password, failed to find website with id=%d from store path: %s",
						targetWebsiteId, storePath));
			}
			BinaryWebsite existedWebsite = array[curIndex];
			long insertingPosition = position;
			long pwdOffsetOfWebsite = 0;
			if(actualCount > 0) {
				if(existedWebsite.isValidOffset()) {
					pwdOffsetOfWebsite = existedWebsite.offset();
					insertingPosition += pwdOffsetOfWebsite;
					insertingPosition += existedWebsite.count()*size;
				}else {
					int prevIndex = -1;
					for(int i=curIndex-1; i>=0; i--) {
						if(array[i].isValidOffset()) {
							prevIndex = i;
							break;
						}
					}
					if(prevIndex != -1) {
						BinaryWebsite prevWebsite = array[prevIndex];
						insertingPosition += prevWebsite.offset();
						insertingPosition += prevWebsite.count()*size;
					}
					pwdOffsetOfWebsite = insertingPosition - position;
				}
			}
			expandAndFill(insertingPosition, buf, size);
			writePasswordCount(actualCount+1);
			writeWebsiteOffset(readWebsiteOffset()+size);
			writeCurrentPwdCountAndOffset(curIndex, array, pwdOffsetOfWebsite, 1);
			writeSucceedingOffsets(array, curIndex+1, actualWebsiteCount, size);
		}catch(IOException e) {
			throw new StoreException(String.format("failed to insert password with website id=%d and username=%s from store path: %s",
					targetWebsiteId, target.username(), storePath), e);
		}
	}
	
	/**
	 * return the exact index of {@code targetWebsiteId}, if it is contained in the {@code array}
	 * within the specified range {@code [begin, end]}; otherwise {@code -1} **/
	private int findExactIndex(long targetWebsiteId, BinaryWebsite[] array, int begin, int end) {
		int index = -1;
		while(begin <= end) {
			int mid = (begin + end) / 2;
			long websiteId = array[mid].websiteId();
			if(targetWebsiteId == websiteId) {
				index = mid;
				break;
			}else if(targetWebsiteId < websiteId) {
				end = mid -1;
			}else {
				begin = mid + 1;
			}
		}
		return index;
	}
	
	private void writeCurrentPwdCountAndOffset(int curIndex, BinaryWebsite[] array, long newOffset, int deltaCount) throws IOException {
		// update password count and password entries offset of current website
		long websiteOffset = readWebsiteOffset();
		int size = BinaryWebsite.occupiedSize();
		long position = websiteOffset + curIndex * size;
		BinaryWebsite website = array[curIndex];
		website.incrementCount(deltaCount);
		website.timestamp(System.currentTimeMillis());
		if(!website.isValidOffset()) { website.offset(newOffset); }
		writePwdPortionOfWebsite(position, website);
	}
	
	private void writePwdPortionOfWebsite(long websitePosition, BinaryWebsite website) {
		long position = BinaryWebsite.pwdPortionPosition(websitePosition);
		ByteBuffer buf = pwdPortionWebsiteBuffer;
		buf.clear();
		BinaryWebsite.writePwdPortion(buf, website);
		buf.flip();
		write(buf, position);
	}
	
	private void writeSucceedingOffsets(BinaryWebsite[] array, int firstIndex, int totalCount,
			int deltaOffset) throws IOException {
		// update password offset of succeeding websites
		long websiteOffset = readWebsiteOffset();
		int size = BinaryWebsite.occupiedSize();
		for(int i=firstIndex; i<totalCount; i++) {
			BinaryWebsite website = array[i];
			if(website.isValidOffset()) {
				website.incrementOffset(deltaOffset);
				long position = websiteOffset + i * size;
				writePasswordOffsetOfWebsite(position, website.offset());
			}
		}
	}
	
	private void writePasswordOffsetOfWebsite(long websitePosition, long newOffset) throws IOException {
		long position = BinaryWebsite.offsetPosition(websitePosition);
		writeLong(position, newOffset);
	}
	
	private void writeDeleteWebsite(BinaryWebsite target) {
		long targetWebsiteId = target.websiteId();
		BinaryWebsite[] array = sortedWebsitesBuffer;
		int actualCount = readWebsiteCount();
		int removingIndex = findExactIndex(targetWebsiteId, array, 0, actualCount-1);
		if(removingIndex == -1) {
			throw new StoreException(String.format("while delete website, website with id=%d not found in store path: %s",
					targetWebsiteId, storePath));
		}
		
		try {
			long position = readWebsiteOffset();
			int size = BinaryWebsite.occupiedSize();
			position += removingIndex * size;
			truncate(position, size);
			
			// check website count equal 0 or not
			System.arraycopy(array, removingIndex+1, array, removingIndex, actualCount - removingIndex - 1);
			array[actualCount-1] = null;
			
			writeWebsiteCount(actualCount-1);
		}catch(IOException e) {
			throw new StoreException(String.format("failed to delete website with id=%d from store path: %s",
					targetWebsiteId, storePath), e);
		}
	}
	
	private void truncate(long position, int decreamentSize) throws IOException {
		long fileSize = storeChannel.size();
		long nextPosition = position + decreamentSize;
		int movedBytes = (int) (fileSize - nextPosition);
		if(movedBytes > 0) {
			transfer(nextPosition, movedBytes, position);
		}
		storeChannel.truncate(fileSize - decreamentSize);
	}
	
	private void writeDeletePassword(BinaryPassword target) {
		BinaryWebsite[] array = sortedWebsitesBuffer;
		int actualWebsiteCount = readWebsiteCount();
		long position = readPasswordOffset();
		int actualCount = readPasswordCount();
		long targetWebsiteId = target.websiteId();
		int size = BinaryPassword.occupiedSize(secretBlockSize);
		
		try {
			int curIndex = findExactIndex(targetWebsiteId, array, 0, actualWebsiteCount-1);
			if(curIndex == -1) {
				throw new StoreException(String.format("while delete password, failed to find website with id=%d from store path: %s",
						targetWebsiteId, storePath));
			}
			BinaryWebsite existedWebsite = array[curIndex];
			
			long deletingPosition = position + existedWebsite.offset();
			int passwordCount = existedWebsite.count();
			deletingPosition = findPasswordPostion(deletingPosition, passwordCount, size, target);
			if(deletingPosition == -1) {
				throw new StoreException(String.format("while delete password, failed to find password with id=%d and username=%s from store path: %s",
						targetWebsiteId, target.username(), storePath));
			}
			truncate(deletingPosition, size);
			writePasswordCount(actualCount-1);
			writeWebsiteOffset(readWebsiteOffset()-size);
			writeCurrentPwdCountAndOffset(curIndex, array, existedWebsite.offset(), -1);
			writeSucceedingOffsets(array, curIndex+1, actualWebsiteCount, -size);
		}catch(IOException e) {
			throw new StoreException(String.format("failed to delete password with website id=%d and username=%s from store path: %s",
					targetWebsiteId, target.username(), storePath), e);
		}
	}
	
	/** return the position of target password if it exists, otherwise -1 **/
	private long findPasswordPostion(long startPosition, int passwordCount, int passwordSize,
			BinaryPassword targetPassword) throws IOException {
		ByteBuffer buf = usernameBuffer;
		long position = startPosition;
		for(int i=0; i<passwordCount; i++) {
			buf.clear();
			storeChannel.read(buf, BinaryPassword.usernamePosition(position));
			buf.flip();
			if(BinaryPassword.hasEqualUsername(targetPassword, buf)) {
				return position;
			}
			position += passwordSize;
		}
		return -1;
	}
	
	private void writeUpdateWebsite(BinaryWebsite oldWebsite, BinaryWebsite newWebsite) {
		long targetWebsiteId = newWebsite.websiteId();
		BinaryWebsite[] array = sortedWebsitesBuffer;
		int actualCount = readWebsiteCount();
		int index = findExactIndex(targetWebsiteId, array, 0, actualCount-1);
		if(index == -1) {
			throw new StoreException(String.format("while update website, website with id=%d not found in store path: %s",
					targetWebsiteId, storePath));
		}
		
		try {
			BinaryWebsite biWebsite = array[index];
			long position = readWebsiteOffset();
			int size = BinaryWebsite.occupiedSize();
			position += index * size;
			if(newWebsite.isKeywordChanged()) {
				if(newWebsite.isUrlChanged()) {
					writeKeywordAndUrl(position, newWebsite);
					biWebsite.pasteUrl(newWebsite);
				}else {
					writeKeyword(position, newWebsite);
				}
				biWebsite.pasteKeyword(newWebsite);
			}else {
				if(newWebsite.isUrlChanged()) {
					writeUrl(position, newWebsite);
					biWebsite.pasteUrl(newWebsite);
				}else {
					throw new StoreException(String.format("while update website, no changed found for website id=%d in store path: %s",
							targetWebsiteId, storePath));
				}
			}
		}catch(StoreException e) {
			throw e;
		}catch(Exception e) {
			throw new StoreException(String.format("failed to update website with id=%d to store path: %s",
					targetWebsiteId, storePath), e);
		}
	}
	
	private void writeKeywordAndUrl(long websitePosition, BinaryWebsite newWebsite) {
		ByteBuffer buf = webPortionWebsiteBuffer;
		buf.clear();
		BinaryWebsite.writeWebPortion(buf, newWebsite);
		buf.flip();
		write(buf, BinaryWebsite.webPortionPosition(websitePosition));
	}
	
	private void writeKeyword(long websitePosition, BinaryWebsite newWebsite) {
		ByteBuffer buf = keywordBuffer;
		buf.clear();
		BinaryWebsite.writeKeyword(buf, newWebsite);
		buf.flip();
		write(buf, BinaryWebsite.keywordPosition(websitePosition));
		writeLong(BinaryWebsite.timestampPosition(websitePosition), newWebsite.timestamp());
	}
	
	private void writeUrl(long websitePosition, BinaryWebsite newWebsite) {
		ByteBuffer buf = urlPortionWebsiteBuffer;
		buf.clear();
		BinaryWebsite.writeUrlPortion(buf, newWebsite);
		buf.flip();
		write(buf, BinaryWebsite.urlPortionPosition(websitePosition));
	}
	
	private void writeUpdatePassword(BinaryPassword oldPassword, BinaryPassword newPassword) {
		BinaryWebsite[] array = sortedWebsitesBuffer;
		int actualWebsiteCount = readWebsiteCount();
		long position = readPasswordOffset();
		long targetWebsiteId = oldPassword.websiteId();
		int size = BinaryPassword.occupiedSize(secretBlockSize);
		
		try {
			int curIndex = findExactIndex(targetWebsiteId, array, 0, actualWebsiteCount-1);
			if(curIndex == -1) {
				throw new StoreException(String.format("while update password, failed to find website with id=%d from store path: %s",
						targetWebsiteId, storePath));
			}
			BinaryWebsite existedWebsite = array[curIndex];
			long updatingPosition = position + existedWebsite.offset();
			int passwordCount = existedWebsite.count();
			updatingPosition = findPasswordPostion(updatingPosition, passwordCount, size, oldPassword);
			if(updatingPosition == -1) {
				throw new StoreException(String.format("while update password, failed to find password with id=%d and username=%s from store path: %s",
						targetWebsiteId, oldPassword.username(), storePath));
			}
			if(newPassword.isEncryptedPasswordChanged()) {
				if(newPassword.isEncryptedKeyValuePairsChanged()) {
					writePwdAndKvp(updatingPosition, newPassword);
				}else {
					writePwd(updatingPosition, newPassword);
				}
			}else {
				if(newPassword.isEncryptedKeyValuePairsChanged()) {
					writeKvp(updatingPosition, newPassword);
				}else {
					throw new StoreException(String.format("while update password, no changed found for password with"
							+ "website id=%d and username=%s in store path: %s",
							targetWebsiteId, oldPassword.username(), storePath));
				}
			}
		}catch(IOException e) {
			throw new StoreException(String.format("failed to update password with webiste id=%d and username=%s to store path: %s",
					targetWebsiteId, oldPassword.username(), storePath), e);
		}
	}
	
	private void writePwdAndKvp(long startPosition, BinaryPassword newPassword) {
		ByteBuffer buf = pwdAndKvpPortionPasswordBuffer;
		buf.clear();
		BinaryPassword.writePwdAndKvpPortion(buf, secretBlockSize, newPassword);
		buf.flip();
		write(buf, BinaryPassword.pwdAndKvpPortionPosition(startPosition));
	}
	
	private void writePwd(long startPosition, BinaryPassword newPassword) {
		ByteBuffer buf = pwdPortionPasswordBuffer;
		buf.clear();
		BinaryPassword.writePwdPortion(buf, secretBlockSize, newPassword);
		buf.flip();
		write(buf, BinaryPassword.pwdPortionPosition(startPosition));
	}
	
	private void writeKvp(long startPosition, BinaryPassword newPassword) {
		ByteBuffer buf = keyValuePairBuffer;
		buf.clear();
		BinaryPassword.writeKeyValuePair(buf, secretBlockSize, newPassword);
		buf.flip();
		write(buf, BinaryPassword.keyValuePairPosition(startPosition, secretBlockSize));
	}
	
	private void flushChanged() throws IOException { storeChannel.force(true); }

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
