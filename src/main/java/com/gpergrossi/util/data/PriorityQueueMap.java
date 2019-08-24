package com.gpergrossi.util.data;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

public class PriorityQueueMap<T> extends AbstractSet<T> {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = -1025788813703001122L;
	
	protected static class PriorityEntry<T> implements Comparable<PriorityEntry<T>> {
		/** The actual item that was added to the queue **/
		protected final T item;
		
		/** Priority - higher means first off the queue **/
		protected int priority;
		
		public PriorityEntry(T item, int priority) {
			this.item = item;
			this.priority = priority;
		}
		
		@Override
		public int compareTo(PriorityEntry<T> o) {
			// If o is higher priority than this, return positive, 
			// which means that this should be sorted after o.
			return o.priority - this.priority;
		}		
	}
	
	private boolean allowPriorityUpgrades = true;
	private boolean allowPriorityDowngrades = false;
	
	protected Map<T, PriorityEntry<T>> entryMap;
	protected PriorityQueue<PriorityEntry<T>> entryPriorityQueue;
	
	public PriorityQueueMap() {
		this.entryMap = new HashMap<>();
		this.entryPriorityQueue = new PriorityQueue<>(10);
	}
	
	/**
	 * @param item - item to be added to the priority queue
	 * @param priority - priority of the item.
	 * @return true if item was added / priority was modified. false if priority change was not allowed, or item was null.
	 */
	public synchronized boolean offer(T item, int priority) {
		if (item == null) return false;
		PriorityEntry<T> entry = entryMap.get(item);
		
		if (entry != null) {
			// If an entry already exists for the item, change its priority
			return changePriority(entry, priority);
		} else {
			// Else: an entry was not found, create one
			entry = new PriorityEntry<>(item, priority);
			entryMap.put(item, entry);
			entryPriorityQueue.add(entry);
			return true;
		}
	}
	
	/**
	 * @param entry - entry for which the priority value should be changed
	 * @param newPriority - the new priority value for the provided entry
	 * @return true if the priority was changed, false if the priority upgrade/downgrade was not allowed
	 */
	protected synchronized boolean changePriority(PriorityEntry<T> entry, int newPriority) {
		if (newPriority == entry.priority) return true;
		
		if (newPriority < entry.priority) {
			if (!allowPriorityDowngrades) return false;
		} else {
			if (!allowPriorityUpgrades) return false;
		}
		
		// If allowed, change the item's priority:
		// Remove the item, change its priority, and re-add the item.
		entryPriorityQueue.remove(entry);
		entry.priority = newPriority;
		entryPriorityQueue.offer(entry);
		return true;
	}
	
	/**
	 * @param item - item to be removed from the priority queue
	 * @return true if the item was removed, else false
	 */
	@Override
	public synchronized boolean remove(Object item) {
		if (item == null) return false;
		PriorityEntry<T> previousMapping = entryMap.remove(item);
		if (previousMapping != null) {
			entryPriorityQueue.remove(item);
			return true;
		}
		return false;
	}
	
	public synchronized int size() {
		return entryPriorityQueue.size();
	}
	
	public synchronized boolean isEmpty() {
		return entryPriorityQueue.isEmpty();
	}
	
	public synchronized T poll() {
		if (entryPriorityQueue.isEmpty()) return null;
		
		PriorityEntry<T> result = entryPriorityQueue.poll();		
		entryMap.remove(result.item);
		return result.item;
	}

	@Override
	public synchronized boolean contains(Object item) {
		return entryMap.containsKey(item);
	}
	
	public synchronized Integer getPriority(T item) {
		PriorityEntry<T> entry = entryMap.get(item);
		if (entry == null) {
			return null;
		} else {
			return entry.priority;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return entryMap.keySet().iterator();
	}

}
