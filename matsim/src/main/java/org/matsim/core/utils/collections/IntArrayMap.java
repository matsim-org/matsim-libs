/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Memory-optimized map, backed by two simple arrays, for storing a <b>small</b> number of entries with integer keys.
 * Many operations (like {@link #get(Object)}) have a runtime of <code>O(n)</code>, so this map implementation
 * should only be used to store a small number of elements in it. But for small number of elements,
 * this implementation performs very well, especially because of its very low memory overhead.
 *
 * @author mrieser / Simunto GmbH
 */
public class IntArrayMap<V> implements Map<Integer, V> {

	private int[] keys;
	private Object[] values;
	private int length = 0;

	public IntArrayMap() {
		this(8);
	}

	public IntArrayMap(int capacity) {
		this.keys = new int[capacity];
		this.values = new Object[capacity];
	}

	@Override
	public int size() {
		return this.length;
	}

	@Override
	public boolean isEmpty() {
		return this.length == 0;
	}

	/**
	 * @deprecated use {@link #containsKey(int)}
	 */
	@Deprecated
	@Override
	public boolean containsKey(Object key) {
		if (key instanceof Integer) {
			return containsKey(((Integer) key).intValue());
		}
		return false;
	}

	public boolean containsKey(int key) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i = 0, n = this.length; i < n; i++) {
			Object v = this.values[i];
			if (Objects.equals(v, value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @deprecated use {@link #get(int)}
	 */
	@Deprecated
	@Override
	public V get(Object key) {
		if (key instanceof Integer) {
			return this.get(((Integer) key).intValue());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public V get(int key) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				return (V) this.values[i];
			}
		}
		return null;
	}

	/**
	 * @deprecated use {@link #put(int, V)}
	 */
	@Deprecated
	@Override
	public V put(Integer key, V value) {
		return this.put(key.intValue(), value);
	}

	@SuppressWarnings("unchecked")
	public V put(int key, V value) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				V oldValue = (V) this.values[i];
				this.values[i] = value;
				return oldValue;
			}
		}
		ensureCapacity(this.length + 1);

		this.keys[this.length] = key;
		this.values[this.length] = value;
		this.length++;
		return null;
	}

	private void ensureCapacity(int capacity) {
		if (this.keys.length < capacity) {
			int newLength = (this.keys.length + 1) * 2;
			while (newLength < capacity) {
				newLength = (newLength + 1) * 2;
			}
			this.keys = Arrays.copyOf(this.keys, newLength);
			this.values = Arrays.copyOf(this.values, newLength);
		}
	}

	/**
	 * @deprecated use {@link #replace(int, Object)}
	 */
	@Deprecated
	@Override
	public V replace(Integer key, V value) {
		return replace(key.intValue(), value);
	}

	@SuppressWarnings("unchecked")
	public V replace(int key, V value) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				V oldValue = (V) this.values[i];
				this.values[i] = value;
				return oldValue;
			}
		}
		return null;
	}

	/**
	 * @deprecated use {@link #remove(int)}
	 */
	@Deprecated
	@Override
	public V remove(final Object key) {
		if (key instanceof Integer) {
			return remove(((Integer) key).intValue());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public V remove(int key) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				V oldValue = (V) this.values[i];
				removeIndex(i);
				return oldValue;
			}
		}
		return null;
	}

	/**
	 * @deprecated use {@link #remove(int, Object)}
	 */
	@Deprecated
	@Override
	public boolean remove(Object key, Object value) {
		if (key instanceof Integer) {
			return remove(((Integer) key).intValue(), value);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean remove(int key, Object value) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				V v = (V) this.values[i];
				if (Objects.equals(v, value)) {
					removeIndex(i);
					return true;
				}
			}
		}
		return false;
	}

	public boolean removeKey(int key) {
		for (int i = 0, n = this.length; i < n; i++) {
			if (this.keys[i] == key) {
				removeIndex(i);
				return true;
			}
		}
		return false;
	}

	public boolean removeValue(final Object value) {
		for (int i = 0, n = this.length; i < n; i++) {
			Object v = this.values[i];
			if (Objects.equals(v, value)) {
				removeIndex(i);
				return true;
			}
		}
		return false;
	}

	private void removeIndex(int i) {
		int lastIndex = this.length - 1;
		this.keys[i] = this.keys[lastIndex];
		this.values[i] = this.values[lastIndex];
		this.keys[lastIndex] = 0;
		this.values[lastIndex] = null;
		this.length--;
	}

	@Override
	public void putAll(final Map<? extends Integer, ? extends V> m) {
		m.forEach(this::put);
	}

	@Override
	public void clear() {
		Arrays.fill(this.keys, 0);
		Arrays.fill(this.values, null);
		this.length = 0;
	}

	@Override
	public Set<Integer> keySet() {
		return new KeySetView<>(this);
	}

	@Override
	public Collection<V> values() {
		return new ValuesView<>(this);
	}

	@Override
	public Set<Map.Entry<Integer, V>> entrySet() {
		return new EntrySetView<>(this);
	}

	private static class Entry<V> implements Map.Entry<Integer, V> {

		private final int k;
		private final V v;

		public Entry(int k, V v) {
			this.k = k;
			this.v = v;
		}

		@Override
		public Integer getKey() {
			return this.k;
		}

		@Override
		public V getValue() {
			return this.v;
		}

		@Override
		public V setValue(final V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Entry<?> entry = (Entry<?>) o;
			return Objects.equals(this.k, entry.k) &&
					Objects.equals(this.v, entry.v);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.k, this.v);
		}
	}

	private static class KeySetView<V> implements Set<Integer> {

		private final IntArrayMap<V> map;

		KeySetView(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return this.map.size();
		}

		@Override
		public boolean isEmpty() {
			return this.map.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Integer) {
				return this.map.containsKey(((Integer) o).intValue());
			}
			return false;
		}

		@Override
		public Iterator<Integer> iterator() {
			return new KeyIterator<>(this.map);
		}

		@Override
		public Integer[] toArray() {
			Integer[] array = new Integer[this.map.length];
			for (int i = 0; i < this.map.length; i++) {
				array[i] = this.map.keys[i];
			}
			return array;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			int resultLength = this.map.length;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			System.arraycopy(this.map.values, 0, result, 0, result.length);
			return (T[]) result;
		}

		@Override
		public boolean add(Integer k) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Integer) {
				return this.map.removeKey(((Integer)o).intValue());
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (o instanceof Integer) {
					if (!this.map.containsKey(((Integer) o).intValue())) {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends Integer> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean modified = false;
			for (int i = 0, n = this.map.length; i < n; i++) {
				int key = this.map.keys[i];
				if (!c.contains(key)) {
					this.map.remove(key);
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Object o : c) {
				if (o instanceof Integer) {
					if (this.map.removeKey(((Integer) o).intValue())) {
						modified = true;
					}
				}
			}
			return modified;
		}

		@Override
		public void clear() {
			this.map.clear();
		}
	}

	private static class KeyIterator<V> implements Iterator<Integer> {
		private final IntArrayMap<V> map;
		private int nextIndex;

		KeyIterator(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.length > this.nextIndex;
		}

		@Override
		public Integer next() {
			if (hasNext()) {
				int key = this.map.keys[this.nextIndex];
				this.nextIndex++;
				return key;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			this.nextIndex--;
			this.map.removeIndex(this.nextIndex);
		}
	}

	private static class ValuesView<V> implements Collection<V> {

		private final IntArrayMap<V> map;

		public ValuesView(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return this.map.size();
		}

		@Override
		public boolean isEmpty() {
			return this.map.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return this.map.containsValue(o);
		}

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator<>(this.map);
		}

		@Override
		public Object[] toArray() {
			Object[] data = this.map.values;
			Object[] result = new Object[this.map.length];
			if (result.length >= 0) {
				System.arraycopy(data, 0, result, 0, result.length);
			}
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			Object[] data = this.map.values;
			int resultLength = this.map.length;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			System.arraycopy(data, 0, result, 0, result.length);
			return (T[]) result;
		}

		@Override
		public boolean add(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return this.map.removeValue(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!this.map.containsValue(o)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends V> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Object o : c) {
				if (this.map.removeValue(o)) {
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean modified = false;
			Object[] data = this.map.values;
			for (int i = 0, n = this.map.length; i < n; i++) {
				Object value = data[i + 1];
				if (!c.contains(value)) {
					this.map.removeValue(value);
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public void clear() {
			this.map.clear();
		}
	}

	private static class ValueIterator<V> implements Iterator<V> {

		private final IntArrayMap<V> map;
		private int nextIndex;

		ValueIterator(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.length > this.nextIndex;
		}

		@SuppressWarnings("unchecked")
		@Override
		public V next() {
			if (hasNext()) {
				V value = (V) this.map.values[this.nextIndex];
				this.nextIndex++;
				return value;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			this.nextIndex--;
			this.map.removeIndex(this.nextIndex);
		}
	}

	private static class EntrySetView<V> implements Set<Map.Entry<Integer, V>> {

		private final IntArrayMap<V> map;

		EntrySetView(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return this.map.size();
		}

		@Override
		public boolean isEmpty() {
			return this.map.isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object o) {
			if (o instanceof Entry) {
				Entry<V> e = (Entry<V>) o;
				return Objects.equals(e.v, this.map.get(e.k));
			}
			return false;
		}

		@Override
		public Iterator<Map.Entry<Integer, V>> iterator() {
			return new EntryIterator<>(this.map);
		}

		@Override
		public Object[] toArray() {
			Object[] result = new Object[this.map.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = new Entry<>(this.map.keys[i], this.map.values[i]);
			}
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			int resultLength = this.map.length;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			for (int i = 0; i < result.length; i++) {
				result[i] = new Entry<>(this.map.keys[i], this.map.values[i]);
			}
			return (T[]) result;
		}

		@Override
		public boolean add(Map.Entry<Integer, V> kvEntry) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			if (o instanceof Entry) {
				Entry<V> e = (Entry<V>) o;
				return this.map.remove(e.k, e.v);
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!this.contains(o)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends Map.Entry<Integer, V>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean modified = false;
			for (int i = 0, n = this.map.length; i < n; i++) {
				int key = this.map.keys[i];
				Object value = this.map.values[i];
				if (!c.contains(new Entry<>(key, value))) {
					this.map.remove(key, value);
					modified = true;
				}
			}
			return modified;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Object o : c) {
				if (o instanceof Entry) {
					Entry<V> e = (Entry<V>) o;
					if (this.map.remove(e.k, e.v)) {
						modified = true;
					}
				}
			}
			return modified;
		}

		@Override
		public void clear() {
			this.map.clear();
		}
	}

	private static class EntryIterator<V> implements Iterator<Map.Entry<Integer, V>> {
		private final IntArrayMap<V> map;
		private int nextIndex;

		EntryIterator(IntArrayMap<V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.length > this.nextIndex;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map.Entry<Integer, V> next() {
			int key = this.map.keys[this.nextIndex];
			V value = (V) this.map.values[this.nextIndex];
			this.nextIndex++;
			return new Entry<>(key, value);
		}
	}

}
