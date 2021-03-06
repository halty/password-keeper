package com.lee.password.keeper.impl.store.binary;

import java.nio.ByteBuffer;

import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreException;
import com.lee.password.keeper.impl.InternalEntity;
import com.lee.password.util.Base64Variants;
import com.lee.password.util.BytePadding;

public class BinaryPassword implements InternalEntity {

	private static final int USER_NAME_LEN_SIZE = 1;
	private static final int SECRET_LEN_SIZE = 2;
	private static final int MAX_USER_NAME_LEN = 48;	// 48 bytes
	private static final int USER_NAME_OFFSET = 8;		// websiteId
	private static final int TIMESTAMP_OFFSET =
			USER_NAME_OFFSET
			+ USER_NAME_LEN_SIZE + MAX_USER_NAME_LEN;	// username (size + data)
	private static final int FIXED_OCCUPIED_BYTES =
			TIMESTAMP_OFFSET
			+ 8	// timestamp
			+ SECRET_LEN_SIZE + 0	// encryptedPassword (size + data)
			+ SECRET_LEN_SIZE + 0;	// encryptedKeyValuePairs (size + data)
	
	private final long websiteId;
	private final byte[] username;
	private long timestamp;
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
	public static int occupiedSize(int secretBlockSize) { return FIXED_OCCUPIED_BYTES + secretBlockSize * 2; }
	/** return the max username size(size + data length) **/
	public static int maxUsernameSize() { return 1 + MAX_USER_NAME_LEN; }
	public static long usernamePosition(long passwordPosition) { return passwordPosition + USER_NAME_OFFSET; }
	public static int pwdPortionSize(int secretBlockSize) { return 8 + SECRET_LEN_SIZE + secretBlockSize; }
	public static long pwdPortionPosition(long passwordPosition) { return passwordPosition + TIMESTAMP_OFFSET; }
	public static int pwdAndKvpPortionSize(int secretBlockSize) { return 8 + 2 * (SECRET_LEN_SIZE + secretBlockSize); }
	public static long pwdAndKvpPortionPosition(long passwordPosition) { return passwordPosition + TIMESTAMP_OFFSET; }
	public static int keyValuePairSize(int secretBlockSize) { return SECRET_LEN_SIZE + secretBlockSize; }
	public static long keyValuePairPosition(long passwordPosition, int secretBlockSize) {
		return passwordPosition + occupiedSize(secretBlockSize) - keyValuePairSize(secretBlockSize);
	}
	
