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
	private static final int KEYWORD_OFFSET = 8;	// websiteId
	private static final int URL_OFFSET =
			KEYWORD_OFFSET	
			+ 1 + MAX_KEYWORD_BYTES;	// keyword (size + data)
	private static final int TIMESTAMP_OFFSET =
			URL_OFFSET
			+ 1 + MAX_URL_BYTES;		// url (size + data)
	private static final int COUNT_OFFSET =
			TIMESTAMP_OFFSET
			+ 8;	// timestamp
	private static final int OFFSET_OFFSET =
			COUNT_OFFSET
			+ 4;	// count
	private static final int OCCUPIED_SIZE =
			OFFSET_OFFSET
			+ 8;	// offset
	
	/** undefined offset **/
	private static final long UNDEF_OFFSET = -1;
	
	private final long websiteId;	// consist of epoch time(high 40 bits) and hash code of keyword(low 24bits)
	private byte[] keyword;
	private byte[] url;
	private long timestamp;
	private int count;		// number of password entries of this website
	private long offset;	// offset of password entries based on password data start index
	
	/** 0 - unchanged; 1- changed */
	private byte changedFlag;
	private static final int KWD_CHANGED_MASK = 0x01;
	private static final int URL_CHANGED_MASK = 0x02;
	
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
	public static int occupiedSize() { return OCCUPIED_SIZE; }
	public static int keywordSize() { return MAX_KEYWORD_BYTES; }
	public static long keywordPosition(long websitePosition) { return websitePosition + KEYWORD_OFFSET; }
	public static int urlPortionSize() { return MAX_URL_BYTES + 8; }
	public static long urlPortionPosition(long websitePosition) { return websitePosition + URL_OFFSET; }
	public static int webPortionSize() { return MAX_KEYWORD_BYTES + MAX_URL_BYTES + 8; }
	public static long webPortionPosition(long websitePosition) { return websitePosition + KEYWORD_OFFSET; }
	public static int pwdPortionSize() { return OCCUPIED_SIZE - TIMESTAMP_OFFSET; }
	public static long pwdPortionPosition(long websitePosition) { return websitePosition + TIMESTAMP_OFFSET; }
	public static long offsetPosition(long websitePosition) { return websitePosition + OFFSET_OFFSET; }
	public static long timestampPosition(long websitePosition) { return websitePosition + TIMESTAMP_OFFSET; }
	
	public static BinaryWebsite read(ByteBuffer buffer) {
		long websiteId = buffer.getLong();
		int len = 0xff & buffer.get();
		byte[] keyword = new byte[len];
		buffer.get(keyword);
		buffer.position(buffer.position() + (MAX_KEYWORD_BYTES - len)); // skip remaining bytes with keyword slot
		len = 0xff & buffer.get();
		byte[] url = new byte[len];
		buffer.get(url);
		buffer.position(buffer.position() + (MAX_URL_BYTES - len)); // skip remaining bytes with url slot
		long timestamp = buffer.getLong();
		int count = buffer.getInt();
		long offset = buffer.getLong();
		BinaryWebsite website = new BinaryWebsite(websiteId, timestamp, keyword, url);
		website.count(count);
		website.offset(offset);
		return website;
	}
	
	public static void write(ByteBuffer buffer, BinaryWebsite target) {
		buffer.putLong(target.websiteId);
		int len = target.keyword.length;
		buffer.put((byte)len);
		buffer.put(target.keyword);
		buffer.position(buffer.position() + (MAX_KEYWORD_BYTES - len)); // skip remaining bytes with keyword slot
		len = target.url.length;
		buffer.put((byte)len);
		buffer.put(target.url);
		buffer.position(buffer.position() + (MAX_URL_BYTES - len)); // skip remaining bytes with url slot
		buffer.putLong(target.timestamp);
		buffer.putInt(target.count);
		buffer.putLong(target.offset);
	}
	
	public static void writeKeyword(ByteBuffer buffer, BinaryWebsite target) {
		int len = target.keyword.length;
		buffer.put((byte)len);
		buffer.put(target.keyword);
		buffer.position(buffer.position() + (MAX_KEYWORD_BYTES - len)); // skip remaining bytes with keyword slot
	}
	
	public static void writeUrlPortion(ByteBuffer buffer, BinaryWebsite target) {
		int len = target.url.length;
		buffer.put((byte)len);
		buffer.put(target.url);
		buffer.position(buffer.position() + (MAX_URL_BYTES - len)); // skip remaining bytes with url slot
		buffer.putLong(target.timestamp);
	}
	
	public static void writeWebPortion(ByteBuffer buffer, BinaryWebsite target) {
		int len = target.keyword.length;
		buffer.put((byte)len);
		buffer.put(target.keyword);
		buffer.position(buffer.position() + (MAX_KEYWORD_BYTES - len)); // skip remaining bytes with keyword slot
		len = target.url.length;
		buffer.put((byte)len);
		buffer.put(target.url);
		buffer.position(buffer.position() + (MAX_URL_BYTES - len)); // skip remaining bytes with url slot
		buffer.putLong(target.timestamp);
	}
	
	public static void writePwdPortion(ByteBuffer buffer, BinaryWebsite target) {
		buffer.putLong(target.timestamp);
		buffer.putInt(target.count);
		buffer.putLong(target.offset);
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
	
	public void pasteKeyword(BinaryWebsite source) { this.keyword = source.keyword; }

	public String url() { return new String(url, CHARSET); }
	
	public void url(String url) { this.url = toBytes(url, MAX_URL_BYTES, "url"); }
	
	public void changeUrl(String url) {
		this.url = toBytes(url, MAX_URL_BYTES, "url");
		changedFlag |= URL_CHANGED_MASK;
	}
	
	public void pasteUrl(BinaryWebsite source) { this.url = source.url; }
	
	public int count() { return count; }
	
	public void count(int count) { this.count = count; }
	
	/** decrease if {@code delta} is negative **/
	public void incrementCount(int delta) { this.count += delta; }
	
	public boolean isValidOffset() { return offset > UNDEF_OFFSET; }
	
	public long offset() { return offset; }
	
	public void offset(long offset) { this.offset = offset; }
	
	/** decrease if {@code delta} is negative **/
	public void incrementOffset(long delta) { this.offset += delta; }
	
	public boolean isKeywordChanged() { return (changedFlag & KWD_CHANGED_MASK) != 0; }
	public void markKeywordChanged() { changedFlag |= KWD_CHANGED_MASK; }
	public boolean isUrlChanged() { return (changedFlag & URL_CHANGED_MASK) != 0; }
	public void markUrlChanged() { changedFlag |= URL_CHANGED_MASK; }

	@Override
	public Type type() { return Type.WEBSITE; }
}
