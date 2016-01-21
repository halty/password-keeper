package com.lee.password.keeper.impl.store.binary;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import com.lee.password.keeper.impl.util.Base64Variants;

public class BinaryPassword {

	private static final int MAX_USER_NAME_LEN = 48;	// 48 bytes
	private static final int MAX_PASSWORD_LEN = 128;	// 128 bytes
	private static final int MAX_KEY_VALUE_PAIR_LEN = 128;	// 128 bytes
	
	/** total bytes to place a binary password entry **/
	public static final int TOTAL_BYTES =
			(Integer.SIZE / Byte.SIZE)	// websiteId
			+ 1 + MAX_USER_NAME_LEN	// username (len + data)
			+ (Long.SIZE / Byte.SIZE)	// timestamp
			+ 1 + MAX_PASSWORD_LEN	// encryptedPassword (len + data)
			+ 1 + MAX_KEY_VALUE_PAIR_LEN;	// encryptedKeyValuePairs (len + data)
	
	private final long websiteId;
	private final byte[] username;
	private final long timestamp;
	private byte[] encryptedPassword;
	private byte[] encryptedKeyValuePairs;
	
	/**
	 * 0 - unchanged; 1- changed
	 * encryptedPassword - 0 bit
	 * encryptedKeyValuePairs - 1 bit
	 */
	private byte changedFlag;
	private static final int ENCRYPT_PWD_CHANGED_MASK = 0x01;
	private static final int ENCRYPT_KVP_CHANGED_MASK = 0x02;
	
	public BinaryPassword(long websiteId, String username, long timestamp) {
		this.websiteId = websiteId;
		this.username = toBytes(username, MAX_USER_NAME_LEN, "username");
		this.timestamp = timestamp;
		this.changedFlag = 0;
	}
	
	private byte[] toBytes(String value, int maxLength, String name) {
		byte[] bytes = Base64Variants.encode(value, CHARSET);
		if(bytes.length > maxLength) {
			throw new IllegalArgumentException(
					String.format("%s length exceed max limit (%d bytes)", name, maxLength));
		}
		return bytes;
	}

	public long websiteId() { return websiteId; }

	public String username() { return Base64Variants.decode(username, CHARSET); }

	public long timestamp() { return timestamp; }

	public byte[] encryptedPassword() { return encryptedPassword; }

	public void encryptedPassword(byte[] encryptedPassword) { this.encryptedPassword = encryptedPassword; }
	
	/** change the encrypted password and set the changed flag **/
	public void changeEncryptedPassword(byte[] encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
		changedFlag |= ENCRYPT_PWD_CHANGED_MASK;
	}

	public byte[] encryptedKeyValuePairs() { return encryptedKeyValuePairs; }

	public void encryptedKeyValuePairs(byte[] encryptedKeyValuePairs) { this.encryptedKeyValuePairs = encryptedKeyValuePairs; }
	
	/** change the encrypted key-value pair and set the changed flag **/
	public void changeEncryptedKeyValuePairs(byte[] encryptedKeyValuePairs) {
		this.encryptedKeyValuePairs = encryptedKeyValuePairs;
		changedFlag |= ENCRYPT_KVP_CHANGED_MASK;
	}
}