	public static BinaryPassword read(ByteBuffer buffer, int secretBlockSize) {
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
	
	public static void write(ByteBuffer buffer, int secretBlockSize, BinaryPassword target) {
		buffer.putLong(target.websiteId);
		int len = target.username.length;
		buffer.put((byte)len);
		buffer.put(target.username);
		BytePadding.padding(buffer, MAX_USER_NAME_LEN - len);
		buffer.putLong(target.timestamp);
		len = target.encryptedPassword.length;
		buffer.putShort((short)len);
		buffer.put(target.encryptedPassword);
		BytePadding.padding(buffer, secretBlockSize - len);
		len = target.encryptedKeyValuePairs.length;
		buffer.putShort((short)len);
		if(len > 0) { buffer.put(target.encryptedKeyValuePairs); }
		BytePadding.padding(buffer, secretBlockSize - len);
	}
	
	public static void writePwdPortion(ByteBuffer buffer, int secretBlockSize, BinaryPassword target) {
		buffer.putLong(target.timestamp);
		int len = target.encryptedPassword.length;
		buffer.putShort((short)len);
		buffer.put(target.encryptedPassword);
		BytePadding.padding(buffer, secretBlockSize - len);
	}
	
	public static void writePwdAndKvpPortion(ByteBuffer buffer, int secretBlockSize, BinaryPassword target) {
		buffer.putLong(target.timestamp);
		int len = target.encryptedPassword.length;
		buffer.putShort((short)len);
		buffer.put(target.encryptedPassword);
		BytePadding.padding(buffer, secretBlockSize - len);
		len = target.encryptedKeyValuePairs.length;
		buffer.putShort((short)len);
		if(len > 0) { buffer.put(target.encryptedKeyValuePairs); }
		BytePadding.padding(buffer, secretBlockSize - len);
	}
	
	public static void writeKeyValuePair(ByteBuffer buffer, int secretBlockSize, BinaryPassword target) {
		int len = target.encryptedKeyValuePairs.length;
		buffer.putShort((short)len);
		if(len > 0) { buffer.put(target.encryptedKeyValuePairs); }
		BytePadding.padding(buffer, secretBlockSize - len);
	}
	
	public static boolean hasEqualUsername(BinaryPassword one, ByteBuffer usernameBuf) {
		int length = one.username.length;
		if(usernameBuf.remaining() <= length) { return false; }
		int len = usernameBuf.get();
		if(len != length) { return false; }
		return byteCompare(one.username, usernameBuf);
	}
	
	private static boolean byteCompare(byte[] one, ByteBuffer usernameBuf) {
		for(int i=0; i<one.length; i++) {
			if(one[i] != usernameBuf.get()) { return false; }
		}
		return true;
	}
	
	public static boolean hasEqualUsername(BinaryPassword one, BinaryPassword another) {
		return byteCompare(one.username, another.username);
	}
	
	private static boolean byteCompare(byte[] one, byte[] another) {
		if(one == another) { return true; }
		int oneLen = one.length;
		int anotherLen = another.length;
		if(oneLen != anotherLen) { return false; }
		for(int i=0; i<oneLen; i++) {
			if(one[i] != another[i]) { return false; }
		}
		return true;
	}
	
	public static boolean hasEqualWebsiteId(BinaryPassword one, BinaryPassword another) {
		return one.websiteId == another.websiteId;
	}
	
	public static boolean hasEqualPassword(BinaryPassword one, BinaryPassword another) {
		return byteCompare(one.encryptedPassword, another.encryptedPassword);
	}
	
	public static boolean hasEqualKeyValuePair(BinaryPassword one, BinaryPassword another) {
		return byteCompare(one.encryptedKeyValuePairs, another.encryptedKeyValuePairs);
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
		if(value == null || value.isEmpty()) {
			throw new StoreException(name + "is empty");
		}
		byte[] bytes = Base64Variants.encode(value, CHARSET);
		if(bytes.length > maxLength) {
			throw new StoreException(
					String.format("%s length exceed max limit (%d bytes)", name, maxLength));
		}
		return bytes;
	}
	
	public BinaryPassword copy() {
		BinaryPassword password = new BinaryPassword(websiteId, username, timestamp);
		password.encryptedPassword(encryptedPassword);
		password.encryptedKeyValuePairs(encryptedKeyValuePairs);
		return password;
	}
	
	public Password transformWithoutSecret() { return new Password(websiteId, username(), timestamp); }

	public long websiteId() { return websiteId; }

	public String username() { return Base64Variants.decode(username, CHARSET); }

	public long timestamp() { return timestamp; }
	
	public void timestamp(long timestamp) { this.timestamp = timestamp; }

	public byte[] encryptedPassword() { return encryptedPassword; }

	public void encryptedPassword(byte[] encryptedPassword) { this.encryptedPassword = encryptedPassword; }

	public byte[] encryptedKeyValuePairs() { return encryptedKeyValuePairs; }

	public void encryptedKeyValuePairs(byte[] encryptedKeyValuePairs) { this.encryptedKeyValuePairs = encryptedKeyValuePairs; }
	
	/** mark the encrypted password changed flag **/
	public void markEncryptedPasswordChanged() { changedFlag |= ENCRYPT_PWD_CHANGED_MASK; }
	
	/** mark the encrypted key-value pair changed flag **/
	public void markEncryptedKeyValuePairsChanged() { changedFlag |= ENCRYPT_KVP_CHANGED_MASK; }
	
	public boolean isEncryptedPasswordChanged() { return (changedFlag & ENCRYPT_PWD_CHANGED_MASK) != 0; }
	
	public boolean isEncryptedKeyValuePairsChanged() { return (changedFlag & ENCRYPT_KVP_CHANGED_MASK) != 0; }

	@Override
	public Type type() { return Type.PASSWORD; }
}
