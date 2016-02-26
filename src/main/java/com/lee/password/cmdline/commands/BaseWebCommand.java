package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.format;
import static com.lee.password.cmdline.Environment.indent;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.store.Website;

public abstract class BaseWebCommand implements Command {

	protected void printWebsite(Website website) {
		indent("websiteId -- " + website.id());
		indent("keyword -- " + website.keyword());
		indent("url -- " + website.url());
		indent("lastChangedTime -- " + format(website.timestamp()));
	}
}
