package com.lee.password.keeper.api.crypto;

import com.lee.password.keeper.api.Entity;

public class CryptoKey implements Entity {

	public static enum KeyType {
		PUBLIC(1),
		PRIVATE(2),
		;
		public final byte code;
		private KeyType(int code) { this.code = (byte) code; }
	}
	
	private final KeyType type;
	private final int maxBlockSize;	// support max block size for encryption/decryption
	private final String path;
	private final byte[] encoded;
	
	public CryptoKey(KeyType type, int maxBlockSize, String path, byte[] encoded) {
		this.type = type;
		this.maxBlockSize = maxBlockSize;
		this.path = path;
		this.encoded = encoded;
	}

	public KeyType keyType() { return type; }

	public int maxBlockSize() { return maxBlockSize; }

	public String path() { return path; }

	public byte[] encoded() { return encoded; }

	@Override
	public Type type() { return Type.KEY; }
}
