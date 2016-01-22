package com.lee.password.keeper.api.store;

import java.util.List;

import com.lee.password.keeper.api.Result;

public interface StoreDriver {

	Result<Website> insertWebsite(Website website);
	
	Result<Website> deleteWebsite(Website website);
	
	Result<Website> updateWebsite(Website website);
	
	Result<Website> selectWebsite(Website website);
	
	/** list all website entry **/ 
	Result<List<Website>> listWebsite();
	
	Result<Password> insertPassword(Password entry);
	
	Result<Password> deletePassword(Password.Header header);
	
	Result<Password> updatePassword(Password entry);
	
	Result<Password> selectPassword(Password.Header header);
	
	/**
	 * list all password entry with only <code>website</code> and <code>username</code>
	 * by <code>website id</code>.
	 */ 
	Result<List<Password.Header>> listPassword(int websiteId);
	
	/**
	 * list all password entry with only <code>website</code> and <code>username</code>
	 * by <code>username</code>.
	 */
	Result<List<Password.Header>> listPassword(String username);
	
	/**
	 * undo the last changed operation, return the undo target entry if success.
	 * subsequent undo call will be failed after you call {@link #flush()}. 
	 */
	Result<?> undo();
	
	/** redo the last changed operation, return the redo target entry if success **/
	Result<?> redo();
	
	/** flush all the changed operation to the underlying storage **/
	Result<Throwable> flush();
	
	/** close this driver and releases any system resources associated with the driver. **/
	Result<Throwable> close();
}
