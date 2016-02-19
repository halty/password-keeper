package com.lee.password.util;

import java.nio.ByteBuffer;

/** Mask byte value by xor and flip opertaion **/
public class ByteMask {

	/**
	 * Mask the {@code srcBytes} array and put them to {@code destBuf} without modify {@code srcBytes}.
	 * If the remaining bytes of {@code destBuf} less than {@code srcBytes.length},
	 * throw {@link IndexOutOfBoundsException}
	 */
	public static void maskAndPut(byte[] srcBytes, ByteBuffer destBuf) {
		int remaining = destBuf.remaining();
		int count = srcBytes.length;
		if(remaining < count) {
			throw new IndexOutOfBoundsException(String.format("destination buffer has remaining %d bytes, "
					+ "it is not enough for %d source bytes array", remaining, count));
		}
		for(int i=0; i<count; i++) { destBuf.put(mask(i, srcBytes[i])); }
	}
	
	/** Mask the {@code bytes} array. Equals {@link #mask(byte[], int, int) mask(bytes, 0, bytes.length)} */
	public static void mask(byte[] bytes) {
		mask(bytes, 0, bytes.length);
	}
	
	/**
	 * Mask the range of {@code bytes} array. The range to be masked extends from
	 * the index {@code fromIndex}, inclusive, to the index {@code toIndex}, exclusive.
	 * If {@code fromIndex == toIndex}, the range to be masked is empty.
	 * If the range is invalid for {@code bytes}, throw {@link IndexOutOfBoundsException}.
	 */
	public static void mask(byte[] bytes, int fromIndex, int toIndex) {
		if(!isValid(bytes, fromIndex, toIndex)) {
			throw new IndexOutOfBoundsException(String.format("invalid range [%d, %d) for %d bytes array",
					fromIndex, toIndex, bytes.length));
		}
		for(int i=fromIndex; i<toIndex; i++) { bytes[i] = mask(i, bytes[i]); }
	}
	
	private static boolean isValid(byte[] bytes, int fromIndex, int toIndex) {
		return 0 <= fromIndex && fromIndex <= toIndex && toIndex <= bytes.length;
	}
	
	private static byte mask(int index, byte b) { return flip(index, xor(index, b)); }
	
	private static byte xor(int index, byte b) { return (byte) (b ^ index); }
	private static byte flip(int index, byte b) {
		switch(b) {
		case Byte.MIN_VALUE:
			return b;
		default:
			return (index & 0x01) != 0 ? b : (byte)(~b);
		}
	}
	
	/**
	 * get {@code length} bytes from {@code srcBuf} and unmask them.
	 * If the remaining bytes of {@code srcBuf} less than {@code length},
	 * throw {@link IndexOutOfBoundsException}
	 */
	public static byte[] getAndUnmask(ByteBuffer srcBuf, int length) {
		int remaining = srcBuf.remaining();
		if(remaining < length) {
			throw new IndexOutOfBoundsException(String.format("source buffer has remaining %d bytes, "
					+ "it is not enough for get %d bytes array", remaining, length));
		}
		byte[] bytes = new byte[length];
		srcBuf.get(bytes);
		unmask(bytes);
		return bytes;
	}
	
	/** Unmask the {@code bytes} array. Equals Equals {@link #unmask(byte[], int, int) unmask(bytes, 0, bytes.length)} */
	public static void unmask(byte[] bytes) {
		unmask(bytes, 0, bytes.length);
	}
	
	/**
	 * Unmask the range of {@code bytes} array. The range to be unmasked extends from
	 * the index {@code fromIndex}, inclusive, to the index {@code toIndex}, exclusive.
	 * If {@code fromIndex == toIndex}, the range to be unmasked is empty.
	 * If the range is invalid for {@code bytes}, throw {@link IndexOutOfBoundsException}.
	 */
	public static void unmask(byte[] bytes, int fromIndex, int toIndex) {
		if(!isValid(bytes, fromIndex, toIndex)) {
			throw new IndexOutOfBoundsException(String.format("invalid range [%d, %d) for %d bytes array",
					fromIndex, toIndex, bytes.length));
		}
		for(int i=fromIndex; i<toIndex; i++) { bytes[i] = unmask(i, bytes[i]); }
	}
	
	private static byte unmask(int index, byte b) { return xor(index, flip(index, b)); }
}
