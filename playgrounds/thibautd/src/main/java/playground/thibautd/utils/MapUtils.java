/* *********************************************************************** *
 * project: org.matsim.*
 * MapUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class MapUtils {
	private MapUtils() {}

	public static <K,V> Collection<V> getCollection(
			final K key,
			final Map<K, Collection<V>> map) {
		Collection<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new ArrayList<V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,V> List<V> getList(
			final K key,
			final Map<K, List<V>> map) {
		List<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new ArrayList<V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,V> Set<V> getSet(
			final K key,
			final Map<K, Set<V>> map) {
		Set<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new HashSet<V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,C,V> Map<C,V> getMap(
			final K key,
			final Map<K, Map<C,V>> map) {
		Map<C,V> coll = map.get( key );

		if ( coll == null ) {
			coll = new HashMap<C,V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,T> T getArbitraryObject(
			final K key,
			final Map<K, T> map,
			final Factory<T> fact) {
		T coll = map.get( key );

		if ( coll == null ) {
			coll = fact.create();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K> Double getDouble(
			final K key,
			final Map<K, Double> map,
			final double initialValue) {
		Double d = map.get( key );

		if ( d == null ) {
			d = initialValue;
			map.put( key , d );
		}

		return d;
	}

	public static <K> double addToDouble(
			final K key,
			final Map<K, Double> map,
			final double initialValue,
			final double toAdd) {
		final double newValue =
			getDouble( key , map , initialValue ) +
			toAdd;
		map.put( key , newValue );
		return newValue;
	}

	public static <K> Integer getInteger(
			final K key,
			final Map<K, Integer> map,
			final int initialValue) {
		Integer i = map.get( key );

		if ( i == null ) {
			i = initialValue;
			map.put( key , i );
		}

		return i;
	}

	public static <K> double addToInteger(
			final K key,
			final Map<K, Integer> map,
			final int initialValue,
			final int toAdd) {
		final int newValue =
			getInteger( key , map , initialValue ) +
			toAdd;
		map.put( key , newValue );
		return newValue;
	}

	public static interface Factory<T> {
		public T create();
	}

	public static class DefaultFactory<T> implements Factory<T> {
		private final Class<? extends T> theClass;

		public DefaultFactory( final Class<? extends T> theClass ) {
			this.theClass = theClass;
		}

		@Override
		public T create() {
			try {
				return theClass.getConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}
}

