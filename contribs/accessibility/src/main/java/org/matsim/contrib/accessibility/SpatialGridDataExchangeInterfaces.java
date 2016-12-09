/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;

/**
 * @author nagel
 *
 */
public class SpatialGridDataExchangeInterfaces implements SpatialGridDataExchangeInterfacesI {
	List<SpatialGridDataExchangeInterface> list = new ArrayList<>() ;

	@Override
	public int size() {
		return this.list.size();
	}

	@Override
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.list.contains(o);
	}

	@Override
	public Iterator<SpatialGridDataExchangeInterface> iterator() {
		return this.list.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.list.toArray(a);
	}

	@Override
	public boolean add(SpatialGridDataExchangeInterface e) {
		return this.list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return this.list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends SpatialGridDataExchangeInterface> c) {
		return this.list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends SpatialGridDataExchangeInterface> c) {
		return this.list.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.list.retainAll(c);
	}

	@Override
	public void clear() {
		this.list.clear();
	}

	@Override
	public boolean equals(Object o) {
		return this.list.equals(o);
	}

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}

	public SpatialGridDataExchangeInterface get(int index) {
		return this.list.get(index);
	}

	public SpatialGridDataExchangeInterface set(int index, SpatialGridDataExchangeInterface element) {
		return this.list.set(index, element);
	}

	public void add(int index, SpatialGridDataExchangeInterface element) {
		this.list.add(index, element);
	}

	public SpatialGridDataExchangeInterface remove(int index) {
		return this.list.remove(index);
	}

	public int indexOf(Object o) {
		return this.list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return this.list.lastIndexOf(o);
	}

	public ListIterator<SpatialGridDataExchangeInterface> listIterator() {
		return this.list.listIterator();
	}

	public ListIterator<SpatialGridDataExchangeInterface> listIterator(int index) {
		return this.list.listIterator(index);
	}

	public List<SpatialGridDataExchangeInterface> subList(int fromIndex, int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}

}
