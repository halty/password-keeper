package com.lee.password.keeper.impl.store;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.impl.store.binary.BinaryPassword;
import com.lee.password.keeper.impl.store.binary.BinaryWebsite;
import com.lee.password.keeper.impl.store.binary.ChangedOperation;

public class BinaryStoreDriver implements StoreDriver {

	private static final String DATA_FILE = "binary.store.pk";
	
	private static final byte[] MAGIC = "bpsd".getBytes(CHARSET);
	
	/** encryption/decryption driver  **/
	private CryptoDriver cryptoDriver;
	
	/** storage resources **/
	private File storePath;
	private RandomAccessFile storeMappedFile;
	private FileChannel storeChannel;
	private FileLock storeLock;
	
	/** the number of website **/
	private int websiteCount;
	/** the offset of website in channel **/
	private long websiteOffset;
	/** the number of password **/
	private int passwordCount;
	/** the offset of password in channel **/
	private long passwordOffset;
	
	/** index website and password **/
	private Map<Long, BinaryWebsite> websiteIdMap;
	private Map<String, BinaryWebsite> websiteKeywordMap;
	private Map<Long, List<BinaryPassword>> websiteIdPwdMap;
	private Map<String, List<BinaryPassword>> usernamePwdMap;
	
	/** change operation queue **/
	private PriorityQueue<ChangedOperation<BinaryWebsite>> websiteOpQueue;
	private PriorityQueue<ChangedOperation<BinaryPassword>> passwordOpQueue;
	
	private boolean isClosed;
	
	public BinaryStoreDriver(String dataDir, CryptoDriver cryptoDriver) {
		try {
			this.cryptoDriver = cryptoDriver;
			this.storePath = createIfNotExisted(dataDir);
			this.storeMappedFile = new RandomAccessFile(storePath, "rw");
			this.storeChannel = storeMappedFile.getChannel();
			this.storeLock = storeChannel.lock();
			
			load();
		}catch(Exception e) {
			release();
			throw new IllegalStateException("failed to init binary store driver from data directory: "+dataDir);
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
	
	/** load all the data from file to memory **/
	private void load() {
		
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
	public boolean flush() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
