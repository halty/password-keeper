package com.lee.password.util;

import java.io.File;

public interface Converter<T> {

	public T convert(String value);
	
	public static final Converter<String> STRING_CONVERTER = new Converter<String>() {
		@Override
		public String convert(String value) { return value; }
	};
	
	public static final Converter<Integer> INTEGER_CONVERTER = new Converter<Integer>() {
		@Override
		public Integer convert(String value) {
			try {
				return Integer.parseInt(value);
			}catch(Exception e) {
				return null;
			}
		}
	};
	
	public static final Converter<Long> LONG_CONVERTER = new Converter<Long>() {
		@Override
		public Long convert(String value) {
			try {
				return Long.parseLong(value);
			}catch(Exception e) {
				return null;
			}
		}
	};
	
	public static final Converter<File> DIR_CONVERTER = new Converter<File>() {
		@Override
		public File convert(String value) {
			try {
				File dir = new File(value);
				return dir.isDirectory() ? dir : null;
			}catch(Exception e) {
				return null;
			}
		}
	};

}
