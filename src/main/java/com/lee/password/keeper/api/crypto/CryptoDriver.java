package com.lee.password.keeper.api.crypto;

import com.lee.password.keeper.api.Result;

public interface CryptoDriver {

	/**
	 * generate public/private key pair with {@code keySize}
	 * and store them to {@code destDirectory}.
	 * the first file of return file array is public key file,
	 * the second file of return file array is private key file.
	 */
	Result<CryptoKey[]> generateKeyPair(String destDirectory, int keySize);
	
	/** load public key from <code>destDirectory</code> with default name **/
	Result<CryptoKey> loadPublicKey(String destDirectory);
	
	/** load private key from <code>destDirectory</code> with default name **/
	Result<CryptoKey> loadPrivateKey(String destDirectory);
	
	Result<byte[]> encrypt(byte[] data, CryptoKey key);
	
	Result<byte[]> decrypt(byte[] secret, CryptoKey key);
	
	/** close this driver and releases any system resources associated with the driver. **/
	void close();
}
