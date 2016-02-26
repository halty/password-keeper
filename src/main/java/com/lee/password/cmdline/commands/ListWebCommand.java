package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.newLine;
import static com.lee.password.cmdline.Environment.prompt;

import java.util.List;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.util.Triple;

public class ListWebCommand extends BaseWebCommand {

	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Result<List<Website>> listResult = storeDriver.listWebsite();
			if(!listResult.isSuccess()) {
				line("failed to list website: "+listResult.msg);
			}else {
				List<Website> websiteList = listResult.result;
				int size = websiteList.size();
				if(size == 0) {
					line("there are no webistes:");
				}else {
					line("there are " + size + " webistes:");
					for(Website web : websiteList) {
						printWebsite(web);
						if(--size > 0) { newLine(); }
					}
				}
			}
		}
		prompt();
	}
}
