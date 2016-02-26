package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.util.Triple;

public class RemovePwdCommand extends BasePwdCommand {

	private final String username;
	
	public RemovePwdCommand(String websiteKeyword, Long websiteId, String username) {
		super(websiteKeyword, websiteId);
		this.username = username;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> storeDriverResult = current().getStoreDriver();
		if(!storeDriverResult.first) {
			line(storeDriverResult.second);
		}else {
			StoreDriver storeDriver = storeDriverResult.third;
			Triple<Boolean, String, Long> triple = takeWebsiteId(storeDriver);
			if(!triple.first) {
				line("while remove password, "+triple.second);
			}else {
				long websiteId = triple.third;
				Header pwdHeader = new Password.Header(websiteId, username);
				Result<Header> deletedResult = storeDriver.deletePassword(pwdHeader);
				if(!deletedResult.isSuccess()) {
					line("failed to remove password: "+deletedResult.msg);
				}else {
					line("remove password successful");
				}
			}
		}
		prompt();
	}

}
