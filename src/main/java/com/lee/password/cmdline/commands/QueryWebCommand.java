package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.util.Triple;

public class QueryWebCommand extends BaseWebCommand {

	private final Long websiteId;
	private final String keyword;
	
	public QueryWebCommand(Long websiteId, String keyword) {
		this.websiteId = websiteId;
		this.keyword = keyword;
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
			Result<Website> queryResult = storeDriver.selectWebsite(website);
			if(!queryResult.isSuccess()) {
				line("failed to query website: "+queryResult.msg);
			}else {
				Website web = queryResult.result;
				line("query website successful:");
				printWebsite(web);
			}
		}
		prompt();
	}

}
