package com.lee.password.keeper.impl.crypto;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.Result.Code;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.crypto.CryptoKey.Type;
import com.lee.password.keeper.impl.crypto.rsa.RSACryptor;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyGenerator;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyReader;
import com.lee.password.keeper.impl.crypto.rsa.RSAKeyWriter;

public class RSACryptoDriver implements CryptoDriver {

	private boolean isClosed;
	
	@Override
	public Result<CryptoKey[]> generateKeyPair(String destDirectory, int keySize) {
		File destDir = null;
		try {
			destDir = RSAKeyWriter.initKeyDir(destDirectory);
		}catch(Exception e) {
			return new Result<CryptoKey[]>(Code.FAIL, e.getMessage());
		}
		
		KeyPair keyPair = null;
		try {
			keyPair = RSAKeyGenerator.generateKeyPair(keySize);
		}catch(Exception e) {
			return new Result<CryptoKey[]>(Code.FAIL, e.getMessage());
		}
		
		try {
			PublicKey publicKey = keyPair.getPublic();
			int maxDataBlockSize = RSACryptor.maxDataBlockSize(keySize);
			String publicKeyPath = RSAKeyWriter.serializePublicKey(publicKey, maxDataBlockSize, destDir);
			PrivateKey privateKey = keyPair.getPrivate();
			int maxSecretBlockSize = RSACryptor.maxSecretBlockSize(keySize);
			String privateKeyPath = RSAKeyWriter.serializePrivateKey(privateKey, maxSecretBlockSize, destDir);
			CryptoKey[] resultKey = new CryptoKey[] {
					new CryptoKey(Type.PUBLIC, maxDataBlockSize, publicKeyPath, publicKey.getEncoded()),
					new CryptoKey(Type.PRIVATE, maxSecretBlockSize, privateKeyPath, privateKey.getEncoded())
			};
			return new Result<CryptoKey[]>(Code.SUCCESS, "success", resultKey);
		}catch(Exception e) {
			return new Result<CryptoKey[]>(Code.FAIL, e.getMessage());
		}
	}

	@Override
	public Result<CryptoKey> loadPublicKey(String destDirectory) {
		File publicKeyFile = null;
		try {
			publicKeyFile = RSAKeyReader.detectPublicKey(new File(destDirectory));
		}catch(Exception e) {
			return new Result<CryptoKey>(Code.FAIL, e.getMessage());
		}
		try {
			CryptoKey cryptoKey = RSAKeyReader.deserializePublicKey(publicKeyFile);
			return new Result<CryptoKey>(Code.SUCCESS, "success", cryptoKey);
		}catch(Exception e) {
			return new Result<CryptoKey>(Code.FAIL, e.getMessage());
		}
	}

	@Override
	public Result<CryptoKey> loadPrivateKey(String destDirectory) {
		File privateKeyFile = null;
		try {
			privateKeyFile = RSAKeyReader.detectPrivateKey(new File(destDirectory));
		}catch(Exception e) {
			return new Result<CryptoKey>(Code.FAIL, e.getMessage());
		}
		try {
			CryptoKey cryptoKey = RSAKeyReader.deserializePrivateKey(privateKeyFile);
			return new Result<CryptoKey>(Code.SUCCESS, "success", cryptoKey);
		}catch(Exception e) {
			return new Result<CryptoKey>(Code.FAIL, e.getMessage());
		}
	}

	@Override
	public Result<byte[]> encrypt(byte[] data, CryptoKey key) {
		try {
			if(Type.PUBLIC != key.type()) {
				return new Result<byte[]>(Code.FAIL, "incorrect key type fro encryption: "+key.type());
			}
			if(data.length > key.maxBlockSize()) {
				return new Result<byte[]>(Code.FAIL, String.format("data must be no longer than %d bytes",  key.maxBlockSize()));
			}
			byte[] secret = RSACryptor.encrypt(data, key.encoded());
			return new Result<byte[]>(Code.SUCCESS, "success", secret);
		}catch(Exception e) {
			return new Result<byte[]>(Code.FAIL, e.getMessage());
		}
	}

	@Override
	public Result<byte[]> decrypt(byte[] secret, CryptoKey key) {
		try {
			if(Type.PRIVATE != key.type()) {
				return new Result<byte[]>(Code.FAIL, "incorrect key type for decryption: "+key.type());
			}
			if(secret.length > key.maxBlockSize()) {
				return new Result<byte[]>(Code.FAIL, String.format("secret must be no longer than %d bytes", key.maxBlockSize()));
			}
			byte[] data = RSACryptor.decrypt(secret, key.encoded());
			return new Result<byte[]>(Code.SUCCESS, "success", data);
		}catch(Exception e) {
			return new Result<byte[]>(Code.FAIL, e.getMessage());
		}
	}

	@Override
	public void close() {
		if(!isClosed) { isClosed = true; }	// do nothing
	}

}
