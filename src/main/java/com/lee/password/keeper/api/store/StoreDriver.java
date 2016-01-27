package com.lee.password.keeper.api.store;

import java.util.List;

import com.lee.password.keeper.api.Entity;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoKey;

public interface StoreDriver {

	Result<Website> insertWebsite(Website website);
	
	Result<Website> deleteWebsite(Website website);
	
	Result<Website> updateWebsite(Website website);
	
	Result<Website> selectWebsite(Website website);
	
	Result<Integer> websiteCount();
	
	/** list all website entry **/ 
	Result<List<Website>> listWebsite();
	
	Result<Password.Header> insertPassword(Password entry, CryptoKey encryptKey);
	
	Result<Password.Header> deletePassword(Password.Header header);
	
	Result<Password.Header> updatePassword(Password entry, CryptoKey encryptKey);
	
	Result<Password> selectPassword(Password.Header header, CryptoKey decryptKey);
	
	Result<Integer> passwordCount();
	
	Result<Integer> passwordCount(long websiteId);
	
	Result<Integer> passwordCount(String username);
	
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
	
	Result<Integer> canUndoTimes();
	
	Result<Integer> canRedoTimes();
	
	/**
	 * undo the last changed operation, return the undo target entry if success.
	 * subsequent undo call will be failed after you call {@link #flush()}. 
	 */
	Result<Entity> undo();
	
	/** redo the last changed operation, return the redo target entry if success **/
	Result<Entity> redo();
	
	Result<Integer> needFlushCount();
	
	/** flush all the changed operation to the underlying storage **/
	Result<Throwable> flush();
	
	/** close this driver and releases any system resources associated with the driver. **/
	Result<Throwable> close();
}
