package com.lee.password.keeper.impl.crypto;

import java.io.File;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.impl.crypto.rsa.RSACryptor;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyGenerator;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyReader;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyWriter;

public class RSACryptoTest {
	
	@Test
	public void testKeyWriter() {
		int keySize = 1024;
		KeyPair keyPair = RSAKeyGenerator.generateKeyPair(keySize);
		String destDir = "E:/tmp/password-keeper";
		File parent = RSAKeyWriter.initKeyDir(destDir);
		Assert.assertTrue(parent != null && parent.isDirectory());
		int maxDataBlockSize = RSACryptor.maxDataBlockSize(keySize);
		String publicKeyPath = RSAKeyWriter.serializePublicKey(keyPair.getPublic(), maxDataBlockSize, parent);
		Assert.assertNotNull(publicKeyPath);
		int maxSecretBlockSize = RSACryptor.maxSecretBlockSize(keySize);
		String privateKeyPath = RSAKeyWriter.serializePrivateKey(keyPair.getPrivate(), maxSecretBlockSize, parent);
		Assert.assertNotNull(privateKeyPath);
		new File(publicKeyPath).deleteOnExit();
		new File(privateKeyPath).deleteOnExit();
	}
	
	@Test
	public void testKeyReader() {
		File destDir = new File("E:/tmp/password-keeper");
		File publicKeyFile = RSAKeyReader.detectPublicKey(destDir);
		CryptoKey publicKey = RSAKeyReader.deserializePublicKey(publicKeyFile);
		Assert.assertNotNull(publicKey);
		File privateKeyFile = RSAKeyReader.detectPrivateKey(destDir);
		CryptoKey privateKey = RSAKeyReader.deserializePrivateKey(privateKeyFile);
		Assert.assertNotNull(privateKey);
	}
	
	@Test
	public void testCryptor() {
		File destDir = new File("E:/tmp/password-keeper");
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
