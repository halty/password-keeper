package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.format;
import static com.lee.password.cmdline.Environment.indent;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.newLine;
import static com.lee.password.cmdline.Environment.prompt;

import java.util.List;

import com.lee.password.cmdline.Cmd;
import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.Password.Header;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class ListPwdCommand implements Command {

	private final Long websiteId;
	private final String username;
	
	public ListPwdCommand(Long websiteId, String username) {
		this.websiteId = websiteId;
		this.username = username;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			if(websiteId == null && username == null) {
				line("'"+Cmd.LIST_PWD.cmd()+"' command need specify websiteId or username, please run 'help list pwd' command "
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
								line("there are 1 password");
								printPassword(header);
							}else {
								line("there are no passwords");
							}
						}
					}else {
						Result<List<Header>> pwdListResult = storeDriver.listPassword(websiteId);
						printPasswordList(pwdListResult);
					}
				}else {
					Result<List<Header>> pwdListResult = storeDriver.listPassword(username);
					printPasswordList(pwdListResult);
				}
			}
		}
		prompt();
	}
	
	private void printPasswordList(Result<List<Header>> pwdListResult) {
		if(!pwdListResult.isSuccess()) {
			line("failed to list password: "+pwdListResult.msg);
		}else {
			List<Header> pwdList = pwdListResult.result;
			int size = pwdList.size();
			if(size == 0) {
				line("there are no passwords");
			}else {
				line("there are " + size + " passwords:");
				for(Header pwd : pwdList) {
					printPassword(pwd);
					if(--size > 0) { newLine(); }
				}
			}
		}
	}

	private void printPassword(Header header) {
		indent("websiteId -- " + header.websiteId());
		indent("username -- " + header.username());
		indent("lastChangedTime -- " + format(header.timestamp()));
	}
}
