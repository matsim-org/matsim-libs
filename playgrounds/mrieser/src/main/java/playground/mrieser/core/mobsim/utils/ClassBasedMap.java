/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.core.utils.misc.ClassUtils;

/**
 * Helper class providing a lookup-table where the key is a Java Class, and all sub-classes
 * of this key-Class are recognized as valid keys in {@link #get(Class) get()}. Thus, one
 * could e.g. put the interface as key in {@link #put(Class, Object) put()}, but use any
 * actual implementation of that interface in <code>get()</code>.
 * <br />
 * If a type is a sub-class of two or more types added to the map, <code>get()</code> will
 * return some of the mapped values when queried with that sub-class. It cannot be
 * guaranteed that always the same value will be returned, except the sub-class itself is
 * also added to the map, in which case the direct value will be returned.  Example:
 * class <code>AB</code> implements both interfaces <code>A</code> and <code>B</code>. If
 * both <code>A</code> and <code>B</code> are added to the Map, and the Map is queried with
 * <code>AB.class</code>, the map may return either the value associated with
 * <code>A.class</code> or <code>B.class</code>. If <code>AB</code> is also added to the
 * map, <code>get(AB.class)</code> will return the value associated with <code>AB</code>.
 * <br />
 * <br />
 * Note: This class is threadsafe
 *
 * @author mrieser
 *
 * @param <K> the type of classes used as keys in this map
 * @param <V> the type of mapped values
 */
public class ClassBasedMap<K, V> {

	private final Map<Class<? extends K>, V> mainMap = new ConcurrentHashMap<Class<? extends K>, V>();
	private final Map<Class<? extends K>, Class<?>> cacheReduction = new ConcurrentHashMap<Class<? extends K>, Class<?>>();

	/**
	 * Associates the specified value with the specified key in this map. If the map previously
	 * contained a mapping for the key, the old value is replaced by the specified value.
	 *
	 * @param key
	 * @param value
	 * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 */
	public V put(final Class<? extends K> key, final V value) {
		V oldValue = this.mainMap.put(key, value);
		this.cacheReduction.clear();
		return oldValue;
	}

	/**
	 * Returns the associated mapped value for the given key. A value is
	 * not only associated with the class <tt>key</tt> it was registered
	 * (see {@link #put(Class, Object)}), but also with all sub-classes
	 * of <tt>key</tt>.
	 *
	 * @param key
	 * @return
	 */
	public V get(final Class<? extends K> key) {
		V value = this.mainMap.get(key);
		if (value == null) {
			Class<?> newKey = this.cacheReduction.get(key);
			if (newKey == null) {
				for (Class<?> klass : ClassUtils.getAllTypes(key)) {
					value = this.mainMap.get(klass);
					if (value != null) {
						this.cacheReduction.put(key, klass);
						break;
					}
				}
			} else {
				value = this.mainMap.get(newKey);
			}
		}
		return value;
	}

	public V remove(final Class<? extends K> key) {
		V value = this.mainMap.remove(key);
		if (value != null) {
			this.cacheReduction.clear();
		}
		return value;
	}

}
