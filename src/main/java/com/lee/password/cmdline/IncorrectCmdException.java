package com.lee.password.cmdline;

public class IncorrectCmdException extends RuntimeException {

	private static final long serialVersionUID = -7487211146727164986L;

	public IncorrectCmdException(String msg) { super(msg); }
	
	public IncorrectCmdException(String msg, Exception e) { super(msg, e); }
}
