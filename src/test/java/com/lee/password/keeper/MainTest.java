package com.lee.password.keeper;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.lee.password.keeper.impl.crypto.rsa.RSACryptor;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyGenerator;

public class MainTest {

	public static void main(String[] args) throws Exception {
		testByteBuffer();
	}
	
	private static void testByteBuffer() {
		ByteBuffer buf = ByteBuffer.allocate(10);
		for(int i=0; i<10; i++) { buf.put((byte)i); }
		buf.flip();
		System.out.println(buf.get() == 0);
		System.out.println(buf.get() == 1);
		buf.position(6);
		System.out.println(buf.get() == 6);
		buf.position(9);
		System.out.println(buf.get() == 9);
	}

	private static void testEpoch() throws Exception {
		long timestamp = System.currentTimeMillis();
		System.out.println(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date d = sdf.parse("2016-01-01 00:00:00 GMT");
		long epoch = d.getTime();
		System.out.println(epoch);
		System.out.println(timestamp - epoch);
		System.out.println(new Date(epoch+ (1L << 40)).toGMTString());
	}
	
	private static void testCrypto() throws Exception {
		KeyPair keyPair = RSAKeyGenerator.generateKeyPair(1024);
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		// String data = "13641815806";
		String str = "halty86@gmail.com";
		byte[] data = str.getBytes("UTF-8");
		byte[] publicEncoded = publicKey.getEncoded();
		byte[] privateEncoded = privateKey.getEncoded();
		System.out.println(data.length);
		System.out.println(publicEncoded.length);
		System.out.println(privateEncoded.length);
		byte[] secret = RSACryptor.encrypt(data, publicEncoded);
		System.out.println(secret.length);
		byte[] bytes = RSACryptor.decrypt(secret, privateEncoded);
		System.out.println(new String(bytes, "UTF-8"));
	}
}
