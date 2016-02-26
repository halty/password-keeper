package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.util.Triple;

public class QueryPwdCommand extends BasePwdCommand {

	private final String username;
	
	public QueryPwdCommand(String websiteKeyword, Long websiteId, String username) {
		super(websiteKeyword, websiteId);
		this.username = username;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> storeDriverResult = current().getStoreDriver();
		if(!storeDriverResult.first) {
			line(storeDriverResult.second);
		}else {
			Triple<Boolean, String, CryptoKey> decryptionKeyResult = current().getDecryptionKey();
			if(!decryptionKeyResult.first) {
				line(decryptionKeyResult.second);
			}else {
				StoreDriver storeDriver = storeDriverResult.third;
				Triple<Boolean, String, Long> triple = takeWebsiteId(storeDriver);
				if(!triple.first) {
					line("while query password, "+triple.second);
				}else {
					long websiteId = triple.third;
					CryptoKey decryptionKey = decryptionKeyResult.third;
					Header pwdHeader = new Password.Header(websiteId, username);
					
					Result<Password> queryResult = storeDriver.selectPassword(pwdHeader, decryptionKey);
					if(!queryResult.isSuccess()) {
						line("failed to query password: "+queryResult.msg);
					}else {
						Password pwd = queryResult.result;
						line("query password successful:");
						printPassword(pwd);
					}
				}
			}
		}
		prompt();
	}

}
