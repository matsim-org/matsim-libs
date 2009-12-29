/* *********************************************************************** *
 * project: org.matsim.*
 * ArraySet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ArraySet<E> implements Set<E>, List<E> {

	private ArrayList<E> arrayList = new ArrayList<E>();
	
	public boolean add(E e) {
		if(arrayList.contains(e))
			return false;
		else
			return arrayList.add(e);
	}

	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for(E e : c) {			
			if(add(e))
				result = true;
		}
		return result;
	}

	public void clear() {
		arrayList.clear();
	}

	public boolean contains(Object o) {
		return arrayList.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return arrayList.containsAll(c);
	}

	public boolean isEmpty() {
		return arrayList.isEmpty();
	}

	public Iterator<E> iterator() {
		return arrayList.iterator();
	}

	public boolean remove(Object o) {
		return arrayList.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return arrayList.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return arrayList.retainAll(c);
	}

	public int size() {
		return arrayList.size();
	}

	public Object[] toArray() {
		return arrayList.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return arrayList.toArray(a);
	}

	public void add(int index, E element) {
		arrayList.add(index, element);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		return arrayList.addAll(index, c);
	}

	public E get(int index) {
		return arrayList.get(index);
	}

	public int indexOf(Object o) {
		return arrayList.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return arrayList.lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		return arrayList.listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return arrayList.listIterator(index);
	}

	public E remove(int index) {
		return arrayList.remove(index);
	}

	public E set(int index, E element) {
		return arrayList.set(index, element);
	}

	public List<E> subList(int fromIndex, int toIndex) {
		return arrayList.subList(fromIndex, toIndex);
	}
	
	public void trimToSize() {
		arrayList.trimToSize();
	}

}
