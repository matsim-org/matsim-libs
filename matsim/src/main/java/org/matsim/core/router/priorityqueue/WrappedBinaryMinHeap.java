/* *********************************************************************** *
 * project: org.matsim.*
 * WrappedBinaryMinHeap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router.priorityqueue;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A wrapper class that allows to use a BinaryMinHeap for objects that do
 * not implement the HasIndex interface.
 * 
 * @author cdobler
 *
 * @param <E>
 */
public class WrappedBinaryMinHeap<E> implements MinHeap<E> {

	private final BinaryMinHeap<WrappedEntry> delegate;
	private final Map<E, WrappedEntry> map;
	
	public WrappedBinaryMinHeap(int maxSize) {
		this.delegate = new BinaryMinHeap<>(maxSize);
		this.map = new IdentityHashMap<>(maxSize);
	}
	
	public WrappedBinaryMinHeap(int maxSize, int fanout, boolean classicalRemove) {
		this.delegate = new BinaryMinHeap<>(maxSize, fanout, classicalRemove);
		this.map = new IdentityHashMap<>(maxSize);
	}
	
	@Override
	public boolean add(E value, double priority) {
		return this.delegate.add(this.getOrCreateWrappedEntry(value), priority);
	}

	@Override
	public boolean remove(E value) {
		if (value == null) return false;
		else return this.delegate.remove(this.getWrappedEntry(value));
	}

	@Override
	public E poll() {
		WrappedEntry entry = this.delegate.poll();
		if (entry != null) return entry.getValue();
		else return null;
	}
	
	@Override
	public boolean decreaseKey(E value, double priority) {
		return this.delegate.decreaseKey(this.getWrappedEntry(value), priority);
	}
	
	@Override
	public void reset() {
		// I don't see why we need this? Might make sense if after a reset are added new elements -
		// but in that case create a new heap.
		// cdobler, sep'17
//		this.map.clear();
		
		this.delegate.reset();
	}
	
	@Override
	public Iterator<E> iterator() {
		return new ArrayIterator(this.delegate.iterator());
	}
	
	private WrappedEntry getWrappedEntry(E value) {
		if (value == null) {
			throw new NullPointerException("null values are not supported!");
		} else return this.map.get(value);
	}
	
	private WrappedEntry getOrCreateWrappedEntry(E value) {	
		
		WrappedEntry wrappedEntry = this.getWrappedEntry(value);
		if (wrappedEntry == null) {
			int index = map.size();
			
//			if (index > maxSize) throw new RuntimeException("Number of elements exceeds the number of specified elements.");
			
			wrappedEntry = new WrappedEntry(value, index);
			map.put(value, wrappedEntry);
		}
		return wrappedEntry;
	}

	@Override
	public E peek() {
		WrappedEntry entry = this.delegate.peek();
		if (entry != null) return entry.getValue();
		else return null;
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}
	
	private final class ArrayIterator implements Iterator<E> {
		
		private final Iterator<WrappedEntry> delegate;
		
		public ArrayIterator(Iterator<WrappedEntry> delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public E next() {
			return this.delegate.next().getValue();
		}

		@Override
		public void remove() {
			this.delegate.remove();
		}
	}
	
	private final class WrappedEntry implements HasIndex {
	
		private final E value;
		private final int index;
		
		public WrappedEntry(E value, int index) {
			this.value = value;
			this.index = index;
		}
		
		public E getValue() {
			return this.value;
		}
		
		@Override
		public int getArrayIndex() {
			return this.index;
		}	
	}
}