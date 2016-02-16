package com.lee.password.keeper.api;

import java.nio.charset.Charset;

/**
 * A class implements {@code Entity} inteface to indicate that
 * it is a resouce which can be manipulated by password keeper.
 */
public interface Entity {
	
	/** common charset **/
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
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
