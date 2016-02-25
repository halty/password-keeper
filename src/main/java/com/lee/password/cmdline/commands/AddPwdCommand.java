package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class AddPwdCommand implements Command {

	private final Long websiteId;
	private final String username;
	private final String password;
	private final String memo;
	
	public AddPwdCommand(Long websiteId, String username, String password, String memo) {
		this.websiteId = websiteId;
		this.username = username;
		this.password = password;
		this.memo = memo;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> storeDriverResult = current().getStoreDriver();
		if(!storeDriverResult.first) {
			line(storeDriverResult.second);
		}else {
			Triple<Boolean, String, CryptoKey> encryptionKeyResult = current().getEncryptionKey();
			if(!encryptionKeyResult.first) {
				line(encryptionKeyResult.second);
			}else {
				StoreDriver storeDriver = storeDriverResult.third;
				CryptoKey encryptionKey = encryptionKeyResult.third;
				Password pwd = new Password(websiteId, username);
				pwd.password(password).keyValuePairs(memo);
				
				Result<Header> addResult = storeDriver.insertPassword(pwd, encryptionKey);
				if(!addResult.isSuccess()) {
					line("failed to add password: "+addResult.msg);
				}else {
					line("add password successful");
				}
			}
		}
		prompt();
	}

}
