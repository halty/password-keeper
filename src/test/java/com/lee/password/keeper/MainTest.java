package com.lee.password.keeper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.lee.password.keeper.impl.crypto.rsa.RSACryptor;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyGenerator;

public class MainTest {

	public static void main(String[] args) throws Exception {
		// testByteBuffer();
		// testCrypto();
		String path = "e:/tmp/tmp.pk";
		File file = new File(path);
		file.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(file);
		for(int i=0; i<10; i++) { fos.write(i); }
		fos.close();
		
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		FileChannel channel = raf.getChannel();
		
		FileInputStream fis = null;
		System.out.println("source: ");
		fis = new FileInputStream(file);
		int b = 0;
		while((b=fis.read()) != -1) {
			System.out.print(b);
			System.out.print(" ");
		}
		fis.close();
		System.out.println();
		
		long size = channel.size();
		System.out.println("size="+size);
		int position = 5;
		long count = size - position;
		int expand = 10;
		ByteBuffer buf = ByteBuffer.allocate(expand).order(ByteOrder.BIG_ENDIAN);
		for(int i=0; i<expand; i++) {
			byte bt = (byte) (expand + i);
			buf.put(bt);
		}
		buf.flip();
		
		channel.position(position+expand);
		channel.transferTo(position, count, channel);
		channel.write(buf, position);
		channel.force(true);
		size = channel.size();
		
		System.out.println("after expand: ");
		System.out.println("new size="+size);
		fis = new FileInputStream(file);
		b = 0;
		while((b=fis.read()) != -1) {
			System.out.print(b);
			System.out.print(" ");
		}
		fis.close();
		System.out.println();

		position = 10;
		int decreament = 5;
		long nextPosition = position+decreament;
		count = size - nextPosition;
		channel.position(nextPosition);
		channel.transferFrom(channel, position, count);
		channel.truncate(size - decreament);
		channel.force(true);
		size = channel.size();
		
		System.out.println("after truncate: ");		
		System.out.println("new size="+size);
		fis = new FileInputStream(file);
		b = 0;
		while((b=fis.read()) != -1) {
			System.out.print(b);
			System.out.print(" ");
		}
		fis.close();
		System.out.println();
		
		channel.close();
		raf.close();
	}
	
	private static String same(String str) { return str; }
	
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
		String str = "";
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
