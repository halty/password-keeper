package com.lee.password.cmdline.commands;

import com.lee.password.cmdline.Command;
import static com.lee.password.cmdline.Environment.*;

public class ListEnvCommand implements Command {

	@Override
	public void execute() {
		line("all customizable program environment variables:");
		for(Name name : Name.values()) {
			indent(name.name + " -- " + name.desc);
		}
		prompt();
	}

}
