package com.lee.password.cmdline.commands;

import com.lee.password.cmdline.Cmd;
import com.lee.password.cmdline.Command;
import static com.lee.password.cmdline.Environment.*;

public class HelpCommand implements Command {

	private final Cmd cmd;
	private final boolean areAll;
	
	public HelpCommand() {
		this.areAll = true;
		this.cmd = null;
	}
	
	public HelpCommand(Cmd cmd) {
		this.areAll = false;
		this.cmd = null;
	}
	
	@Override
	public void execute() {
		if(areAll) {
			Cmd[] cmds = Cmd.values();
			int index = 0;
			for(Cmd cmd : cmds) {
				cmd.printDoc();
				if(++index < cmds.length) { newLine(); }
			}
		}else {
			cmd.printDoc();
		}
		prompt();
	}

}
