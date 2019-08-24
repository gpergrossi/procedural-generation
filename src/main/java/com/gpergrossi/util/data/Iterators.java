package com.gpergrossi.util.data;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Iterators {

	public static <From, To> Iterator<To> cast(Iterator<From> iterator, Function<? super From, ? extends To> caster) {
		return new IteratorCaster<>(iterator, caster);
	}
	
	private static class IteratorCaster<From, To> implements Iterator<To> {
		private Iterator<From> iterator;
		private Function<? super From, ? extends To> caster;
		
		public IteratorCaster(Iterator<From> iterator, Function<? super From, ? extends To> caster) {
			this.iterator = iterator;
			this.caster = caster;
		}
		
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public To next() {
			return caster.apply(iterator.next());
		}
		
		@Override
		public void remove() {
			iterator.remove();
		}
	}
	
	public static <From, To> Iterable<To> cast(Iterable<From> iterable, Function<? super From, ? extends To> caster) {
		return new IterableCaster<>(iterable, caster);
	}
	
	private static class IterableCaster<From, To> implements Iterable<To> {
		private Iterable<From> iterable;
		private Function<? super From, ? extends To> caster;
		
		public IterableCaster(Iterable<From> iterable, Function<? super From, ? extends To> caster) {
			this.iterable = iterable;
			this.caster = caster;
		}

		@Override
		public Iterator<To> iterator() {
			return new IteratorCaster<>(iterable.iterator(), caster);
		}
	}
	
	public static <T> Iterator<T> unwrap(Iterable<? extends Iterable<T>> iterable) {
		return new IteratorIterator<>(iterable.iterator(), t -> t.iterator());
	}
	
	public static <T> Iterator<T> unwrap(Iterator<? extends Iterable<T>> iterator) {
		return new IteratorIterator<>(iterator, t -> t.iterator());
	}
	
	public static <From, To> Iterator<To> unwrap(Iterator<From> iterator, Function<? super From, ? extends Iterator<To>> extractor) {
		return new IteratorIterator<>(iterator, extractor);
	}
	
	private static class IteratorIterator<From, To> implements Iterator<To> {
		final Iterator<From> topLevelIterator;
		final Function<? super From, ? extends Iterator<To>> topLevelExtractor;
		private Iterator<To> currentIterator;
		private Iterator<To> previousIterator;
		
		public IteratorIterator(Iterator<From> iterator, Function<? super From, ? extends Iterator<To>> extractor) {
			this.topLevelIterator = iterator;
			this.topLevelExtractor = extractor;
		}
		
		@Override
		public boolean hasNext() {
			if (currentIterator != null && currentIterator.hasNext()) return true;
			while (topLevelIterator.hasNext()) {
				currentIterator = topLevelExtractor.apply(topLevelIterator.next());
				if (currentIterator.hasNext()) return true;
			}
			return false;
		}

		@Override
		public To next() {
			if (!hasNext()) throw new NoSuchElementException();
			previousIterator = currentIterator;
			return currentIterator.next();
		}
		
		@Override
		/**
		 * This function removes the previously returned item from the previously accessed iterator.
		 * Ideally, if the iterator was empty, it would be removed from the topLevelIterator. However,
		 * it is not possible to determine when the second level iterator is "empty".
		 */
		public void remove() {
			if (previousIterator == null) throw new IllegalStateException();
			previousIterator.remove();
			previousIterator = null;
		};
	}
	
	public static <T> Iterator<T> empty() {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public T next() {
				return null;
			}
		};
	}
	
	@FunctionalInterface
	public static interface RemoveCallback<T> {
		/**
		 * Operation to be performed when an item is removed.
		 * @param item
		 * @return return false to cancel the remove operation.
		 */
		public boolean onRemove(T item);
	}
	
	public static <T> Iterator<T> withRemoveCallback(Iterator<T> iterator, RemoveCallback<T> callback) {
		return new Iterator<T>() {
			T lastReturned = null;
			
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				lastReturned = iterator.next(); 
				return lastReturned;
			}
			
			@Override
			public void remove() {
				if (lastReturned == null) {
					throw new IllegalStateException();
				}
				
				if (callback.onRemove(lastReturned)) {
					iterator.remove();
				}
				
				lastReturned = null;
			}
		};
	}
	
}
