package com.lee.password.keeper.api.store;

import com.lee.password.keeper.api.Entity;

/** website **/
public class Website implements Entity {

	private final long timestamp;
	
	private String keyword;
	
	private String url;
	
	private long id;
	
	private boolean hasId;
	private boolean hasKeyword;
	
	public Website(long id) { this(System.currentTimeMillis(), id); }
	
	public Website(long timestamp, long id) {
		this.timestamp = timestamp;
		this.id = id;
		this.hasId = true;
	}
	
	public Website(String keyword) {
		this.timestamp = System.currentTimeMillis();
		this.keyword = keyword;
		this.hasKeyword = isNotEmpty(keyword);
	}
	
	public Website(String keyword, String url) {
		this.timestamp = System.currentTimeMillis();
		this.keyword = keyword;
		this.url = url;
		this.hasKeyword = isNotEmpty(keyword);
	}
	
	private static boolean isNotEmpty(String str) { return str != null && !str.isEmpty(); }
	
	public long timestamp() { return timestamp; }

	public String keyword() { return keyword; }
	
	public Website keyword(String keyword) {
		this.keyword = keyword;
		this.hasKeyword = isNotEmpty(keyword);
		return this;
	}

	public String url() { return url; }

	public Website url(String url) {
		this.url = url;
		return this;
	}
	
	public long id() { return id; }

	public Website id(long id) {
		this.id = id;
		this.hasId = true;
		return this;
	}
	
	/** if this method return {@code false}, then {@link #id()} behavior is undefined **/
	public boolean hasId() { return hasId; }
	
	public boolean hasKeyword() { return hasKeyword; }

	@Override
	public Type type() { return Type.WEBSITE; }
}
