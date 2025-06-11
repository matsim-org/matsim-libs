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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Memory-optimized map, backed by a simple array, for storing a <b>small</b> number of entries.
 * Many operations (like {@link #get(Object)}) have a runtime of <code>O(n)</code>, so this map implementation
 * should only be used to store a small number of elements in it. But for small number of elements,
 * this implementation performs very well, especially because of its very low memory overhead.
 *
 * Internally, this implementation uses a single array which stores boths keys and values in sequential order
 * (data[i * 2] contains the keys, data[i * 2 + 1] contains the values).
 *
 * @author mrieser / Simunto GmbH
 */
public class ArrayMap<K, V> implements Map<K, V> {

	private final static Object[] EMPTY = new Object[0];

	private Object[] data = EMPTY;

	public ArrayMap() {
	}

	public ArrayMap(Map<K, V> map) {
		this.data = new Object[map.size() * 2];
		int i = 0;
		for (Map.Entry<K, V> e : map.entrySet()) {
			this.data[i] = e.getKey();
			this.data[i + 1] = e.getValue();
			i += 2;
		}
	}

	@Override
	public int size() {
		return this.data.length / 2;
	}

	@Override
	public boolean isEmpty() {
		return this.data.length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i = 1, n = this.data.length; i < n; i += 2) {
			Object v = this.data[i];
			if (Objects.equals(v, value)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				return (V) this.data[i + 1];
			}
		}
		return null;
	}

	public V put(K key, V value) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				V oldValue = (V) this.data[i + 1];
				this.data[i + 1] = value;
				return oldValue;
			}
		}
		int oldLength = this.data.length;
		this.data = Arrays.copyOf(this.data, oldLength + 2);
		this.data[oldLength] = key;
		this.data[oldLength + 1] = value;
		return null;
	}

	@Override
	public V replace(K key, V value) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				V oldValue = (V) this.data[i + 1];
				this.data[i + 1] = value;
				return oldValue;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(final Object key) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				V oldValue = (V) this.data[i + 1];
				removeIndex(i);
				return oldValue;
			}
		}
		return null;
	}

	@Override
	public boolean remove(Object key, Object value) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				V v = (V) this.data[i + 1];
				if (Objects.equals(v, value)) {
					removeIndex(i);
					return true;
				}
			}
		}
		return false;
	}

	public boolean removeKey(final Object key) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object k = this.data[i];
			if (Objects.equals(k, key)) {
				removeIndex(i);
				return true;
			}
		}
		return false;
	}

	public boolean removeValue(final Object value) {
		for (int i = 0, n = this.data.length; i < n; i += 2) {
			Object v = this.data[i + 1];
			if (Objects.equals(v, value)) {
				removeIndex(i);
				return true;
			}
		}
		return false;
	}

	private void removeIndex(int i) {
		Object[] tmp = new Object[this.data.length - 2];
		if (i > 0) {
			System.arraycopy(this.data, 0, tmp, 0, i);
		}
		if (i + 2 < this.data.length) {
			System.arraycopy(this.data, i + 2, tmp, i, this.data.length - 2 - i);
		}
		this.data = tmp;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		m.forEach(this::put);
	}

	@Override
	public void clear() {
		this.data = EMPTY;
	}

	@Override
	public Set<K> keySet() {
		return new KeySetView<>(this);
	}

	@Override
	public Collection<V> values() {
		return new ValuesView(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new EntrySetView(this);
	}

	private static class Entry<K, V> implements Map.Entry<K, V> {

		private final K k;
		private final V v;

		public Entry(K k, V v) {
			this.k = k;
			this.v = v;
		}

		@Override
		public K getKey() {
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
			Entry<?, ?> entry = (Entry<?, ?>) o;
			return Objects.equals(this.k, entry.k) &&
					Objects.equals(this.v, entry.v);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.k, this.v);
		}
	}

	private static class KeySetView<K, V> implements Set<K> {

		private final ArrayMap<K, V> map;

		KeySetView(ArrayMap<K, V> map) {
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
			return this.map.containsKey(o);
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator<K, V>(this.map);
		}

		@Override
		public Object[] toArray() {
			Object[] data = this.map.data;
			Object[] result = new Object[data.length / 2];
			for (int i = 0; i < result.length; i++) {
				result[i] = data[i * 2];
			}
			return result;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			Object[] data = this.map.data;
			int resultLength = data.length / 2;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			for (int i = 0; i < result.length; i++) {
				result[i] = data[i * 2];
			}
			return (T[]) result;
		}

		@Override
		public boolean add(K k) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return this.map.removeKey(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!this.map.containsKey(o)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends K> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean modified = false;
			Object[] data = this.map.data;
			for (int i = 0, n = data.length; i < n; i += 2) {
				Object key = data[i];
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
				if (this.map.removeKey(o)) {
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

	private static class KeyIterator<K, V> implements Iterator<K> {
		private final ArrayMap<K, V> map;
		private int nextIndex;

		KeyIterator(ArrayMap<K, V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.data.length > this.nextIndex;
		}

		@Override
		public K next() {
			if (hasNext()) {
				K key = (K) this.map.data[this.nextIndex];
				this.nextIndex += 2;
				return key;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			this.nextIndex -= 2;
			this.map.removeIndex(this.nextIndex);
		}
	}

	private static class ValuesView<K, V> implements Collection<V> {

		private final ArrayMap<K, V> map;

		public ValuesView(ArrayMap<K, V> map) {
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
			Object[] data = this.map.data;
			Object[] result = new Object[data.length / 2];
			for (int i = 0; i < result.length; i++) {
				result[i] = data[i * 2 + 1];
			}
			return result;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			Object[] data = this.map.data;
			int resultLength = data.length / 2;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			for (int i = 0; i < result.length; i++) {
				result[i] = data[i * 2 + 1];
			}
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
			Object[] data = this.map.data;
			for (int i = 0, n = data.length; i < n; i += 2) {
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

	private static class ValueIterator<K, V> implements Iterator<V> {

		private final ArrayMap<K, V> map;
		private int nextIndex;

		ValueIterator(ArrayMap<K, V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.data.length > this.nextIndex;
		}

		@Override
		public V next() {
			if (hasNext()) {
				V value = (V) this.map.data[this.nextIndex + 1];
				this.nextIndex += 2;
				return value;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			this.nextIndex -= 2;
			this.map.removeIndex(this.nextIndex);
		}
	}

	private static class EntrySetView<K, V> implements Set<java.util.Map.Entry<K, V>> {

		private final ArrayMap<K, V> map;

		EntrySetView(ArrayMap<K, V> map) {
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
			if (o instanceof Entry) {
				Entry e = (Entry) o;
				return Objects.equals(e.v, this.map.get(e.k));
			}
			return false;
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator<>(this.map);
		}

		@Override
		public Object[] toArray() {
			Object[] data = this.map.data;
			Object[] result = new Object[data.length / 2];
			for (int i = 0; i < result.length; i++) {
				result[i] = new Entry<>(data[i * 2], data[i * 2 + 1]);
			}
			return result;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			Object[] data = this.map.data;
			int resultLength = data.length / 2;
			Object[] result = a;
			if (result == null) {
				result = new Object[resultLength];
			} else if (result.length != resultLength) {
				result = Arrays.copyOf(a, resultLength);
			}
			for (int i = 0; i < result.length; i++) {
				result[i] = new Entry<>(data[i * 2], data[i * 2 + 1]);
			}
			return (T[]) result;
		}

		@Override
		public boolean add(Map.Entry<K, V> kvEntry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Entry) {
				Entry e = (Entry) o;
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
		public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean modified = false;
			Object[] data = this.map.data;
			for (int i = 0, n = data.length; i < n; i += 2) {
				Object key = data[i];
				Object value = data[i + 1];
				if (!c.contains(new Entry<>(key, value))) {
					this.map.remove(key, value);
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Object o : c) {
				if (o instanceof Entry) {
					Entry e = (Entry) o;
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

	private static class EntryIterator<K, V> implements Iterator<java.util.Map.Entry<K, V>> {

		private final ArrayMap<K, V> map;
		private int nextIndex;

		EntryIterator(ArrayMap<K, V> map) {
			this.map = map;
		}

		@Override
		public boolean hasNext() {
			return this.map.data.length > this.nextIndex;
		}

		@Override
		public java.util.Map.Entry<K, V> next() {
			K key = (K) this.map.data[this.nextIndex];
			V value = (V) this.map.data[this.nextIndex + 1];
			this.nextIndex += 2;
			return new Entry<>(key, value);
		}

	}


}
