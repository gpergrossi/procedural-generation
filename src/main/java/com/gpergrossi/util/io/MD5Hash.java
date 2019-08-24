package com.gpergrossi.util.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash {

	public static String hash(byte[] bytes) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return byteArrayToHex(md5.digest(bytes));			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String hash(String str) {
		return hash(str.getBytes());
	}
	
	private static String byteArrayToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(byteToHex(b));
		}
		return sb.toString();
	}

	private static String byteToHex(byte b) {
		int low = b & 0xF;
		int high = (b >> 4) & 0xF;
		return nibbleToHex(high) + nibbleToHex(low);
	}

	private static String nibbleToHex(int nibble) {
		if (nibble < 0 || nibble > 15) throw new IllegalArgumentException();
		return Integer.toHexString(nibble);
	}
	
}
