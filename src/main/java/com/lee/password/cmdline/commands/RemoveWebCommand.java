package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.cmdline.Environment.Name;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.util.Triple;

public class RemoveWebCommand implements Command {

	private final Long websiteId;
	private final String keyword;
	
	public RemoveWebCommand(Long websiteId, String keyword) {
		this.websiteId = websiteId;
		this.keyword = keyword;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getVariable(Name.STORE_DRIVER, StoreDriver.class);
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Website website = new Website(keyword);
			if(websiteId != null) { website.id(websiteId); }
			Result<Website> removedResult = storeDriver.deleteWebsite(website);
			if(!removedResult.isSuccess()) {
				line("failed to remove website: "+removedResult.msg);
			}else {
				line("remove website successful");
			}
		}
		prompt();
	}

}
