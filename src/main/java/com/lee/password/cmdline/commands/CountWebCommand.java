package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Cmd;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class CountWebCommand extends BaseWebCommand {

	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Result<Integer> countResult = storeDriver.websiteCount();
			if(!countResult.isSuccess()) {
				line("failed to count the number of website: "+countResult.msg);
			}else {
				line("the number of webiste: "+countResult.result);
				line("you can run '" + Cmd.LIST_WEB.cmd() + "' command see more details");
			}
		}
		prompt();
	}

}
