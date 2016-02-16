package com.lee.password.keeper.impl.util;

import java.nio.ByteBuffer;
import java.util.Random;

/** random padding bytes to ByteBuffer */
public class BytePadding {
	
	private static final Random RAND = new Random();
	private static final int UPPER_BOUND = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;

	/** Random padding byte to {@code buf} from current position to limit */
	public static void padding(ByteBuffer buf) {
		while(buf.hasRemaining()) { buf.put(randomByte()); }
	}
	
	/**
	 * Random padding byte to {@code buf} from current position to limit.
	 * If remaining bytes of {@code buf} equals or more than {@code paddingLength}, padding
	 * {@code paddingLength} bytes; otherwise, just padding the remaining bytes.
	 */
	public static void padding(ByteBuffer buf, int paddingLength) {
		int count = buf.remaining();
		if(count > paddingLength) { count = paddingLength; }
		while(count-- > 0) { buf.put(randomByte()); }
	}
	
	private static byte randomByte() { return (byte) (RAND.nextInt(UPPER_BOUND) + Byte.MIN_VALUE); }
}
