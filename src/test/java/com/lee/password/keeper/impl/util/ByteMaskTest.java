package com.lee.password.keeper.impl.util;

import java.nio.charset.Charset;

import com.lee.password.util.ByteMask;

public class ByteMaskTest {

	public static void main(String[] args) {
		Charset charset = Charset.forName("UTF-8");
		byte[] bytes = {(byte) 0x85, 0x69, (byte) 0x94, 0x6B, (byte) 0x8E};
		ByteMask.unmask(bytes);
		System.out.println(new String(bytes, charset));
		
		bytes = new byte[] {(byte) 0x88, 0x76, (byte) 0x8A, 0x2D, (byte) 0x81, 0x6D, (byte) 0x90, 0x6F, (byte) 0x82, 0x27, (byte) 0x96, 0x64, (byte) 0x9E};
		ByteMask.unmask(bytes);
		System.out.println(new String(bytes, charset));
	}
}
