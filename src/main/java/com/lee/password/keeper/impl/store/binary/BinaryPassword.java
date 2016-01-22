package com.lee.password.keeper.impl.store.binary;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import java.nio.ByteBuffer;

import com.lee.password.keeper.impl.util.Base64Variants;

public class BinaryPassword {

	private static final int MAX_USER_NAME_LEN = 48;	// 48 bytes
	public static final long FIXED_OCCUPIED_BYTES =
			8	// websiteId
			+ 1 + MAX_USER_NAME_LEN	// username (size + data)
			+ 8	// timestamp
			+ 2 + 0	// encryptedPassword (size + data)
			+ 2 + 0;	// encryptedKeyValuePairs (size + data)
	
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
	
	/** return the bytes of this object occupied in store file **/
	public static long occupiedSize(int secretBlockSize) { return FIXED_OCCUPIED_BYTES + secretBlockSize * 2; }
	
	public static BinaryPassword load(ByteBuffer buffer, int secretBlockSize) {
		long websiteId = buffer.getLong();
		int len = 0xff & buffer.get();
		byte[] username = new byte[len];
		buffer.get(username);
		buffer.position(buffer.position() + (MAX_USER_NAME_LEN - len)); // skip remaining bytes with username slot
		long timestamp = buffer.getLong();
		len = 0xffff & buffer.getShort();
		byte[] encryptedPassword = new byte[len];
		buffer.get(encryptedPassword);
		buffer.position(buffer.position() + (secretBlockSize - len)); // skip remaining bytes with encrypted password slot
		len = 0xffff & buffer.getShort();
		byte[] encryptedKeyValuePairs = new byte[len];
		if(len > 0) { buffer.get(encryptedKeyValuePairs); }		// maybe empty
		BinaryPassword password = new BinaryPassword(websiteId, username, timestamp);
		password.encryptedPassword(encryptedPassword);
		password.encryptedKeyValuePairs(encryptedKeyValuePairs);
		return password;
	}
	
	private BinaryPassword(long websiteId, byte[] usernameBytes, long timestamp) {
		this.websiteId = websiteId;
		this.username = usernameBytes;
		this.timestamp = timestamp;
		this.changedFlag = 0;
	}
	
	public BinaryPassword(long websiteId, String username, long timestamp) {
		this(websiteId,
			 toBytes(username, MAX_USER_NAME_LEN, "username"),
			 timestamp);
	}
	
	private static byte[] toBytes(String value, int maxLength, String name) {
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
