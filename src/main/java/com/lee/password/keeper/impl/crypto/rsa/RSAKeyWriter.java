package com.lee.password.keeper.impl.crypto.rsa;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.lee.password.keeper.api.crypto.CryptoKey.Type;

public class RSAKeyWriter implements RSAConstants {
	
	public static File initKeyDir(String destDir) {
		File dir = new File(destDir);
		if(!dir.exists()) {
			if(dir.mkdirs()) { return dir; }
			throw new IllegalArgumentException(destDir + " create failed");
		}else {
			if(dir.isDirectory()) { return dir; }
			throw new IllegalArgumentException(destDir + " is not a directory");
		}
	}
	
	/** serialize the public key to the {@code destDir} and return the file path **/
	public static String serializePublicKey(PublicKey publicKey, int maxDataBlockSize, File destDir) {
		return serializeTo(publicKey, Type.PUBLIC, maxDataBlockSize, new File(destDir, PUBLIC_KEY_FILE));
	}
	
	/** serialize the private key to the {@code destDir} and return the file path **/
	public static String serializePrivateKey(PrivateKey privateKey, int maxSecretBlockSize, File destDir) {
		return serializeTo(privateKey, Type.PRIVATE, maxSecretBlockSize, new File(destDir, PRIVATE_KEY_FILE));
	}
	
	private static String serializeTo(Key key, Type keyType, int maxBlockSize, File destFile) {
		if(destFile.exists() && destFile.length() > 0) {
			throw new IllegalArgumentException(destFile + "exists, please check no duplicate file");
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(destFile);
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
			dos.writeByte(keyType.code);
			dos.writeUTF(key.getAlgorithm());
			dos.writeUTF(key.getFormat());
			dos.writeChar(maxBlockSize);
			byte[] encoded = key.getEncoded();
			dos.writeInt(encoded.length);
			dos.write(encoded);
			dos.close();
			
			return destFile.getAbsolutePath();
		}catch(IOException e) {
			throw new RuntimeException(String.format("failed to serialize key with type {%d} to {%s}", keyType, destFile));
		}finally {
			if(fos != null) {
				try { fos.close(); }catch(Exception e) { /** can not do anything **/ }
			}
		}
	}
}
