package com.lee.password.cmdline.commands;

import com.lee.password.cmdline.Cmd;
import com.lee.password.cmdline.Command;
import static com.lee.password.cmdline.Environment.*;

public class HelpCommand implements Command {

	private final Cmd cmd;
	private final boolean areAll;
	private final boolean isOnlySynopsis;
	
	public HelpCommand(boolean isOnlySynopsis) {
		this.areAll = true;
		this.isOnlySynopsis = isOnlySynopsis;
		this.cmd = null;
	}
	
	public HelpCommand(Cmd cmd) {
		this.areAll = false;
		this.isOnlySynopsis = false;
		this.cmd = cmd;
	}
	
	@Override
	public void execute() {
		if(areAll) {
			Cmd[] cmds = Cmd.values();
			int index = 0;
			if(isOnlySynopsis) {
				line("All supported commands:");
				newLine();
				for(Cmd cmd : cmds) { cmd.printSynopsis(); }
			}else {
				line("All supported command details:");
				newLine();
				for(Cmd cmd : cmds) {
					cmd.printDoc();
					if(++index < cmds.length) { newLine(); }
				}
			}
		}else {
			cmd.printDoc();
		}
		prompt();
	}

}
