package com.lee.password.keeper.api;

public class Result<T> {

	public static enum Code {
		SUCCESS, FAIL, RETRY;
	}
	
	public final Code code;
	public final String msg;
	public final T result;
	
	public Result(Code code, String msg) {
		this(code, msg, null);
	}
	
	public Result(Code code, String msg, T result) {
		this.code = code;
		this.msg = msg;
		this.result = result;
	}
	
	public boolean isSuccess() { return code == Code.SUCCESS; }
}
