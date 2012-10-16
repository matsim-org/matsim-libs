/* *********************************************************************** *
 * project: org.matsim.*
 * PoiList
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
package playground.vsp.energy.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author dgrether
 *
 */
@XmlRootElement(name = "validation_information")
public class ValidationInformation implements List<PoiInfo> {

	List<PoiInfo> delegate = new ArrayList<PoiInfo>();
	
	
  @XmlElement(name = "poi_info")
  public List<PoiInfo> getValidationInformationList() {
    return this;
  }


	public int size() {
		return delegate.size();
	}


	public boolean isEmpty() {
		return delegate.isEmpty();
	}


	public boolean contains(Object o) {
		return delegate.contains(o);
	}


	public Iterator<PoiInfo> iterator() {
		return delegate.iterator();
	}


	public Object[] toArray() {
		return delegate.toArray();
	}


	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}


	public boolean add(PoiInfo e) {
		return delegate.add(e);
	}


	public boolean remove(Object o) {
		return delegate.remove(o);
	}


	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}


	public boolean addAll(Collection<? extends PoiInfo> c) {
		return delegate.addAll(c);
	}


	public boolean addAll(int index, Collection<? extends PoiInfo> c) {
		return delegate.addAll(index, c);
	}


	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}


	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}


	public void clear() {
		delegate.clear();
	}


	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}


	@Override
	public int hashCode() {
		return delegate.hashCode();
	}


	public PoiInfo get(int index) {
		return delegate.get(index);
	}


	public PoiInfo set(int index, PoiInfo element) {
		return delegate.set(index, element);
	}


	public void add(int index, PoiInfo element) {
		delegate.add(index, element);
	}


	public PoiInfo remove(int index) {
		return delegate.remove(index);
	}


	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}


	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}


	public ListIterator<PoiInfo> listIterator() {
		return delegate.listIterator();
	}


	public ListIterator<PoiInfo> listIterator(int index) {
		return delegate.listIterator(index);
	}


	public List<PoiInfo> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex);
	}
  
}
