package com.lee.password.keeper.api.store;

import java.util.List;

import com.lee.password.keeper.api.Entity;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoKey;

/**
 * Each implementation of {@link StoreDriver} must provides a special constructor
 * with follow exact signature: <pre>
 * public xxxStoreDriver(String dataDir, {@link com.lee.password.keeper.api.crypto.CryptoDriver CryptoDriver} cryptoDriver,
 *                       int secretBlockSize, boolean isStoreFileLock)
 *   dataDir -- where the data store file stored in;
 *   cryptoDriver -- encrypt/decrypt driver;
 *   secretBlockSize -- the bit size of secret block for asymmetric encryption;
 *   isStoreFileLock -- whether the data store file is locked or not
 * </pre>
 */
public interface StoreDriver {
	
	/** return the store file path if it is local file storage, otherwise undefined **/
	Result<String> storePath();

	Result<Website> insertWebsite(Website website);
	
	Result<Website> deleteWebsite(Website website);
	
	Result<Website> updateWebsite(Website website);
	
	Result<Website> selectWebsite(Website website);
	
	Result<Integer> websiteCount();
	
	/** list all website entry **/ 
	Result<List<Website>> listWebsite();
	
	Result<Password.Header> insertPassword(Password entry, CryptoKey encryptionKey);
	
	Result<Password.Header> deletePassword(Password.Header header);
	
	Result<Password.Header> updatePassword(Password entry, CryptoKey encryptionKey);
	
	Result<Password> selectPassword(Password.Header header, CryptoKey decryptionKey);
	
	Result<Integer> passwordCount();
	
	Result<Integer> passwordCount(long websiteId);
	
	Result<Integer> passwordCount(String username);
	
	Result<Integer> passwordCount(long websiteId, String username);
	
	/**
	 * list all password entry with only <code>website</code> and <code>username</code>
	 * by <code>website id</code>.
	 */ 
	Result<List<Password.Header>> listPassword(long websiteId);
	
	/**
	 * list all password entry with only <code>website</code> and <code>username</code>
	 * by <code>username</code>.
	 */
	Result<List<Password.Header>> listPassword(String username);
	
	Result<Password.Header> listPassword(long websiteId, String username);
	
	Result<Integer> canUndoTimes();
	
	/**
	 * undo the last change operation, return the undo target entry if success.
	 * subsequent undo call will be failed after you call {@link #flush()}. 
	 */
	Result<Entity> undo();
	
	Result<Integer> canRedoTimes();
	
	/** redo the last change operation, return the redo target entry if success **/
	Result<Entity> redo();
	
	Result<Integer> needCommitCount();
	
	/** flush all the change operation to the underlying storage **/
	Result<Throwable> commit();
	
	/** close this driver and releases any system resources associated with the driver. **/
	Result<Throwable> close();
}
