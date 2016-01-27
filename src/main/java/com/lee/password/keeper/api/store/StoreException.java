package com.lee.password.keeper.api.store;

public class StoreException extends RuntimeException {

	private static final long serialVersionUID = -2753360581503431448L;

	public StoreException(String msg) { super(msg); }
	
	public StoreException(String msg, Exception e) { super(msg, e); }
}
