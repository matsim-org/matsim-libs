/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author dgrether
 *
 */
public class IntegerCountMap<T> implements Map<T, Integer> {

	private Map<T, Integer> delegate = new HashMap<T, Integer>();

	public int incrementValue(T key) {
		Integer i = delegate.get(key);
		if (i == null) {
			i = Integer.valueOf(1);
			delegate.put(key, i);
		}
		else {
			delegate.remove(key);
			i = Integer.valueOf(i.intValue() + 1);
			delegate.put(key, i);
		}
		return i.intValue();
	}
	
	
	public void clear() {
		delegate.clear();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	public Set<Entry<T, Integer>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public Integer get(Object key) {
		return delegate.get(key);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public Set<T> keySet() {
		return delegate.keySet();
	}

	public Integer put(T key, Integer value) {
		return delegate.put(key, value);
	}

	public void putAll(Map<? extends T, ? extends Integer> t) {
		delegate.putAll(t);
	}

	public Integer remove(Object key) {
		return delegate.remove(key);
	}

	public int size() {
		return delegate.size();
	}

	public Collection<Integer> values() {
		return delegate.values();
	}
	
	
}
