package com.lee.password.keeper.impl.crypto.rsa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RSAKeyGenerator implements RSAConstants {
	
	public static KeyPair generateKeyPair(int keySize) {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
			generator.initialize(keySize);
			return generator.generateKeyPair();
		}catch(Exception e) {
			throw new RuntimeException(
					String.format("failed to generate %s key pair with size %d", ALGORITHM, keySize),
					e);
		}
	}
}
