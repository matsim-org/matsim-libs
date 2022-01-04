/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Memory-optimized map, backed by a simple array, for storing small number of {@link Identifiable}s.
 * Access using {@link #get(Object)} have a runtime of <code>O(n)</code>, so this map implementation
 * should only be used to store a small number of elements in it. But for small number of elements,
 * this implementation performs very well, especially because of its very low memory overhead.
 * 
 * @author mrieser / senozon
 */
public class IdentifiableArrayMap<S, T extends Identifiable<S>> implements Map<Id<S>, T> {

	private final static Identifiable[] EMPTY = new Identifiable[0];

	@SuppressWarnings("unchecked")
	private T[] data = (T[]) EMPTY;
	
	@Override
	public int size() {
		return this.data.length;
	}

	@Override
	public boolean isEmpty() {
		return this.data.length == 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		for (Identifiable<S> o : this.data) {
			if (o.getId().equals(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(final Object value) {
		for (Identifiable<S> o : this.data) {
			if (o.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(final Object key) {
		for (Identifiable<S> o : this.data) {
			if (o.getId().equals(key)) {
				return (T) o;
			}
		}
		return null;
	}

	public T put(final T value) {
		return put(value.getId(), value);
	}

	@Override
	public T put(final Id<S> key, final T value) {
		for (int i = 0; i < this.data.length; i++) {
			T old = this.data[i];
			if (old.getId().equals(key)) {
				this.data[i] = value;
				return old;
			}
		}
		this.data = Arrays.copyOf(this.data, this.data.length + 1);
		this.data[this.data.length - 1] = value;
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T remove(final Object key) {
		for (int i = 0; i < this.data.length; i++) {
			T old = this.data[i];
			if (old.getId().equals(key)) {
				
				Identifiable<S>[] tmp = new Identifiable[this.data.length - 1];
				if (i > 0) {
					System.arraycopy(this.data, 0, tmp, 0, i);
				}
				if (i + 1 < this.data.length) {
					System.arraycopy(this.data, i + 1, tmp, i, this.data.length - 1 - i);
				}
				this.data = (T[]) tmp;
				
				return old;
			}
		}
		return null;
	}

	@Override
	public void putAll(final Map<? extends Id<S>, ? extends T> m) {
		for (T t : m.values()) {
			put(t.getId(), t);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clear() {
		this.data = (T[]) new Identifiable[0];
	}

	@Override
	public Set<Id<S>> keySet() {
		Set<Id<S>> ids = new LinkedHashSet<Id<S>>();
		for (Identifiable<S> o : this.data) {
			ids.add(o.getId());
		}
		return ids;
	}

	@Override
	public Collection<T> values() {
		return new ArrayCollection<T>(this.data);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<java.util.Map.Entry<Id<S>, T>> entrySet() {
		Set<Map.Entry<Id<S>, T>> entries = new LinkedHashSet<Map.Entry<Id<S>, T>>();
		for (Identifiable<S> o : this.data) {
			entries.add(new Entry<S, T>((T) o));
		}
		return entries;
	}

	private static class Entry<S, T extends Identifiable<S>> implements Map.Entry<Id<S>, T> {

		private final T t;
		
		public Entry(final T t) {
			this.t = t;
		}
		
		@Override
		public Id<S> getKey() {
			return this.t.getId();
		}

		@Override
		public T getValue() {
			return this.t;
		}

		@Override
		public T setValue(final T value) {
			throw new UnsupportedOperationException();
		}
		
	}
	
	/**
	 * Array-backed read-only collection for high-performance, preventing unnecessary array copy operations.
	 * 
	 * @author mrieser / senozon
	 *
	 * @param <A>
	 */
	private static class ArrayCollection<A> implements Collection<A> {

		private final A[] data;
		
		public ArrayCollection(final A[] data) {
			this.data = data;
		}
		
		@Override
		public int size() {
			return this.data.length;
		}

		@Override
		public boolean isEmpty() {
			return this.data.length == 0;
		}

		@Override
		public boolean contains(Object o) {
			for (A t : this.data) {
				if (t == null) {
					return o == null;
				}
				if (t.equals(o)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<A> iterator() {
			return new ArrayIterator<A>(data);
		}

		@Override
		public Object[] toArray() {
			return this.data.clone();
		}

		@Override
		public <TT> TT[] toArray(final TT[] a) {
			TT[] dest = a;
			if (a.length != this.data.length) {
				dest = Arrays.copyOf(a, this.data.length);
			}
			System.arraycopy(this.data, 0, dest, 0, this.data.length);
			return dest;
		}

		@Override
		public boolean add(A e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				boolean isPartOf = this.contains(o);
				if (!isPartOf) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends A> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();			
		}
	}
	
	private static class ArrayIterator<B> implements Iterator<B> {

		private final B[] data;
		private int pos = 0;
		
		public ArrayIterator(final B[] data) {
			this.data = data;
		}
		
		@Override
		public boolean hasNext() {
			return this.pos < data.length;
		}

		@Override
		public B next() {
			if (this.pos < data.length) {
				B t = data[this.pos];
				this.pos++;
				return t;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
}
