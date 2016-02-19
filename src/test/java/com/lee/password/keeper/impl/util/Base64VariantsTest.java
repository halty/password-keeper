package com.lee.password.keeper.impl.util;

import java.nio.charset.Charset;

import com.lee.password.util.Base64Variants;

public class Base64VariantsTest {

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
