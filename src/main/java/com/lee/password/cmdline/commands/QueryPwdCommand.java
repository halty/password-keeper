package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.format;
import static com.lee.password.cmdline.Environment.indent;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.Password.Secret;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.util.Triple;

public class QueryPwdCommand implements Command {

	private final Long websiteId;
	private final String username;
	
	public QueryPwdCommand(Long websiteId, String username) {
		this.websiteId = websiteId;
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
				CryptoKey decryptionKey = decryptionKeyResult.third;
				Header pwdHeader = new Password.Header(websiteId, username);
				
				Result<Password> queryResult = storeDriver.selectPassword(pwdHeader, decryptionKey);
				if(!queryResult.isSuccess()) {
					line("failed to query password: "+queryResult.msg);
				}else {
					Password pwd = queryResult.result;
					Header header = pwd.header();
					Secret secret = pwd.secret();
					line("query password successful:");
					indent("websiteId -- " + header.websiteId());
					indent("username -- " + header.username());
					indent("password -- " + secret.password());
					indent("memo -- " + secret.keyValuePairs());
					indent("lastChangedTime -- " + format(header.timestamp()));
				}
			}
		}
		prompt();
	}

}
