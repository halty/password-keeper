package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.newLine;
import static com.lee.password.cmdline.Environment.prompt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lee.password.cmdline.Cmd;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class ListPwdCommand extends BasePwdCommand {

	private final String username;
	
	public ListPwdCommand(String websiteKeyword, Long websiteId, String username) {
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
				line("while list password, "+triple.second);
			}else {
				Long websiteId = triple.third;
				if(websiteId == null && username == null) {
					line("'"+Cmd.LIST_PWD.cmd()+"' command need specify the websiteId or username, please run 'help list pwd' command "
							+ "to check the use examples");
				}else {
					if(websiteId != null) {
						if(username != null) {
							Result<Header> listResult = storeDriver.listPassword(websiteId, username);
							if(!listResult.isSuccess()) {
								line("failed to list password: "+listResult.msg);
							}else {
								Header header = listResult.result;
								if(header != null) {
									Triple<Boolean, String, String> keywordTriple = mapToWebsiteKeyword(storeDriver, header.websiteId());
									String keyword = keywordTriple.first ? keywordTriple.third : keywordTriple.second;	// error info
									line("there are 1 password");
									printPassword(header, keyword);
								}else {
									line("there are no passwords");
								}
							}
						}else {
							Result<List<Header>> pwdListResult = storeDriver.listPassword(websiteId);
							printPasswordList(storeDriver, pwdListResult);
						}
					}else {
						Result<List<Header>> pwdListResult = storeDriver.listPassword(username);
						printPasswordList(storeDriver, pwdListResult);
					}
				}
			}
		}
		prompt();
	}
	
	private void printPasswordList(StoreDriver storeDriver, Result<List<Header>> pwdListResult) {
		if(!pwdListResult.isSuccess()) {
			line("failed to list password: "+pwdListResult.msg);
		}else {
			List<Header> pwdList = pwdListResult.result;
			int size = pwdList.size();
			if(size == 0) {
				line("there are no passwords");
			}else {
				line("there are " + size + " passwords:");
				Map<Long, String> idKeywordMap = new HashMap<Long, String>(size);
				for(Header pwd : pwdList) {
					String keyword = idKeywordMap.get(pwd.websiteId());
					if(keyword == null) {
						Triple<Boolean, String, String> keywordTriple = mapToWebsiteKeyword(storeDriver, pwd.websiteId());
						keyword = keywordTriple.first ? keywordTriple.third : keywordTriple.second;	// error info
						idKeywordMap.put(pwd.websiteId(), keyword);
					}
					printPassword(pwd, keyword);
					if(--size > 0) { newLine(); }
				}
			}
		}
	}
}
