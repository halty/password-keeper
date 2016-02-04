package com.lee.password.keeper.impl.store;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.impl.crypto.RSACryptoDriver;

public class BinaryStoreDriverTest {

	private static int keySize;
	private static File keyDir;
	private static CryptoDriver cryptoDriver;
	private static CryptoKey publicKey;
	private static CryptoKey privateKey;
	private static File dataDir;
	private static StoreDriver storeDriver;
	
	@BeforeClass
	public static void init() {
		keySize = 1024;
		keyDir = new File("E:/tmp/password-keeper/key");
		cryptoDriver = new RSACryptoDriver();
		Result<CryptoKey[]> keyPair = cryptoDriver.generateKeyPair(keyDir.getAbsolutePath(), keySize);
		publicKey = keyPair.result[0];
		privateKey = keyPair.result[1];
		dataDir = new File("E:/tmp/password-keeper/data");
		storeDriver = new BinaryStoreDriver(dataDir.getAbsolutePath(), cryptoDriver, privateKey.maxBlockSize());
	}
	
	@Test
	public void testStorePath() {
		Result<String> result = storeDriver.storePath();
		Assert.assertTrue(result.isSuccess());
		String storePath = result.result;
		Assert.assertTrue(storePath != null && new File(storePath).exists());
	}
	
	@Test
	public void testInsertWebsite() {
		String keyword = "zhihu";
		String url = "www.zhihu.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		
		result = storeDriver.insertWebsite(website);
		Assert.assertFalse(result.isSuccess());
	}
	
	@Test
	public void testDeleteWebsite() {
		String keyword = "jd";
		String url = "www.jd.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		
		result = storeDriver.deleteWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		
		result = storeDriver.deleteWebsite(website);
		Assert.assertFalse(result.isSuccess());
	}
	
	@Test
	public void testUpdateWebsite() {
		String keyword = "dp";
		String url = "www.dianping.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String newUrl = "t.dianping.com";
		website = new Website(keyword, newUrl);
		result = storeDriver.updateWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(newUrl, result.result.url());
		
		website = new Website(websiteId);
		String newKeyword = "dianping";
		website.keyword(newKeyword);
		result = storeDriver.updateWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(newKeyword, result.result.keyword());
		
		website = new Website(websiteId);
		newUrl = "login.dianping.com";
		website.url(newUrl);
		result = storeDriver.updateWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(newUrl, result.result.url());
		
		website = new Website(websiteId);
		newKeyword = "dptuan";
		website.keyword(newKeyword);
		newUrl = "dianping.com";
		website.url(newUrl);
		result = storeDriver.updateWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(newKeyword, result.result.keyword());
		Assert.assertEquals(newUrl, result.result.url());
		
		website = new Website("none");
		newUrl = "none";
		website.url(newUrl);
		result = storeDriver.updateWebsite(website);
		Assert.assertFalse(result.isSuccess());
		
		website = new Website(0);
		newUrl = "none";
		website.url(newUrl);
		result = storeDriver.updateWebsite(website);
		Assert.assertFalse(result.isSuccess());
	}
	
	@Test
	public void testSelectWebsite() {
		String keyword = "12306";
		String url = "www.12306.cn";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		website = new Website("12306");
		result = storeDriver.selectWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(websiteId, result.result.id());
		Assert.assertEquals(keyword, result.result.keyword());
		Assert.assertEquals(url, result.result.url());
		
		website = new Website(websiteId);
		result = storeDriver.selectWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(websiteId, result.result.id());
		Assert.assertEquals(keyword, result.result.keyword());
		Assert.assertEquals(url, result.result.url());
		
		website = new Website("none");
		result = storeDriver.selectWebsite(website);
		Assert.assertFalse(result.isSuccess());
		
		website = new Website(0);
		result = storeDriver.selectWebsite(website);
		Assert.assertFalse(result.isSuccess());
	}
	
	@Test
	public void testWebsiteCount() {
		String keyword = "netease";
		String url = "www.163.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		
		Result<Integer> count = storeDriver.websiteCount();
		Assert.assertTrue(count.isSuccess());
		Assert.assertTrue(count.result >= 1);
	}
	
	@Test
	public void testListWebsite() {
		String keyword = "taobao";
		String url = "www.taobao.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		
		Result<List<Website>> resultList = storeDriver.listWebsite();
		Assert.assertTrue(resultList.isSuccess());
		Assert.assertTrue(resultList.result != null && !resultList.result.isEmpty());
	}
	
	@AfterClass
	public static void destory() {
		storeDriver.close();
		File publicKeyFile = new File(publicKey.path());
		File privateKeyFile = new File(privateKey.path());
		File dataFile = new File(storeDriver.storePath().result);
		publicKeyFile.delete();
		privateKeyFile.delete();
		dataFile.delete();
	}
}
