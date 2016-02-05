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
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.Password.Secret;
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
	
	@Test
	public void testInsertPassword() {
		String keyword = "douban";
		String url = "www.douban.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "email";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password).pairOf("securityCode", "123456");
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		entry = new Password(websiteId, username);
		entry.password(password);
		pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertFalse(pwdResult.isSuccess());
	}
	
	@Test
	public void testDeletePassword() {
		String keyword = "meituan";
		String url = "www.meituan.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "phoneNumber";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password);
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		Header header = new Header(websiteId, username);
		pwdResult = storeDriver.deletePassword(header);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		header = new Header(websiteId, username);
		pwdResult = storeDriver.deletePassword(header);
		Assert.assertFalse(pwdResult.isSuccess());
	}
	
	@Test
	public void testUpdatePassword() {
		String keyword = "dangdang";
		String url = "www.dangdang.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "phoneNumber";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password);
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		password = "234567";
		entry = new Password(websiteId, username);
		entry.password(password);
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		entry = new Password(websiteId, username);
		entry.pairOf("securityCode", "123456");
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		password = "345678";
		entry = new Password(websiteId, username);
		entry.password(password).pairOf("securityCode", "234567");
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		entry = new Password(websiteId, username);
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertFalse(pwdResult.isSuccess());
	}
	
	@Test
	public void testSelectPassword() {
		String keyword = "yihaodian";
		String url = "www.yihaodian.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "phoneNumber";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password);
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		Assert.assertEquals(websiteId, pwdResult.result.websiteId());
		Assert.assertEquals(username, pwdResult.result.username());
		
		Header header = new Header(websiteId, username);
		Result<Password> pwd = storeDriver.selectPassword(header, privateKey);
		Assert.assertTrue(pwd.isSuccess());
		Header retHeader = pwd.result.header();
		Secret retSecret = pwd.result.secret();
		Assert.assertEquals(websiteId, retHeader.websiteId());
		Assert.assertEquals(username, retHeader.username());
		Assert.assertEquals(password, retSecret.password());
		String retKvp = retSecret.keyValuePairs();
		Assert.assertTrue(retKvp == null || retKvp.isEmpty());
		
		password = "234567";
		entry = new Password(websiteId, username);
		entry.password(password);
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		header = new Header(websiteId, username);
		pwd = storeDriver.selectPassword(header, privateKey);
		Assert.assertTrue(pwd.isSuccess());
		retHeader = pwd.result.header();
		retSecret = pwd.result.secret();
		Assert.assertEquals(websiteId, retHeader.websiteId());
		Assert.assertEquals(username, retHeader.username());
		Assert.assertEquals(password, retSecret.password());
		retKvp = retSecret.keyValuePairs();
		Assert.assertTrue(retKvp == null || retKvp.isEmpty());
		
		entry = new Password(websiteId, username);
		entry.pairOf("securityCode", "123456");
		String kvp = entry.secret().keyValuePairs();
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		header = new Header(websiteId, username);
		pwd = storeDriver.selectPassword(header, privateKey);
		Assert.assertTrue(pwd.isSuccess());
		retHeader = pwd.result.header();
		retSecret = pwd.result.secret();
		Assert.assertEquals(websiteId, retHeader.websiteId());
		Assert.assertEquals(username, retHeader.username());
		Assert.assertEquals(password, retSecret.password());
		Assert.assertEquals(kvp,retSecret.keyValuePairs());
		
		password = "345678";
		entry = new Password(websiteId, username);
		entry.password(password).pairOf("securityCode", "234567");
		kvp = entry.secret().keyValuePairs();
		pwdResult = storeDriver.updatePassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		header = new Header(websiteId, username);
		pwd = storeDriver.selectPassword(header, privateKey);
		Assert.assertTrue(pwd.isSuccess());
		retHeader = pwd.result.header();
		retSecret = pwd.result.secret();
		Assert.assertEquals(websiteId, retHeader.websiteId());
		Assert.assertEquals(username, retHeader.username());
		Assert.assertEquals(password, retSecret.password());
		Assert.assertEquals(kvp,retSecret.keyValuePairs());
		
		header = new Header(websiteId, username);
		pwdResult = storeDriver.deletePassword(header);
		Assert.assertTrue(pwdResult.isSuccess());
		pwd = storeDriver.selectPassword(header, privateKey);
		Assert.assertFalse(pwd.isSuccess());
	}
	
	@Test
	public void testPasswordCount() {
		String keyword = "oracle";
		String url = "www.oracle.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "nickName";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password);
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		
		keyword = "java";
		url = "www.java.com";
		website = new Website(keyword, url);
		result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		websiteId = result.result.id();
		
		username = "phoneNumber";
		password = "123456";
		entry = new Password(websiteId, username);
		entry.password(password);
		pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		
		username = "nickName";
		password = "123456";
		entry = new Password(websiteId, username);
		entry.password(password);
		pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		
		Result<Integer> countResult = storeDriver.passwordCount();
		Assert.assertTrue(countResult.isSuccess());
		Assert.assertTrue(countResult.result >= 3);
		
		countResult = storeDriver.passwordCount(websiteId);
		Assert.assertTrue(countResult.isSuccess());
		Assert.assertTrue(countResult.result == 2);
		
		countResult = storeDriver.passwordCount(username);
		Assert.assertTrue(countResult.isSuccess());
		Assert.assertTrue(countResult.result == 2);
		
		countResult = storeDriver.passwordCount(0);
		Assert.assertTrue(countResult.isSuccess());
		Assert.assertTrue(countResult.result == 0);
		
		countResult = storeDriver.passwordCount("");
		Assert.assertTrue(countResult.isSuccess());
		Assert.assertTrue(countResult.result == 0);
	}
	
	@Test
	public void testListPassword() {
		String keyword = "microsoft";
		String url = "www.microsoft.com";
		Website website = new Website(keyword, url);
		Result<Website> result = storeDriver.insertWebsite(website);
		Assert.assertTrue(result.isSuccess());
		Assert.assertTrue(result.result.hasId());
		long websiteId = result.result.id();
		
		String username = "userId";
		String password = "123456";
		Password entry = new Password(websiteId, username);
		entry.password(password);
		Result<Header> pwdResult = storeDriver.insertPassword(entry, publicKey);
		Assert.assertTrue(pwdResult.isSuccess());
		
		Result<List<Header>> listResult = storeDriver.listPassword(websiteId);
		Assert.assertTrue(listResult.isSuccess());
		List<Header> headerList = listResult.result;
		Assert.assertTrue(headerList != null && headerList.size() == 1);
		Header header = headerList.get(0);
		Assert.assertEquals(websiteId, header.websiteId());
		Assert.assertEquals(username, header.username());
		
		listResult = storeDriver.listPassword(username);
		Assert.assertTrue(listResult.isSuccess());
		headerList = listResult.result;
		Assert.assertTrue(headerList != null && headerList.size() == 1);
		header = headerList.get(0);
		Assert.assertEquals(websiteId, header.websiteId());
		Assert.assertEquals(username, header.username());
		
		listResult = storeDriver.listPassword(0);
		Assert.assertFalse(listResult.isSuccess());
		
		listResult = storeDriver.listPassword("");
		Assert.assertFalse(listResult.isSuccess());
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
