package org.matsim.api.core.v01;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdMap<T, D> implements Iterable<D> {

	private Class<T> idClass;
	private Class<D> dataClass;
	private int size = 0;
	private D[] data;

	public IdMap(Class<T> idClass, Class<D> dataClass) {
		this(idClass, dataClass, Math.max(Id.getNumberOfIds(idClass), 100));
	}

	public IdMap(Class<T> idClass, Class<D> dataClass, int size) {
		this.idClass = idClass;
		this.dataClass = dataClass;
		this.data = (D[]) Array.newInstance(this.dataClass, size);
	}

	public int size() {
		return this.size;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public D get(Id<T> key) {
		int idx = key.index();
		if (idx < this.data.length) {
			return this.data[idx];
		}
		return null;
	}

	public D put(Id<T> key, D value) {
		int idx = key.index();
		ensureCapacity(idx);
		D oldValue = this.data[idx];
		this.data[idx] = value;
		if (value != null && oldValue == null) {
			this.size++;
		}
		if (value == null && oldValue != null) {
			this.size--;
		}
		return oldValue;
	}

	private void ensureCapacity(int index) {
		if (index >= this.data.length) {
			D[] tmp = (D[]) Array.newInstance(this.dataClass, index + 100);
			System.arraycopy(this.data, 0, tmp, 0, this.data.length);
			this.data = tmp;
		}
	}
	public D remove(Id<T> key) {
		int idx = key.index();
		if (idx < this.data.length) {
			D oldValue = this.data[idx];
			this.data[idx] = null;
			if (oldValue != null) {
				this.size--;
			}
			return oldValue;
		}
		return null;
	}

	public void clear() {
		this.size = 0;
		Arrays.fill(this.data, null);
	}

	public void forEach(BiConsumer<? super Id<T>, ? super D> action) {
		for (int i = 0; i < this.data.length; i++) {
			D o = this.data[i];
			if (o != null) {
				action.accept(Id.get(i, this.idClass), o);
			}
		}
	}

	public Collection<D> values() {
		ArrayList<D> list = new ArrayList<>(this.size);
		for (D o : this.data) {
			if (o != null) {
				list.add(o);
			}
		}
		return list;
	}

	@Override
	public Iterator<D> iterator() {
		return new DataIterator<>(this.data);
	}

	private static class DataIterator<D> implements Iterator<D> {

		private final D[] data;
		private int index = 0;
		private D next;

		public DataIterator(D[] data) {
			this.data = data;
			findNext();
		}

		private void findNext() {
			this.next = null;
			while (this.next == null && this.index < this.data.length) {
				this.next = this.data[index];
				this.index++;
			}
		}

		@Override
		public boolean hasNext() {
			return this.next != null ;
		}

		@Override
		public D next() {
			D tmp = this.next;
			findNext();
			return tmp;
		}
	}
}
