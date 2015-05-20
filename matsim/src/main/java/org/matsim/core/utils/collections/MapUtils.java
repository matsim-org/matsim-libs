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
package org.matsim.core.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utils class for the common pattern of creating an entry in a map if nothing is associated to the requested key.
 * This is typically useful for weights or counters (Double or Integer), or one-to-many mappings (Lists or Sets).
 *
 * @author thibautd
 */
public final class MapUtils {
	private MapUtils() {}

	/**
	 * Gets the collection associated with the key in this map, or create an empty list if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <V> parameter of the collection type
	 * @return the collection (evt. newly) associated with the key
	 */
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

	/**
	 * Gets the collection associated with the key in this map, or create an empty list if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <V> parameter of the List type
	 * @return the List (evt. newly) associated with the key
	 */
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

	/**
	 * Gets the set associated with the key in this map, or create an empty set if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <V> parameter of the set type
	 * @return the set (evt. newly) associated with the key
	 */
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

	/**
	 * Gets the map associated with the key in this map, or create an empty map if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the in the primary map
	 * @param <C> type of the key in the secondary map
	 * @param <V> type of the values in the secondary map
	 * @return the Map (evt. newly) associated with the key
	 */
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

	/**
	 * Gets the object associated with the key in this map, or create one if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <T> parameter of the values type
	 * @param fact the factory to use to create a default object if none is found
	 * @return the Object (evt. newly) associated with the key
	 */
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

	/**
	 * Gets the double associated with the key in this map, or create a new one if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param initialValue the value to which new entries should be initialized
	 * @param <K> type of the key
	 * @return the value (evt. newly) associated with the key
	 */
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

	/**
	 * Add a given value to the Double associated with the key in this map, or initialize a new one if no mapping exists
	 * yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param initialValue the value to which new entries should be initialized
	 * @param toAdd the value to add to the existing mapped values
	 * @param <K> type of the key
	 * @return the collection (evt. newly) associated with the key
	 */
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


	/**
	 * Gets the Integer associated with the key in this map, or create a new one if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param initialValue the value to which new entries should be initialized
	 * @param <K> type of the key
	 * @return the value (evt. newly) associated with the key
	 */
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

	/**
	 * Add a given value to the Integer associated with the key in this map, or initialize a new one if no mapping exists
	 * yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param initialValue the value to which new entries should be initialized
	 * @param toAdd the value to add to the existing mapped values
	 * @param <K> type of the key
	 * @return the collection (evt. newly) associated with the key
	 */
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

	/**
	 * Fill in a collection with the values associated with the requested keys
	 *
	 * @param keys the keys to search for. If a key appears several times, its value will be added to the collection the
	 *             corresponding number of times.
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <V> type of the values
	 * @return a new collection, filled in with the values associated with the keys provided, in iteration order.
	 */
	public static <K,V> Collection<V> get(
			final Iterable<K> keys,
			final Map<K, V> map ) {
		final List<V> coll = new ArrayList<V>();

		for ( K k : keys ) coll.add( map.get( k ) );

		return coll;
	}

	public interface Factory<T> {
		T create();
	}

	/**
	 * Helper class, to use as a factory if objects should be instanciated using their parameter-less constructor.
	 *
	 * @param <T>
	 */
	public static class DefaultFactory<T> implements Factory<T> {
		private final Class<? extends T> theClass;

		public DefaultFactory( final Class<? extends T> theClass ) {
			this.theClass = theClass;
		}

		/**
		 * @throws RuntimeException if the constructor does not exist or throws an exception
		 * @return a new instance
		 */
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

