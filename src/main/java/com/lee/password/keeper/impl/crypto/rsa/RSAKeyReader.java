package com.lee.password.keeper.impl.crypto.rsa;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.lee.password.keeper.api.crypto.CryptoException;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.crypto.CryptoKey.KeyType;

public class RSAKeyReader implements RSAConstants {
	
	public static File detectPublicKey(File destDir) { return exists(destDir, PUBLIC_KEY_FILE); }
	
	public static File detectPrivateKey(File destDir) { return exists(destDir, PRIVATE_KEY_FILE); }
	
	private static File exists(File parent, String fileName) {
		File file = new File(parent, fileName);
		if(file.exists() && file.isFile()) { return file; }
		throw new CryptoException(file + "don't exist or is not a file");
	}

	public static CryptoKey deserializePublicKey(File destFile) {
		return deserializeFrom(destFile, PUBLIC_KEY_VERIFIER);
	}
	
	public static CryptoKey deserializePrivateKey(File destFile) {
		return deserializeFrom(destFile, PRIVATE_KEY_VERIFIER);
	}
	
	private static CryptoKey deserializeFrom(File destFile, KeyVerifier keyVerifier) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(destFile);
			DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
			keyVerifier.verifyKeyType(dis.readByte());
			keyVerifier.verifyAlgorithm(dis.readUTF());
			keyVerifier.verifyFormat(dis.readUTF());
			int maxBlockSize = dis.readUnsignedShort();
			int encodedLength = dis.readInt();
			byte[] encoded = new byte[encodedLength];
			int count = dis.read(encoded);
			if(count != encodedLength) {
				throw new CryptoException(String.format("incorrect key length for key file {%s}", destFile));
			}
			dis.close();
			return new CryptoKey(keyVerifier.keyType(), maxBlockSize, destFile.getAbsolutePath(), encoded);
		}catch(IOException e) {
			throw new CryptoException(String.format("failed to deserialize key from {%s}", destFile));
		}finally {
			if(fis != null) {
				try { fis.close(); }catch(Exception e) { /** can not do anything **/ }
			}
		}
	}
	
	private static final KeyVerifier PUBLIC_KEY_VERIFIER = new KeyVerifier(KeyType.PUBLIC, ALGORITHM, PUBLIC_KEY_FORMAT);
	private static final KeyVerifier PRIVATE_KEY_VERIFIER = new KeyVerifier(KeyType.PRIVATE, ALGORITHM, PRIVATE_KEY_FORMAT);
	
	private static class KeyVerifier {
		private final KeyType expectKeyType;
		private final String expectAlgorithm;
		private final String expectFormat;
		
		KeyVerifier(KeyType expectKeyType, String expectAlgorithm, String expectFormat) {
			this.expectKeyType = expectKeyType;
			this.expectAlgorithm = expectAlgorithm;
			this.expectFormat = expectFormat;
		}
		
		final void verifyKeyType(int keyType) {
			if(expectKeyType.code != keyType) {
				throw new CryptoException(String.format("expect key type {%s} but {%d}", expectKeyType, keyType));
			}
		}
		
		final void verifyAlgorithm(String algorithm) {
			if(!expectAlgorithm.equalsIgnoreCase(algorithm)) {
				throw new CryptoException(String.format("expect algorithm {%s} but {%s}", expectAlgorithm, algorithm));
			}
		}
		
		final void verifyFormat(String format) {
			if(!expectFormat.equalsIgnoreCase(format)) {
				throw new CryptoException(String.format("expect format {%s} but {%s}", expectFormat, format));
			}
		}
		
		final KeyType keyType() { return expectKeyType; }
	}
}
