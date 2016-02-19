package com.lee.password.cmdline.commands;

import java.util.List;

import com.lee.password.cmdline.Command;
import com.lee.password.cmdline.Environment;

import static com.lee.password.cmdline.Environment.*;

import com.lee.password.util.Pair;
import com.lee.password.util.Triple;

public class SetCommand implements Command {

	private final boolean isSet;
	private final Name name;
	private final String value;
	
	/** show all the program environment variables **/
	public SetCommand() {
		this.isSet = false;
		this.name = null;
		this.value = null;
	}
	
	/** show the program environment variable value named with {@code name} **/
	public SetCommand(Name name) {
		this.isSet = false;
		this.name = name;
		this.value = null;
	}
	
	/** set the program environment variable with specified {@code name} and {@code value} **/
	public SetCommand(Name name, String value) {
		this.isSet = true;
		this.name = name;
		this.value = value;
	}
	
	@Override
	public void execute() {
		Environment env = Environment.current();
		if(isSet) {
			Pair<Boolean, String> result = env.putVariable(name, value);
			if(result.first) {
				line("variable named with '" + name.name + "' was set successful");
			}else {
				line("variable named with '" + name.name + "' was set failed: "+result.second);
			}
		}else {
			if(name == null) {
				List<Pair<Name, String>> variableList = env.getAllVariables();
				if(variableList.isEmpty()) {
					line("no variables are set");
				}else {
					line("all set program environment variables:");
					for(Pair<Name, String> variable : variableList) {
						indent(variable.first.name + " = " + variable.second);
					}
				}
			}else {
				Triple<Boolean, String, String> triple = env.getVariable(name, String.class);
				if(triple.first) {
					line(name.name + " = " + triple.third);
				}else {
					line("variable named with '" + name.name + "' is not set: "+triple.second);
				}
			}
		}
		prompt();
	}

}
