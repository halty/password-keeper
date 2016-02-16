package com.lee.password.keeper.api.store;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import com.lee.password.keeper.api.Entity;

public class Password implements Entity {
	
	private final Header header;
	private final Secret secret;
	
	public Password(long websiteId, String username) {
		this.header = new Header(websiteId, username);
		this.secret = new Secret();
	}

	public Header header() { return header; }

	public Secret secret() { return secret; }

	public Password password(String password) {
		secret.password = password;
		return this;
	}
	
	public Password pairOf(String key, String value) {
		if(secret.keyValuePairs == null) {
			secret.keyValuePairs = new StringBuilder().append(encode(key)).append("=").append(encode(value));
		}else {
			secret.keyValuePairs.append("&").append(encode(key)).append("=").append(encode(value));
		}
		return this;
	}
	
	public Password keyValuePairs(String kvp) {
		secret.keyValuePairs = new StringBuilder(kvp.length()).append(decode(kvp));
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
	
	private static String decode(String value) {
		try {
			return URLDecoder.decode(value, CHARSET.name());
		}catch(UnsupportedEncodingException e) {
			// don't support utf-8 charset, heh...
			return value;
		}
	}

	public static class Header {
		
		private final long timestamp;

		private long websiteId;
		
		private String username;
		
		private boolean hasId;
		private boolean hasUsername;
		
		public Header(long websiteId) {
			this.websiteId = websiteId;
			this.hasId = true;
			this.timestamp = System.currentTimeMillis();
		}
		
		public Header(String username) {
			this.username = username;
			this.hasUsername = isNotEmpty(username); 
			this.timestamp = System.currentTimeMillis();
		}
		
		public Header(long websiteId, String username) {
			this.websiteId = websiteId;
			this.hasId = true;
			this.username = username;
			this.hasUsername = isNotEmpty(username); 
			this.timestamp = System.currentTimeMillis();
		}
		
		private static boolean isNotEmpty(String str) { return str != null && !str.isEmpty(); }

		public long timestamp() { return timestamp; }
		
		public long websiteId() { return websiteId; }
		
		public String username() { return username; }
		
		public boolean hasId() { return hasId; }
		public boolean hasUsername() { return hasUsername; }
	}
	
	public static class Secret {
	
		private String password;
		
		private StringBuilder keyValuePairs;
		
		public String password() { return password; }
		
		public String keyValuePairs() { return keyValuePairs == null ? null : keyValuePairs.toString(); }
	}

	@Override
	public Type type() { return Type.PASSWORD; }
	
}
