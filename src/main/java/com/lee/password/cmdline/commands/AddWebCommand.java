package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.indent;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.util.Triple;

public class AddWebCommand extends BaseWebCommand {

	private final String keyword;
	private final String url;
	
	public AddWebCommand(String keyword, String url) {
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
			Result<Website> addResult = storeDriver.insertWebsite(new Website(keyword, url));
			if(!addResult.isSuccess()) {
				line("failed to add website: "+addResult.msg);
			}else {
				line("add website successful:");
				indent("the websiteId of new added website is '"+addResult.result.id() + "'");
			}
		}
		prompt();
	}

}
