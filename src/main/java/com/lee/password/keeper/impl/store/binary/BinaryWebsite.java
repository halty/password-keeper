package com.lee.password.keeper.impl.store.binary;

import static com.lee.password.keeper.api.store.Password.CHARSET;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

import com.lee.password.keeper.api.store.StoreException;
import com.lee.password.keeper.api.store.Website;
import com.lee.password.keeper.impl.InternalEntity;

public class BinaryWebsite implements InternalEntity {
	
	private static final long EPOCH = epoch();
	private static final int EPOCH_TIME_BITS = 40;	// high 40 bits, support up to 2050-11-03 19:53:47 GMT
	
	private static final int MAX_KEYWORD_BYTES = 32;
	private static final int MAX_URL_BYTES = 64;
	private static final long OCCUPIED_SIZE =
			8	// websiteId
			+ 1 + MAX_KEYWORD_BYTES	// keyword (size + data)
			+ 1 + MAX_URL_BYTES		// url (size + data)
			+ 4	// count
			+ 8	// offset
			+ 8;	// timestamp
	
	/** undefined offset **/
	public static final long UNDEF_OFFSET = -1;
	
	private final long websiteId;	// consist of epoch time(high 40 bits) and hash code of keyword(low 24bits)
	private byte[] keyword;
	private byte[] url;
	private int count;		// number of password entries of this website
	private long offset;	// offset of password entries based on password data start index
	private long timestamp;
	
	/**
	 * 0 - unchanged; 1- changed
	 * url - 0 bit
	 * count - 1th bit
	 * offset - 3rd bit
	 */
	private byte changedFlag;
	private static final int KWD_CHANGED_MASK = 0x01;
	private static final int URL_CHANGED_MASK = 0x02;
	private static final int CNT_CHANGED_MASK = 0x04;
	private static final int OFT_CHANGED_MASK = 0x08;
	
	private static long epoch() {
		String epochDate = "2016-01-01 00:00:00 GMT";
		String epochFormat = "yyyy-MM-dd HH:mm:ss z";
		try {
			return new SimpleDateFormat(epochFormat)
					.parse(epochDate)
					.getTime();
		}catch(Exception e) {
			throw new InternalError(
					String.format("init epoch {%s} with format {%s} error", epochDate, epochFormat));
		}
	}
	
	/** return the bytes of this object occupied in store file **/
	public static long occupiedSize() { return OCCUPIED_SIZE; }
	
	public static BinaryWebsite load(ByteBuffer buffer) {
		long websiteId = buffer.getLong();
		int len = 0xff & buffer.get();
		byte[] keyword = new byte[len];
		buffer.get(keyword);
		buffer.position(buffer.position() + (MAX_KEYWORD_BYTES - len)); // skip remaining bytes with keyword slot
		len = 0xff & buffer.get();
		byte[] url = new byte[len];
		buffer.get(url);
		buffer.position(buffer.position() + (MAX_URL_BYTES - len)); // skip remaining bytes with url slot
		int count = buffer.getInt();
		long offset = buffer.getLong();
		long timestamp = buffer.getLong();
		BinaryWebsite website = new BinaryWebsite(websiteId, timestamp, keyword, url);
		website.count(count);
		website.offset(offset);
		return website;
	}
	
	public static BinaryWebsite cast(Website website) {
		return new BinaryWebsite(website.timestamp(), website.keyword(), website.url());
	}
	
	private BinaryWebsite(long websiteId, long timestamp, byte[] keywordBytes, byte[] urlBytes) {
		this.websiteId = websiteId;
		this.keyword = keywordBytes;
		this.url = urlBytes;
		this.count = 0;
		this.offset = UNDEF_OFFSET;
		this.timestamp = timestamp;
		this.changedFlag = 0;
	}
	
	private BinaryWebsite(long timestamp, String keyword, String url) {
		this(createWebsiteId(timestamp, hash(keyword)),
			 timestamp,
			 toBytes(keyword, MAX_KEYWORD_BYTES, "keyword"),
			 toBytes(url, MAX_URL_BYTES, "url"));
	}
	
	private static long createWebsiteId(long timestamp, int hash) {
		long websiteId = timestamp - EPOCH;
		websiteId <<= EPOCH_TIME_BITS;
		websiteId |= (((1L<<-EPOCH_TIME_BITS) - 1) & hash);
		return websiteId;
	}
	
	private static int hash(String keyword) {
		int h = 0;
		int length = keyword.length();
		for(int i=0; i<length; i++) {
			h = 31 * h + keyword.charAt(i);
		}
		return h;
	}
	
	private static byte[] toBytes(String value, int maxLength, String name) {
		if(value == null || value.isEmpty()) {
			throw new StoreException(name + "is empty");
		}
		byte[] bytes = value.getBytes(CHARSET);
		if(bytes.length > maxLength) {
			throw new StoreException(
					String.format("%s length exceed max limit (%d bytes)", name, maxLength));
		}
		return bytes;
	}
	
	public BinaryWebsite copy() {
		BinaryWebsite newWebsite = new BinaryWebsite(websiteId, timestamp, keyword, url);
		newWebsite.count = this.count;
		newWebsite.offset = this.offset;
		newWebsite.changedFlag = this.changedFlag;
		return newWebsite;
	}
	
	public Website transform() {
		Website website = new Website(timestamp, websiteId);
		website.keyword(keyword());
		website.url(url());
		return website;
	}
	
	public long websiteId() { return websiteId; }

	public long createTimestamp() { return (websiteId >>> EPOCH_TIME_BITS) + EPOCH; }
	
	public long timestamp() { return timestamp; }
	
	public void timestamp(long timestamp) { this.timestamp = timestamp; }

	public String keyword() { return new String(keyword, CHARSET); }
	
	public void keyword(String keyword) { this.keyword = toBytes(keyword, MAX_KEYWORD_BYTES, "keyword"); }
	
	public void changeKeyword(String keyword) {
		this.keyword = toBytes(keyword, MAX_KEYWORD_BYTES, "keyword");
		changedFlag |= KWD_CHANGED_MASK;
	}

	public String url() { return new String(url, CHARSET); }
	
	public void url(String url) { this.url = toBytes(url, MAX_URL_BYTES, "url"); }
	
	public void changeUrl(String url) {
		this.url = toBytes(url, MAX_URL_BYTES, "url");
		changedFlag |= URL_CHANGED_MASK;
	}
	
	public int count() { return count; }
	
	public void count(int count) { this.count = count; }
	
	public void increment() {
		this.count += 1;
		changedFlag |= CNT_CHANGED_MASK;
	}
	
	public long offset() { return offset; }
	
	public void offset(long offset) { this.offset = offset; }
	
	public void changeOffset(long offset) {
		this.offset = offset;
		changedFlag |= OFT_CHANGED_MASK;
	}
	
	public boolean isKeywordChanged() { return (changedFlag & KWD_CHANGED_MASK) != 0; }
	public void markKeywordChanged() { changedFlag |= KWD_CHANGED_MASK; }
	public boolean isUrlChanged() { return (changedFlag & URL_CHANGED_MASK) != 0; }
	public void markUrlChanged() { changedFlag |= URL_CHANGED_MASK; }
	public boolean isCountChanged() { return (changedFlag & CNT_CHANGED_MASK) != 0; }
	public void markCountChanged() { changedFlag |= CNT_CHANGED_MASK; }
	public boolean isOffsetChanged() { return (changedFlag & OFT_CHANGED_MASK) != 0; }
	public void markOffsetChanged() { changedFlag |= OFT_CHANGED_MASK; }

	@Override
	public Type type() { return Type.WEBSITE; }
}
