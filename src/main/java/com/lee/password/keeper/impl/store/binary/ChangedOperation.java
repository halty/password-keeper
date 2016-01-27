package com.lee.password.keeper.impl.store.binary;

public class ChangedOperation<T> {

	public static enum OP { INSERT, DELETE, UPDATE; }
	
	private final OP op;
	private final T after;
	private final T before;
	
	public ChangedOperation(T before, OP op, T after) {
		this.op = op;
		this.after = after;
		this.before = before;
	}

	public OP op() { return op; }

	/** the target state after apply this opertaion **/
	public T after() { return after; }
	
	/** the target state before apply this opertaion **/
	public T before() { return before; }
}
