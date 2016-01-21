package com.lee.password.keeper.impl.store.binary;

public class ChangedOperation<T> {

	public static enum OP {
		DELETE(1),  INSERT(2), UPDATE(3);
		public final int order;
		private OP(int order) { this.order = order; }
	}
	
	private final OP op;
	private final T target;
	
	public ChangedOperation(OP op, T target) {
		this.op = op;
		this.target = target;
	}

	public OP op() { return op; }

	public T target() { return target; }
}
