package com.lee.password.keeper.impl.crypto;

import java.io.File;
import java.security.KeyPair;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.impl.crypto.rsa.RSACryptor;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyGenerator;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyReader;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyWriter;

public class RSACryptoTest {
	
	private static final String TEST_DIR = "/password-keeper/tmp";
	
	private static String publicKeyPath;
	private static String privateKeyPath;
	
	@BeforeClass
	public static void initKey() {
		int keySize = 1024;
		KeyPair keyPair = RSAKeyGenerator.generateKeyPair(keySize);
		File destDir = new File(TEST_DIR);
		File parent = RSAKeyWriter.initKeyDir(destDir.getAbsolutePath());
		Assert.assertTrue(parent != null && parent.isDirectory());
		int maxDataBlockSize = RSACryptor.maxDataBlockSize(keySize);
		publicKeyPath = RSAKeyWriter.serializePublicKey(keyPair.getPublic(), maxDataBlockSize, parent);
		Assert.assertNotNull(publicKeyPath);
		int maxSecretBlockSize = RSACryptor.maxSecretBlockSize(keySize);
		privateKeyPath = RSAKeyWriter.serializePrivateKey(keyPair.getPrivate(), maxSecretBlockSize, parent);
		Assert.assertNotNull(privateKeyPath);
	}
	
	@AfterClass
	public static void destroy() {
		if(publicKeyPath != null) { new File(publicKeyPath).deleteOnExit(); }
		if(privateKeyPath != null) { new File(privateKeyPath).deleteOnExit(); }
	}
	
	@Test
	public void testKeyReader() {
		File destDir = new File(TEST_DIR);
		File publicKeyFile = RSAKeyReader.detectPublicKey(destDir);
		CryptoKey publicKey = RSAKeyReader.deserializePublicKey(publicKeyFile);
		Assert.assertNotNull(publicKey);
		File privateKeyFile = RSAKeyReader.detectPrivateKey(destDir);
		CryptoKey privateKey = RSAKeyReader.deserializePrivateKey(privateKeyFile);
		Assert.assertNotNull(privateKey);
	}
	
	@Test
	public void testCryptor() {
		File destDir = new File(TEST_DIR);
		File publicKeyFile = RSAKeyReader.detectPublicKey(destDir);
		CryptoKey publicKey = RSAKeyReader.deserializePublicKey(publicKeyFile);
		Assert.assertNotNull(publicKey);
		File privateKeyFile = RSAKeyReader.detectPrivateKey(destDir);
		CryptoKey privateKey = RSAKeyReader.deserializePrivateKey(privateKeyFile);
		Assert.assertNotNull(privateKey);
		
		String expected = "012345678901234567890123456789";
		byte[] secret = RSACryptor.encrypt(expected.getBytes(), publicKey.encoded());
		Assert.assertTrue(secret != null && secret.length > 0);
		byte[] data = RSACryptor.decrypt(secret, privateKey.encoded());
		String decrypted = new String(data);
		Assert.assertEquals(expected, decrypted);
	}
}
