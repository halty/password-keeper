package com.lee.password.cmdline;

import java.io.PrintStream;

public interface Command {

	/** execute command and flush the result to standard output stream **/
	public void execute();
}
