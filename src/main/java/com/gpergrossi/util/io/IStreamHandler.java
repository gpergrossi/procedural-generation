package com.gpergrossi.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>A stream handler provides handles for reading and writing
 * objects of a specific class to and from a stream.</p>
 *
 * @param <T> - class of objects to be handled
 */
public interface IStreamHandler<T> {
	
	@FunctionalInterface
	public static interface Writer<T> {
		public void write(OutputStream os, T obj) throws IOException;
	}
	
	@FunctionalInterface
	public static interface Reader<T> {
		public T read(InputStream is) throws IOException;
	}
	
	public Writer<T> getWriter();
	public Reader<T> getReader();
	
}
