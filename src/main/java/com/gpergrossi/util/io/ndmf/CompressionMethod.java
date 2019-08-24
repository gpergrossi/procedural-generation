package com.gpergrossi.util.io.ndmf;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public interface CompressionMethod {
	
	public static Map<Byte, CompressionMethod> METHODS = new HashMap<>();
	public static CompressionMethod ZLIB = Zlib.INSTANCE;
	
	public static CompressionMethod fromID(byte id) {
		return METHODS.get(id);
	}

	public static boolean register(CompressionMethod method) {
		final CompressionMethod alreadyRegistered = METHODS.get(method.getCompressionID());
		if (alreadyRegistered != null) {
			if (alreadyRegistered != method) {
				throw new RuntimeException("The method ID: "+method.getCompressionID()+" is already in use by a different CompressionMethod!");
			}
			return false;
		}
		METHODS.put(method.getCompressionID(), method);
		return true;
	}
	
	/**
	 * The unique ID that identifies a compression method
	 * @return
	 */
	public byte getCompressionID();
	
	public OutputStream getCompressionStream(OutputStream os);
	public InputStream getDecompressionStream(InputStream is);
	
	
	
	public static class Zlib implements CompressionMethod {
		public static Zlib INSTANCE = new Zlib();

		private Zlib() {
			register(this);
		}
		
		@Override
		public byte getCompressionID() {
			return 1;
		}

		@Override
		public OutputStream getCompressionStream(OutputStream os) {
			return new DeflaterOutputStream(os);
		}

		@Override
		public InputStream getDecompressionStream(InputStream is) {
			return new InflaterInputStream(is);
		}
		
	}

}
