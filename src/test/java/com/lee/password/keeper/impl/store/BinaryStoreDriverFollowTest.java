package com.lee.password.keeper.impl.store;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.impl.crypto.RSACryptoDriver;

public class BinaryStoreDriverFollowTest {
	
	private String keyword1 = "亚马逊";
	private String url1 = "www.amazon.com";
	private String username11 = "mobile";
	private String password11 = "13912345678";
	
	private String keyword2 = "天猫";
	private String url2 = "www.tmall.com";
	private String username21 = "mobile";
	private String password21 = "13412345678";
	private String username22 = "mail";
	private String password22 = "xxxx@gmail.com";

	private int keySize;
	private File keyDir;
	private CryptoDriver cryptoDriver;
	private CryptoKey publicKey;
	private CryptoKey privateKey;
	private File dataDir;
	private StoreDriver storeDriver;
	
	@Before
	public void initStore() {
		keySize = 1024;
		keyDir = new File("E:/tmp/password-keeper/follow");
		cryptoDriver = new RSACryptoDriver();
		Result<CryptoKey[]> keyPair = cryptoDriver.generateKeyPair(keyDir.getAbsolutePath(), keySize);
		publicKey = keyPair.result[0];
		privateKey = keyPair.result[1];
		dataDir = new File("E:/tmp/password-keeper/follow");
		storeDriver = new BinaryStoreDriver(dataDir.getAbsolutePath(), cryptoDriver, privateKey.maxBlockSize(), false);
		
		Website website1 = new Website(keyword1, url1);
		Result<Website> result1 = storeDriver.insertWebsite(website1);
		Assert.assertTrue(result1.isSuccess());
		Assert.assertTrue(result1.result.hasId());
		long websiteId1 = result1.result.id();
		Password entry11 = new Password(websiteId1, username11);
		entry11.password(password11);
		Result<Header> pwdResult1 = storeDriver.insertPassword(entry11, publicKey);
		Assert.assertTrue(pwdResult1.isSuccess());
		Assert.assertEquals(websiteId1, pwdResult1.result.websiteId());
		Assert.assertEquals(username11, pwdResult1.result.username());
		
		Website website2 = new Website(keyword2, url2);
		Result<Website> result2 = storeDriver.insertWebsite(website2);
		Assert.assertTrue(result2.isSuccess());
		Assert.assertTrue(result2.result.hasId());
		long websiteId2 = result2.result.id();
		Password entry21 = new Password(websiteId2, username21);
		entry21.password(password21);
		Result<Header> pwdResult21 = storeDriver.insertPassword(entry21, publicKey);
		Assert.assertTrue(pwdResult21.isSuccess());
		Assert.assertEquals(websiteId2, pwdResult21.result.websiteId());
		Assert.assertEquals(username21, pwdResult21.result.username());
		Password entry22 = new Password(websiteId2, username22);
		entry22.password(password22);
		Result<Header> pwdResult22 = storeDriver.insertPassword(entry22, publicKey);
		Assert.assertTrue(pwdResult22.isSuccess());
		Assert.assertEquals(websiteId2, pwdResult22.result.websiteId());
		Assert.assertEquals(username22, pwdResult22.result.username());
		
		Result<Throwable> result = storeDriver.close();
		Assert.assertTrue(result.isSuccess());
	}
	
	@Test
	public void testFollow() {
		keyDir = new File("E:/tmp/password-keeper/follow");
		cryptoDriver = new RSACryptoDriver();
		Result<CryptoKey> loadKeyResult = cryptoDriver.loadPublicKey(keyDir.getAbsolutePath());
		Assert.assertTrue(loadKeyResult.isSuccess());
		publicKey = loadKeyResult.result;
		loadKeyResult = cryptoDriver.loadPrivateKey(keyDir.getAbsolutePath());
		Assert.assertTrue(loadKeyResult.isSuccess());
		privateKey = loadKeyResult.result;
		dataDir = new File("E:/tmp/password-keeper/follow");
		storeDriver = new BinaryStoreDriver(dataDir.getAbsolutePath(), cryptoDriver, privateKey.maxBlockSize(), false);
		
		Result<Integer> websiteCountResult = storeDriver.websiteCount();
		Assert.assertTrue(websiteCountResult.isSuccess());
		Assert.assertEquals(Integer.valueOf(2), websiteCountResult.result);
		Result<Website> websiteResult = storeDriver.selectWebsite(new Website(keyword1));
		Assert.assertTrue(websiteResult.isSuccess());
		Assert.assertEquals(url1, websiteResult.result.url());
		long websiteId1 = websiteResult.result.id();
		
		websiteResult = storeDriver.selectWebsite(new Website(keyword2));
		Assert.assertTrue(websiteResult.isSuccess());
		Assert.assertEquals(url2, websiteResult.result.url());
		long websiteId2 = websiteResult.result.id();
		
		Result<Integer> passwordCountResult = storeDriver.passwordCount(websiteId1);
		Assert.assertTrue(passwordCountResult.isSuccess());
		Assert.assertEquals(Integer.valueOf(1), passwordCountResult.result);
		Result<Password> passwordResult = storeDriver.selectPassword(new Header(websiteId1, username11), privateKey);
		Assert.assertTrue(passwordResult.isSuccess());
		Assert.assertEquals(password11, passwordResult.result.secret().password());
		
		passwordCountResult = storeDriver.passwordCount(websiteId2);
		Assert.assertTrue(passwordCountResult.isSuccess());
		Assert.assertEquals(Integer.valueOf(2), passwordCountResult.result);
		passwordResult = storeDriver.selectPassword(new Header(websiteId2, username21), privateKey);
		Assert.assertTrue(passwordResult.isSuccess());
		Assert.assertEquals(password21, passwordResult.result.secret().password());
		passwordResult = storeDriver.selectPassword(new Header(websiteId2, username22), privateKey);
		Assert.assertTrue(passwordResult.isSuccess());
		Assert.assertEquals(password22, passwordResult.result.secret().password());
		
		Result<Throwable> result = storeDriver.close();
		Assert.assertTrue(result.isSuccess());
	}
	
	@After
	public void destroy() {
		new File(publicKey.path()).delete();
		new File(privateKey.path()).delete();
		new File(storeDriver.storePath().result).delete();
	}
}
