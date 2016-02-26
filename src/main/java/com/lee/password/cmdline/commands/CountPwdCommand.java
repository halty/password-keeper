package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Cmd;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class CountPwdCommand extends BasePwdCommand {

	private final String username;
	
	public CountPwdCommand(String websiteKeyword, Long websiteId, String username) {
		super(websiteKeyword, websiteId);
		this.username = username;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Triple<Boolean, String, Long> triple = takeOptionalWebsiteId(storeDriver);
			if(!triple.first) {
				line("while count password, "+triple.second);
			}else {
				Long websiteId = triple.third;
				Result<Integer> countResult = null;
				if(websiteId == null && username == null) {
					countResult = storeDriver.passwordCount();
				}else {
					if(websiteId != null) {
						if(username != null) {
							countResult = storeDriver.passwordCount(websiteId, username);
						}else {
							countResult = storeDriver.passwordCount(websiteId);
						}
					}else {
						countResult = storeDriver.passwordCount(username);
					}
				}
				if(!countResult.isSuccess()) {
					line("failed to count the number of password: "+countResult.msg);
				}else {
					line("the number of password: "+countResult.result);
					line("you can run '" + Cmd.LIST_PWD.cmd() + "' command see more details");
				}
			}
		}
		prompt();
	}

}
