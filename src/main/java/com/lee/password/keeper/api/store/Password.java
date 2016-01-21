package com.lee.password.keeper.api.store;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class Password {
	
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	private final Header header;
	private final Entity entity;
	
	public Password(int websiteId, String username) {
		this.header = new Header(websiteId, username);
		this.entity = new Entity();
	}

	public Header getHeader() { return header; }

	public Entity getEntity() { return entity; }

	public Password password(String password) {
		entity.password = password;
		return this;
	}
	
	public Password pairOf(String key, String value) {
		if(entity.keyValuePairs == null) {
			entity.keyValuePairs = new StringBuilder().append(encode(key)).append("=").append(encode(value));
		}else {
			entity.keyValuePairs.append("&").append(encode(key)).append("=").append(encode(value));
		}
		return this;
	}
	
	private static String encode(String value) {
		try {
			return URLEncoder.encode(value, CHARSET.name());
		}catch(UnsupportedEncodingException e) {
			// don't support utf-8 charset, heh...
			return value;
		}
	}

	public static class Header {

		private final int websiteId;
		
		private String username;
		
		private long timestamp;
		
		public Header(int websiteId, String username) {
			this.websiteId = websiteId;
			this.username = username;
			this.timestamp = System.currentTimeMillis();
		}
		
		public int websiteId() { return websiteId; }
		
		public String username() { return username; }

		public long timestamp() { return timestamp; }
	}
	
	public static class Entity {
	
		private String password;
		
		private StringBuilder keyValuePairs;
		
		public String password() { return password; }
		
		public String keyValuePairs() { return keyValuePairs.toString(); }
	}
	
}
