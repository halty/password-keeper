package com.lee.password.keeper.impl.crypto.rsa;

public interface RSAConstants {

	public static final String ALGORITHM = "RSA";
	
	public static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	
	/** padding bytes with 'RSA/ECB/PKCS1Padding' transformation **/
	public static final int PADDING_LENGTH = 11;
	
	/* ======================= public key  ======================= */
	public static final String PUBLIC_KEY_FILE = "public.key.pk";
	
	public static final String PUBLIC_KEY_FORMAT = "X.509";
	
	/* ======================= private key ======================= */
	public static final String PRIVATE_KEY_FILE = "private.key.pk";
	
	public static final String PRIVATE_KEY_FORMAT = "PKCS#8";
}
