package org.matsim.api.core.v01;

import java.util.*;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdSet<T> implements Set<Id<T>> {

	private final Class<T> idClass;
	private int size = 0;
	private final BitSet data;

	public IdSet(Class<T> idClass) {
		this(idClass, Math.max(Id.getNumberOfIds(idClass), 100));
	}

	public IdSet(Class<T> idClass, int size) {
		this.idClass = idClass;
		this.data = new BitSet(size);
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
	public boolean contains(Object key) {
		if (key instanceof Id) {
			return contains((Id<T>) key);
		}
		return false;
	}

	public boolean contains(Id<T> id) {
		return this.data.get(id.index());
	}

	public boolean contains(int index) {
		return this.data.get(index);
	}

	@Override
	public Iterator<Id<T>> iterator() {
		return new IdSetIterator<>(this);
	}

	@Override
	public Id<T>[] toArray() {
		int index = 0;
		int count = 0;
		Id[] array = new Id[this.size];
		while (true) {
			index = this.data.nextSetBit(index);
			if (index < 0) {
				break;
			}
			array[count] = Id.get(index, this.idClass);
			count++;
			index++;
		}
		return (Id<T>[]) array;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		Object[] array = a;
		if (array == null) {
			array = new Id[this.size];
		} else if (array.length < this.size) {
			array = Arrays.copyOf(a, this.size);
		} else if (array.length > this.size) {
			Arrays.fill(a, this.size, a.length, null);
		}

		int index = 0;
		int count = 0;
		while (true) {
			index = this.data.nextSetBit(index);
			if (index < 0) {
				break;
			}
			array[count] = Id.get(index, this.idClass);
			count++;
			index++;
		}
		return (T[]) array;
	}

	@Override
	public boolean remove(Object key) {
		if (key instanceof Id) {
			return remove((Id<T>) key);
		}
		return false;
	}

	public boolean remove(Id<T> key) {
		return this.remove(key.index());
	}

	private boolean remove(int idx) {
		if (this.data.get(idx)) {
			this.data.clear(idx);
			this.size--;
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (o instanceof Id) {
				if (!this.data.get(((Id) o).index())) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Id<T>> c) {
		boolean changed = false;
		for (Id<T> k : c) {
			int index = k.index();
			if (!this.data.get(index)) {
				this.data.set(index);
				this.size++;
				changed = true;
			}
		}
		return changed;
	}

	public boolean addAll(IdSet<T> m) {
		boolean changed = false;
		int index = 0;
		while (true) {
			index = m.data.nextSetBit(index);
			if (index >= 0) {
				if (!this.data.get(index)) {
					this.data.set(index);
					this.size++;
					changed = true;
				}
				index++;
			} else {
				break;
			}
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		int index = 0;
		while (true) {
			index = this.data.nextSetBit(index);
			if (index >= 0) {
				Id<T> id = Id.get(index, this.idClass);
				if (!c.contains(id)) {
					this.data.clear(index);
					this.size--;
					changed = true;
				}
				index++;
			} else {
				break;
			}
		}
		return changed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			if (o instanceof Id) {
				int index = ((Id) o).index();
				if (this.data.get(index)) {
					this.data.clear(index);
					this.size--;
					changed = true;
				}
			}
		}
		return changed;
	}

	@Override
	public boolean add(Id<T> value) {
		return this.add(value.index());
	}

	private boolean add(int index) {
		boolean hadValue = this.data.get(index);
		this.data.set(index);
		if (!hadValue) {
			this.size++;
		}
		return !hadValue;
	}

	@Override
	public void clear() {
		this.size = 0;
		this.data.clear();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Set))
			return false;
		if (o instanceof IdSet) {
			IdSet<?> m = (IdSet<?>) o;
			return this.idClass.equals(m.idClass) && this.size == m.size && this.data.equals(m.data);
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
		if (isEmpty())
			return -1;
		int h = 0;
		for (int i = 0; i < this.data.length(); i++) {
			h += this.data.get(i) ? i : 0;
		}
		return h;
	}

	private static class IdSetIterator<T> implements Iterator<Id<T>> {

		private final IdSet<T> set;
		private int currentIndex = -1;

		IdSetIterator(IdSet<T> set) {
			this.set = set;
		}

		@Override
		public boolean hasNext() {
			return this.set.data.nextSetBit(this.currentIndex + 1) >= 0;
		}

		@Override
		public Id<T> next() {
			int index = this.set.data.nextSetBit(this.currentIndex + 1);
			if (index >= 0) {
				this.currentIndex = index;
				return Id.get(index, this.set.idClass);
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			this.set.data.clear(this.currentIndex);
		}
	}

}
