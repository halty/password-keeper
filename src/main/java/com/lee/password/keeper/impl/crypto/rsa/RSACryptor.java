package com.lee.password.keeper.impl.crypto.rsa;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class RSACryptor implements RSAConstants {
	
	public static byte[] encrypt(byte[] data, byte[] publicKey) {
		try {
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			PublicKey key = keyFactory.generatePublic(keySpec);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(data);
		}catch(Exception e) {
			throw new RuntimeException("failed to encrypt data with "+TRANSFORMATION, e);
		}
	}
	
	public static byte[] decrypt(byte[] secret, byte[] privateKey) {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			PrivateKey key = keyFactory.generatePrivate(keySpec);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(secret);
		}catch(Exception e) {
			throw new RuntimeException("failed to decrypt secret with "+TRANSFORMATION, e);
		}
	}
	
	/** compute the support max data block size for public key encryption **/
	public static int maxDataBlockSize(int keySize) {
		return keySize / Byte.SIZE - PADDING_LENGTH;
	}
	
	/** compute the support max secret block size for private key decryption **/
	public static int maxSecretBlockSize(int keySize) {
		return keySize / Byte.SIZE;
	}
}
