package com.lee.password.keeper.api.store;

/** website **/
public class Website {
	
	private final String keyword;
	
	private final String url;
	
	private final long timestamp;
	
	private int id;
	
	public Website(String keyword, String url) {
		this.keyword = keyword;
		this.url = url;
		this.timestamp = System.currentTimeMillis();
	}

	public String keyword() { return keyword; }

	public String url() { return url; }
	
	public long timestamp() { return timestamp; }
	
	public int id() { return id; }

	public void id(int id) { this.id = id; }
}
