package com.gpergrossi.util.data.btree;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.gpergrossi.util.data.btree.BinaryNode.getFirstDescendant;
import static com.gpergrossi.util.data.btree.BinaryNode.getLastDescendant;
import static com.gpergrossi.util.data.btree.BinaryNode.getSuccessor;

public class TreeNodeIterator<T extends BinaryNode<T>> implements Iterator<T> {

	T nextElem;
	T lastElem;

	/**
	 * Constructs an iterator that will iterate over each of the Nodes in its root node's subtree in order.
	 * This iterator is not safe with concurrent modifications. Concurrent modification is not supported or detected.
	 * @param localRoot
	 */
	public TreeNodeIterator(T localRoot) {
		this.nextElem = getFirstDescendant(localRoot);
		this.lastElem = getLastDescendant(localRoot);	// The lastElem needs to be defined in case more successor nodes exist outside of localRoot's subtree
	}
	
	@Override
	public boolean hasNext() {
		return nextElem != null;
	}

	@Override
	public T next() {
		if (!hasNext()) throw new NoSuchElementException();
		T result = nextElem;

		if (nextElem == lastElem) {
			// Stop if lastElem has been reached
			nextElem = null;
		} else {
			// Proceed to next successor
			nextElem = getSuccessor(nextElem);
		}
		
		return result;
	}
	
	public static class Traversal<S extends BinaryNode<S>, T> implements Iterator<T> {
		TreeNodeIterator<S> iterator;
		Function<S, T> valueConverter;
		 
		public Traversal(S localRoot, Function<S, T> valueConverter) {
			this.iterator = new TreeNodeIterator<S>(localRoot);
			this.valueConverter = valueConverter;
		}
		
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}
		
		@Override
		public T next() {
			return valueConverter.apply(iterator.next());
		}
	}
	
	public static class Empty<T> implements Iterator<T> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			return null;
		}
	}
	
}
