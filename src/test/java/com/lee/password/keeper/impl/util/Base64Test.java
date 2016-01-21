package com.lee.password.keeper.impl.util;

import java.nio.charset.Charset;

public class Base64Test {

	public static void main(String[] args) {
		Charset charset = Charset.forName("UTF-8");
		String s = "abcdef123";
		byte[] bs = Base64Variants.encode(s, charset);
		System.out.println(Base64Variants.decode(bs, charset));
		s = "我是谁";
		bs = Base64Variants.encode(s, charset);
		System.out.println(Base64Variants.decode(bs, charset));
	}
}
