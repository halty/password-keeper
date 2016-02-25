package com.lee.password.cmdline.commands;

import static com.lee.password.cmdline.Environment.current;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.prompt;

import com.lee.password.cmdline.Command;
import com.lee.password.keeper.api.Entity;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.store.StoreDriver;
import com.lee.password.util.Triple;

public class UndoCommand implements Command {

	@Override
	public void execute() {
		Triple<Boolean, String, StoreDriver> result = current().getStoreDriver();
		if(!result.first) {
			line(result.second);
		}else {
			StoreDriver storeDriver = result.third;
			Result<Entity> undoResult = storeDriver.undo();
			if(!undoResult.isSuccess()) {
				line("failed to undo the last change: "+undoResult.msg);
			}else {
				line("success to undo the last '"+undoResult.result.type()+"' change");
			}
		}
		prompt();
	}

}
