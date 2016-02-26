package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.format;
import static com.lee.password.cmdline.Environment.indent;
import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.Password;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.Password.Secret;
import com.lee.password.util.Triple;

public abstract class BasePwdCommand implements Command {

	private final String websiteKeyword;
	private final Long websiteId;
	
	protected BasePwdCommand(String websiteKeyword, Long websiteId) {
		this.websiteKeyword = websiteKeyword;
		this.websiteId = websiteId;
	}
	
	protected Triple<Boolean, String, Long> takeWebsiteId(StoreDriver storeDriver) {
		return takeWebsiteId(storeDriver, false);
	}
	
	protected Triple<Boolean, String, Long> takeOptionalWebsiteId(StoreDriver storeDriver) {
		return takeWebsiteId(storeDriver, true);
	}
	
	private Triple<Boolean, String, Long> takeWebsiteId(StoreDriver storeDriver, boolean isOptional) {
		if(websiteId != null) {
			if(websiteKeyword != null) {
				Result<Website> websiteResult = storeDriver.selectWebsite(new Website(websiteKeyword));
				if(!websiteResult.isSuccess()) {
					return Triple.create(false, "can not find website by websiteKeyword '"+websiteKeyword+"'", null);
				}
				long id = websiteResult.result.id();
				if(id != websiteId) {
					return Triple.create(false, "the website spcified by websiteId '"+websiteId+"' is not the same as "
							+ "website spcified by websiteKeyword '"+websiteKeyword+"'", null);
				}
			}
			return Triple.create(true, "success", websiteId);
		}else {
			if(websiteKeyword == null) {
				return Triple.create(isOptional, "both websiteId and websiteKeyword are not specified", null);
			}
			Result<Website> websiteResult = storeDriver.selectWebsite(new Website(websiteKeyword));
			if(!websiteResult.isSuccess()) {
				return Triple.create(false, "can not find website by websiteKeyword '"+websiteKeyword+"'", null);
			}
			return Triple.create(true, "success", websiteResult.result.id());
		}
	}
	
	protected void printPassword(Password password) {
		Header header = password.header();
		Secret secret = password.secret();
		indent("websiteId -- " + header.websiteId());
		indent("username -- " + header.username());
		indent("password -- " + secret.password());
		indent("memo -- " + secret.keyValuePairs());
		indent("lastChangedTime -- " + format(header.timestamp()));
	}
	
	protected void printPassword(Header header) {
		indent("websiteId -- " + header.websiteId());
		indent("username -- " + header.username());
		indent("lastChangedTime -- " + format(header.timestamp()));
	}
}
