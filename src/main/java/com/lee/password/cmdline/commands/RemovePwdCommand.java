package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.util.Triple;

public class RemovePwdCommand implements Command {

	private final Long websiteId;
	private final String username;
	
	public RemovePwdCommand(Long websiteId, String username) {
		this.websiteId = websiteId;
		this.username = username;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> storeDriverResult = current().getStoreDriver();
		if(!storeDriverResult.first) {
			line(storeDriverResult.second);
		}else {
			StoreDriver storeDriver = storeDriverResult.third;
			Header pwdHeader = new Password.Header(websiteId, username);
			
			Result<Header> deletedResult = storeDriver.deletePassword(pwdHeader);
			if(!deletedResult.isSuccess()) {
				line("failed to delete password: "+deletedResult.msg);
			}else {
				line("delete password successful");
			}
		}
		prompt();
	}

}
