package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.util.Triple;

public class ChangeWebCommand implements Command {

	private final Long websiteId;
	private final String keyword;
	private final String url;
	
	public ChangeWebCommand(Long websiteId, String keyword, String url) {
		this.websiteId = websiteId;
		this.keyword = keyword;
		this.url = url;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Website website = new Website(keyword);
			if(websiteId != null) { website.id(websiteId); }
			website.url(url);
			Result<Website> updatedResult = storeDriver.updateWebsite(website);
			if(!updatedResult.isSuccess()) {
				line("failed to update website: "+updatedResult.msg);
			}else {
				line("update website successful");
			}
		}
		prompt();
	}

}
