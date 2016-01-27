package com.lee.password.keeper.api;

/**
 * A class implements {@code Entity} inteface to indicate that
 * it is a resouce which can be manipulated by password keeper.
 */
public interface Entity {
	
	/** resource type **/
	public static enum Type {
		KEY,
		WEBSITE,
		PASSWORD,
		;
	}
	
	/** return this resource type **/
	public Type type();
}
