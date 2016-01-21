package com.lee.password.keeper.api.crypto;

public class CryptoKey {

	public static enum Type {
		PUBLIC(1),
		PRIVATE(2),
		;
		public final byte code;
		private Type(int code) { this.code = (byte) code; }
	}
	
	private final Type type;
	private final int maxBlockSize;	// support max block size for encryption/decryption
	private final String path;
	private final byte[] encoded;
	
	public CryptoKey(Type type, int maxBlockSize, String path, byte[] encoded) {
		this.type = type;
		this.maxBlockSize = maxBlockSize;
		this.path = path;
		this.encoded = encoded;
	}

	public Type type() { return type; }

	public int maxBlockSize() { return maxBlockSize; }

	public String path() { return path; }

	public byte[] encoded() { return encoded; }
}
