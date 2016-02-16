package com.lee.password.keeper.impl.util;

import java.nio.charset.Charset;

/**
 * Static methods for translating string with charset to Base64 encoded bytes
 * and vice-versa.
 */
public class Base64Variants {
	
	/**
     * Translates the specified string with charset into an "alternate representation"
     * Base64 byte array.  This non-standard variant uses an alphabet that does
     * not contain the uppercase alphabetic characters, which makes it
     * suitable for use in situations where case-folding occurs.
     */
	public static byte[] encode(String source, Charset charset) {
		byte[] sourceBytes = source.getBytes(charset);
		
		int groups = sourceBytes.length / 3;
		int remainBytes = sourceBytes.length - 3 * groups;
		int resultLen = ((sourceBytes.length - 1) / 3 + 1) * 4;
		byte[] result = new byte[resultLen];
		byte[] byteToBase64Table = BYTE_TO_BASE64;
		
		int inPos = 0;
		int outPos = 0;
		// Translate all full groups from byte array elements to Base64 bytes
		for(int i=0; i<groups; i++) {
			int b0 = sourceBytes[inPos++] & 0xff;
			int b1 = sourceBytes[inPos++] & 0xff;
			int b2 = sourceBytes[inPos++] & 0xff;
			result[outPos++] = byteToBase64Table[b0 >> 2];
			result[outPos++] = byteToBase64Table[(b0 << 4)&0x3f | (b1 >> 4)];
			result[outPos++] = byteToBase64Table[(b1 << 2)&0x3f | (b2 >> 6)];
			result[outPos++] = byteToBase64Table[b2 & 0x3f];
		}
		// translate partial group if present
		if(remainBytes > 0) {
			int b0 = sourceBytes[inPos++] & 0xff;
			result[outPos++] = byteToBase64Table[b0 >> 2];
			if(remainBytes == 1) {
				result[outPos++] = byteToBase64Table[(b0 << 4) & 0x3f];
				result[outPos++] = PADDING_BYTE;
				result[outPos++] = PADDING_BYTE;
			}else {
				// assert remainBytes == 2;
				int b1 = sourceBytes[inPos++] & 0xff;
				result[outPos++] = byteToBase64Table[(b0 << 4)&0x3f | (b1 >> 4)];
				result[outPos++] = byteToBase64Table[(b1 << 2) & 0x3f];
				result[outPos++] = PADDING_BYTE;
			}
		}
		// assert inPos == sourceBytes.length;
		// assert outPos == result.length;
		return result;
	}
	
	/**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Alternate Base64 Alphabet" equivalents.
     * This is NOT the real Base64 Alphabet as per in Table 1 of RFC 2045.
     * This alternate alphabet does not use the capital letters.  It is
     * designed for use in environments where "case folding" occurs.
     */
	private static final byte[] BYTE_TO_BASE64 = {
        '!', '"', '#', '$', '%', '&', '\'', '(', ')', ',', '-', '.', ':',
        ';', '<', '>', '@', '[', ']', '^',  '`', '_', '{', '|', '}', '~',
        'a', 'b', 'c', 'd', 'e', 'f', 'g',  'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't',  'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6',  '7', '8', '9', '+', '?'
    };
	
	/** padding byte for partial group with Base64 **/
	private static final byte PADDING_BYTE = '=';
	
	/**
     * This array is the analogue of BASE64_TO_BYTE, but for the nonstandard
     * variant that avoids the use of uppercase alphabetic characters.
     */
	private static final byte[] BASE64_TO_BYTE = {
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1,  0,  1,  2,  3,  4,  5,  6,  7,  8, -1, 62,  9, 10, 11, -1,
		52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 12, 13, 14, -1, 15, 63,
		16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, 18, 19, 21,
		20, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
		41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 22, 23, 24, 25, -1
    };
	
	/**
     * Translates the specified "alternate representation" Base64 byte array
     * into a string with charset.
     * 
     * @throw IllegalArgumentException or ArrayOutOfBoundsException
     *        if <tt>encoded</tt> is not a valid alternate representation
     *        Base64 byte.
     */
	public static String decode(byte[] encoded, Charset charset) {
		int len = encoded.length;
		int numGroups = len / 4;
		if(numGroups*4 != len) {
			throw new IllegalArgumentException("encoded length must be a multiple of 4 for base64");
		}
		int groups = numGroups;
		int missingBytes = 0;
		if(encoded[len-1] == PADDING_BYTE) {
			missingBytes++;
			groups--;
		}
		if(encoded[len-2] == PADDING_BYTE) {
			missingBytes++;
		}
		byte[] result = new byte[3*numGroups - missingBytes];
		byte[] base64ToByteTable = BASE64_TO_BYTE;
		
		// Translate all full groups from base64 bytes to byte array elements
		int inPos = 0;
		int outPos = 0;
		for(int i=0; i<groups; i++) {
			int b0 = base64ToByteTable[encoded[inPos++]] & 0xff;
			int b1 = base64ToByteTable[encoded[inPos++]] & 0xff;
			int b2 = base64ToByteTable[encoded[inPos++]] & 0xff;
			int b3 = base64ToByteTable[encoded[inPos++]] & 0xff;
			result[outPos++] = (byte) ((b0 << 2) | (b1 >> 4));
			result[outPos++] = (byte) ((b1 << 4) | (b2 >> 2));
			result[outPos++] = (byte) ((b2 << 6) | b3);
		}
		// Translate partial group, if present
		if(missingBytes > 0) {
			int b0 = base64ToByteTable[encoded[inPos++]] & 0xff;
			int b1 = base64ToByteTable[encoded[inPos++]] & 0xff;
			result[outPos++] = (byte) ((b0 << 2) | (b1 >> 4));
			if(missingBytes == 1) {
				int b2 = base64ToByteTable[encoded[inPos++]] & 0xff;
				result[outPos++] = (byte) ((b1 << 4) | (b2 >> 2));
			}
		}
		// assert inPos == encoded.length;
		// assert outPos == result.length;
		return new String(result, charset);
	}

}
