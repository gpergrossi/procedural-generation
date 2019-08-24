package com.gpergrossi.util.io;

/**
 * <p>A stream handler provides handles for reading and writing
 * objects of a specific class to and from a stream.</p>
 *
 * <p>A fixed size stream handler implies that all objects of
 * the given class will fit into {@link getMaxSize()} bytes
 * when written to a stream.</p>
 *
 * @param <T> - class of objects to be handled
 */
public interface IStreamHandlerFixedSize<T> extends IStreamHandler<T> {
	
	/**
	 * <p>The maximum number of bytes that objects written by this
	 * handler will need when written to a stream.</p>
	 * 
	 * <p><b><i>All objects written by this stream handler must obey
	 * this byte limit!</i></b></p>
	 * 
	 * @return maximum number of bytes when objects are written by this handler
	 */
	public int getMaxSize();
}