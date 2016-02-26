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

public class ChangePwdCommand extends BasePwdCommand {

	private final String username;
	private final String password;
	private final String memo;
	
	public ChangePwdCommand(String websiteKeyword, Long websiteId, String username, String password, String memo) {
		super(websiteKeyword, websiteId);
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
				Triple<Boolean, String, Long> triple = takeWebsiteId(storeDriver);
				if(!triple.first) {
					line("while change password, "+triple.second);
				}else {
					long websiteId = triple.third;
					CryptoKey encryptionKey = encryptionKeyResult.third;
					StringBuilder changedFieldsBuf = new StringBuilder();
					Password pwd = new Password(websiteId, username);
					if(password != null && !password.isEmpty()) {
						pwd.password(password);
						changedFieldsBuf.append("password");
					}
					if(memo != null) {
						pwd.keyValuePairs(memo);
						if(changedFieldsBuf.length() > 0) { changedFieldsBuf.append(" and "); }
						changedFieldsBuf.append("memo");
					}
					
					Result<Header> updatedResult = storeDriver.updatePassword(pwd, encryptionKey);
					
					if(!updatedResult.isSuccess()) {
						line("failed to change "+changedFieldsBuf+": "+updatedResult.msg);
					}else {
						line("change "+changedFieldsBuf+" successful");
					}
				}
			}
		}
		prompt();
	}

}
