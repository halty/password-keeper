package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.printStackTrace;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class ExitCommand implements Command {

	@Override
	public void execute() {
		if(current().canExitNow()) {
			current().exitNow();
			line("exiting system...");
		}else {
			Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
			if(!result.first) {
				line(result.second);
				prompt();
			}else {
				StoreDriver storeDriver = result.third;
				Result<Throwable> closeResult = storeDriver.close();
				if(!closeResult.isSuccess()) {
					line("failed to release system resources: "+closeResult.msg);
					Throwable t = closeResult.result;
					if(t != null) { printStackTrace(t); }
					prompt();
				}else {
					line("success to release system resources, exiting system...");
					current().signalExit();
				}
			}
		}
	}

}
