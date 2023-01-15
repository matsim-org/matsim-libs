package org.matsim.api.core.v01;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdMap<T, V> implements Map<Id<T>, V>, Iterable<V> {

	private static final int INCREMENT = 100;
	private static final float INCREMENT_FACTOR = 1.5f;
	private final Class<T> idClass;
	private int size = 0;
	private Object[] data;

	public IdMap(Class<T> idClass) {
		this(idClass, Math.max(Id.getNumberOfIds(idClass), INCREMENT));
	}

	public IdMap(Class<T> idClass, int size) {
		this.idClass = idClass;
		this.data = new Object[size];
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof Id) {
			return containsKey((Id<T>) key);
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		Objects.requireNonNull(value);
		for (Object v : this.data) {
			if (v != null && value.equals(v)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		if (key instanceof Id) {
			return get((Id<T>) key);
		}
		return null;
	}

	@Override
	public V remove(Object key) {
		if (key instanceof Id) {
			return remove((Id<T>) key);
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends Id<T>, ? extends V> m) {
		int maxIndex = 0;
		for (Id<T> k : m.keySet()) {
			int index = k.index();
			if (index > maxIndex) {
				maxIndex = index;
			}
		}
		ensureCapacity(maxIndex);

		m.forEach((k, v) -> {
			int index = k.index();
			Object oldValue = this.data[index];
			this.data[k.index()] = v;
			if (v != null && oldValue == null) {
				this.size++;
			}
			if (v == null && oldValue != null) {
				this.size--;
			}
		});
	}

	public void putAll(IdMap<T, ? extends V> m) {
		this.ensureCapacity(m.data.length - 1);
		for (int i = 0; i < m.data.length; i++) {
			Object value = m.data[i];
			if (value != null) {
				Object oldValue = this.data[i];
				this.data[i] = value;
				if (oldValue == null) {
					this.size++;
				}
			}
		}
	}

	public boolean containsKey(Id<T> key) {
		int idx = key.index();
		return idx < this.data.length && this.data[idx] != null;
	}

	public boolean containsKey(int index) {
		return index < this.data.length && this.data[index] != null;
	}

	public V get(Id<T> key) {
		int idx = key.index();
		if (idx < this.data.length) {
			return (V) this.data[idx];
		}
		return null;
	}

	public V get(int index) {
		if (index < this.data.length) {
			return (V) this.data[index];
		}
		return null;
	}

	@Override
	public V put(Id<T> key, V value) {
		return this.put(key.index(), value);
	}

	V put(int index, V value) {
		ensureCapacity(index);
		Object oldValue = this.data[index];
		this.data[index] = value;
		if (value != null && oldValue == null) {
			this.size++;
		}
		if (value == null && oldValue != null) {
			this.size--;
		}
		return (V) oldValue;
	}

	private void ensureCapacity(int index) {
		if (index >= this.data.length) {
			int newSize = Math.max(index + INCREMENT, (int)(data.length * INCREMENT_FACTOR));
			Object[] tmp = new Object[newSize];
			System.arraycopy(this.data, 0, tmp, 0, this.data.length);
			this.data = tmp;
		}
	}
	public V remove(Id<T> key) {
		return this.remove(key.index());
	}

	V remove(int idx) {
		if (idx < this.data.length) {
			Object oldValue = this.data[idx];
			this.data[idx] = null;
			if (oldValue != null) {
				this.size--;
			}
			return (V) oldValue;
		}
		return null;
	}

	@Override
	public void clear() {
		this.size = 0;
		Arrays.fill(this.data, null);
	}

	@Override
	public Set<Id<T>> keySet() {
		return new KeySet<>(this);
	}

	@Override
	public void forEach(BiConsumer<? super Id<T>, ? super V> action) {
		for (int i = 0; i < this.data.length; i++) {
			Object o = this.data[i];
			if (o != null) {
				action.accept(Id.get(i, this.idClass), (V) o);
			}
		}
	}

	@Override
	public Collection<V> values() {
		return new DataCollection<T, V>(this);
	}

	@Override
	public Set<Map.Entry<Id<T>, V>> entrySet() {
		return new EntrySet<T, V>(this);
	}

	@Override
	public Iterator<V> iterator() {
		return new DataIterator<>(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Map))
			return false;
		if (o instanceof IdMap) {
			IdMap<?, ?> m = (IdMap<?, ?>) o;
			if (this.size != m.size)
				return false;
			for (int i = 0; i < this.data.length && i < m.data.length; i++) { // one of the data arrays may have more capacity than the other despite having the same number of non-null entries. This is okay if and only if the additional entries are null. This gets checked implicitly by the loop because we already know they have the same number of nun-null elements.
				if (this.data[i] != m.data[i])
					return false;
			}
			return true;
		} else {
			Map<Id<?>, ?> m = (Map<Id<?>, ?>) o;
			try {
				Iterator<java.util.Map.Entry<Id<T>, V>> iter = entrySet().iterator();
				while (iter.hasNext()) {
					java.util.Map.Entry<Id<T>, V> e = iter.next();
					Id<T> key = e.getKey();
					V value = e.getValue();
					if (value == null) {
						if (!(m.get(key) == null && m.containsKey(key)))
							return false;
					} else {
						if (!value.equals(m.get(key)))
							return false;
					}
				}
			} catch (ClassCastException | NullPointerException noIdAsKey) {
				return false;
			}
			return true;
		}
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int i = 0; i < data.length; i++) {
			h += data[i] == null ? 0 : i ^ data[i].hashCode();
		}
		return h;
	}

	private static class DataCollection<K, V> implements Collection<V> {

		private final IdMap<K, V> map;

		public DataCollection(IdMap map) {
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
			return new DataIterator<K, V>(this.map);
		}

		@Override
		public Object[] toArray() {
			Object[] array = new Object[this.map.size];
			int index = 0;
			for (Object v : this.map.data) {
				if (v != null) {
					array[index] = v;
					index++;
				}
			}
			return array;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			T[] values = a;
			if (values == null) {
				values = (T[]) new Object[this.map.size];
			} else if (values.length < this.map.size) {
				values = Arrays.copyOf(values, this.map.size);
			} else if (values.length > this.map.size) {
				Arrays.fill(values, this.map.size, values.length, null);
			}

			int index = 0;
			for (Object v : this.map.data) {
				if (v != null) {
					values[index] = (T) v;
					index++;
				}
			}

			return values;
		}

		@Override
		public boolean add(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			if (o == null) {
				return false;
			}
			Object[] data = this.map.data;
			for (int i = 0; i < data.length; i++) {
				if (o.equals(data[i])) {
					this.map.remove(i);
					return true;
				}
			}
			return false;
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
			boolean changed = false;
			for (Object o : c) {
				changed = this.remove(o) | changed;
			}
			return changed;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean changed = false;
			Set<?> set = new HashSet<>(c);
			Object[] data = this.map.data;
			for (int i = 0; i < data.length; i++) {
				Object v = data[i];
				if (!set.contains(v)) {
					this.map.data[i] = null;
					this.map.size--;
					changed = true;
				}
			}
			return changed;
		}

		@Override
		public void clear() {
			this.map.clear();
		}
	}

	private static class DataIterator<K, V> implements Iterator<V> {

		private final IdMap<K, V> map;
		private final Object[] data;
		private int index = 0;
		private int currentIndex = -1;
		private int nextIndex = -1;
		private Object next;

		DataIterator(IdMap<K, V> map) {
			this.map = map;
			this.data = map.data;
			findNext();
		}

		private void findNext() {
			this.next = null;
			while (this.next == null && this.index < this.data.length) {
				this.nextIndex = this.index;
				this.next = this.data[this.index];
				this.index++;
			}
		}

		@Override
		public boolean hasNext() {
			return this.next != null ;
		}

		@Override
		public V next() {
			if (this.next == null) {
				throw new NoSuchElementException();
			}
			Object tmp = this.next;
			this.currentIndex = this.nextIndex;
			findNext();
			return (V) tmp;
		}

		@Override
		public void remove() {
			this.map.remove(this.currentIndex);
		}
	}

	private static class IdIterator<T, D> implements Iterator<Id<T>> {

		private final D[] data;
		private final Class<T> idClass;
		private int index = 0;
		private Id<T> next;

		IdIterator(D[] data, Class<T> idClass) {
			this.data = data;
			this.idClass = idClass;
			findNext();
		}

		private void findNext() {
			this.next = null;
			while (this.next == null && this.index < this.data.length) {
				if (this.data[this.index] != null) {
					this.next = Id.get(this.index, this.idClass);
				}
				this.index++;
			}
		}

		@Override
		public boolean hasNext() {
			return this.next != null ;
		}

		@Override
		public Id<T> next() {
			Id<T> tmp = this.next;
			findNext();
			return tmp;
		}
	}

	private static class KeySet<T, V> implements Set<Id<T>> {

		private final IdMap<T, V> map;

		KeySet(IdMap<T, V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return this.map.size;
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
		public Iterator<Id<T>> iterator() {
			return new IdIterator<>(this.map.data, this.map.idClass);
		}

		@Override
		public Id<T>[] toArray() {
			return toArray(new Id[this.map.size]);
		}

		@Override
		public <K> K[] toArray(K[] a) {
			Id[] keys = (Id[]) a;
			if (keys == null) {
				keys = new Id[this.map.size];
			} else if (keys.length < this.map.size) {
				keys = Arrays.copyOf(keys, this.map.size);
			} else if (keys.length > this.map.size) {
				Arrays.fill(keys, this.map.size, keys.length, null);
			}

			int count = 0;
			Object[] values = this.map.data;
			for (int i = 0; i < values.length; i++) {
				if (values[i] != null) {
					keys[count] = Id.get(i, this.map.idClass);
					count++;
				}
			}
			return (K[]) keys;
		}

		@Override
		public boolean add(Id<T> k) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return this.map.remove(o) != null;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object e : c)
				if (!contains(e))
					return false;
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends Id<T>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			Set<Id<?>> ids = new HashSet<>();
			for (Object o : c) {
				if (o instanceof Id) {
					ids.add((Id<?>) o);
				}
			}
			boolean changed = false;
			for (int i = 0; i < this.map.data.length; i++) {
				if (this.map.data[i] != null) {
					Id<T> t = Id.get(i, this.map.idClass);
					if (!ids.contains(t)) {
						this.map.data[i] = null;
						this.map.size--;
						changed = true;
					}
				}
			}
			return changed;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			Set<Id<?>> ids = new HashSet<>();
			for (Object o : c) {
				if (o instanceof Id) {
					ids.add((Id<?>) o);
				}
			}
			boolean changed = false;
			for (int i = 0; i < this.map.data.length; i++) {
				if (this.map.data[i] != null) {
					Id<T> t = Id.get(i, this.map.idClass);
					if (ids.contains(t)) {
						this.map.data[i] = null;
						this.map.size--;
						changed = true;
					}
				}
			}
			return changed;
		}

		@Override
		public void clear() {
			this.map.clear();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Set))
				return false;
			if (o instanceof KeySet) {
				KeySet<?, ?> k = (KeySet<?, ?>) o;
				if (this.size() != k.size())
					return false;
				for (int i = 0; i < this.map.data.length && i < k.map.data.length; i++) { // one of the data arrays may have more capacity than the other despite having the same number of non-null entries. This is okay if and only if the additional entries are null. This gets checked implicitly by the loop because we already know they have the same number of nun-null elements.
					if ((this.map.data[i] == null && k.map.data[i] != null) || (this.map.data[i] != null && k.map.data[i] == null))
						return false;
				}
				return true;
			} else {
				Collection<?> c = (Collection<?>) o;
				if (c.size() != size())
					return false;
				try {
					return containsAll(c);
				} catch (ClassCastException | NullPointerException unused) {
					return false;
				}
			}
		}

		@Override
		public int hashCode() {
			int h = 0;
			for (int i = 0; i < this.map.data.length; i++) {
				h += this.map.data[i] == null ? 0 : i;
			}
			return h;
		}
	}

	private static class EntrySet<T, V> implements Set<Map.Entry<Id<T>, V>> {

		private final IdMap<T, V> map;

		EntrySet(IdMap<T, V> map) {
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
			if (o instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
				return e.getValue().equals(this.map.get(e.getKey()));
			}
			return false;
		}

		@Override
		public Iterator<Map.Entry<Id<T>, V>> iterator() {
			return new EntryIterator<>(this.map);
		}

		@Override
		public Object[] toArray() {
			return toArray(new Entry[this.map.size]);
		}

		@Override
		public <T> T[] toArray(T[] a) {
			Entry[] entries = (Entry[]) a;
			if (entries == null) {
				entries = new Entry[this.map.size];
			} else if (entries.length < this.map.size) {
				entries = Arrays.copyOf(entries, this.map.size);
			} else if (entries.length > this.map.size) {
				Arrays.fill(entries, this.map.size, entries.length, null);
			}

			int count = 0;
			Object[] values = this.map.data;
			for (int i = 0; i < values.length; i++) {
				if (values[i] != null) {
					entries[count] = new Entry<>(this.map, i);
					count++;
				}
			}
			return (T[]) entries;
		}

		@Override
		public boolean add(Map.Entry<Id<T>, V> entry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry e = (Entry) o;
				Object k = e.getKey();
				if (k instanceof Id) {
					Id id = (Id) k;
					int index = id.index();
					V value = this.map.get(index);
					if (value != null && value.equals(e.getValue())) {
						this.map.remove(id);
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Map.Entry<Id<T>, V>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			this.map.clear();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Set))
				return false;
			if (o instanceof EntrySet) {
				EntrySet<?, ?> e = (EntrySet<?, ?>) o;
				return this.map.equals(e.map);
			} else {
				Collection<?> c = (Collection<?>) o;
				if (c.size() != size())
					return false;
				try {
					return containsAll(c);
				} catch (ClassCastException | NullPointerException unused) {
					return false;
				}
			}
		}

		@Override
		public int hashCode() {
			return this.map.hashCode();
		}
	}

	public static class Entry<T, V> implements Map.Entry<Id<T>, V> {

		private final IdMap<T, V> map;
		private final V value;
		private final int index;

		public Entry(IdMap<T, V> map, int index) {
			this.map = map;
			this.index = index;
			this.value = this.map.get(index);
		}

		@Override
		public Id<T> getKey() {
			return Id.get(this.index, this.map.idClass);
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(V value) {
			return this.map.put(this.index, value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Map.Entry))
				return false;
			if (o instanceof Entry) {
				Entry<?, ?> e = (Entry<?, ?>) o;
				return this.index == e.index && this.value.equals(e.value); // Since our values should never be null we can skip the null-check they do in AbstractMap#eq(Object, Object)
			} else {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
				if (e.getKey() == null || e.getValue() == null)
					return false; // our keys and values should never be null
				return this.getKey().equals(e.getKey()) && this.value.equals(e.getValue());
			}
		}

		@Override
		public int hashCode() {
			return index ^ (value == null ? 0 : value.hashCode());
		}
	}

	private static class EntryIterator<T, V> implements Iterator<Map.Entry<Id<T>, V>> {

		private final IdMap<T, V> map;
		private final Object[] data;
		private int index = 0;
		private Entry<T, V> next;
		private Entry<T, V> current;

		EntryIterator(IdMap<T, V> map) {
			this.map = map;
			this.data = map.data;
			findNext();
		}

		private void findNext() {
			this.next = null;
			while (this.next == null && this.index < this.data.length) {
				if (this.map.data[this.index] != null) {
					this.next = new Entry<>(this.map, this.index);
				}
				this.index++;
			}
		}

		@Override
		public boolean hasNext() {
			return this.next != null ;
		}

		@Override
		public Map.Entry<Id<T>, V> next() {
			this.current = this.next;
			findNext();
			return this.current;
		}

		@Override
		public void remove() {
			this.map.remove(this.current.index);
			this.current = null;
		}
	}
}
