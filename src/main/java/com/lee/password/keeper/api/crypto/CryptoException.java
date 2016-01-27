package com.lee.password.keeper.api.crypto;

public class CryptoException extends RuntimeException {

	private static final long serialVersionUID = -4086871919260126633L;

	public CryptoException(String msg) { super(msg); }
	
	public CryptoException(String msg, Exception e) { super(msg, e); }
}
