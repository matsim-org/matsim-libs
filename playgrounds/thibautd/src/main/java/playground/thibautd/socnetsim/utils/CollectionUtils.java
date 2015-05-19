/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionUtils.java
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
package playground.thibautd.socnetsim.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class CollectionUtils {
	public static <T> List<T> getRandomDistinctElements(
			final Random random,
			final List<T> list,
			final int nElements ) {
		if ( list.size() < nElements ) throw new IllegalArgumentException( "cannot sample "+nElements+" elements from "+list.size() );

		// TODO: avoid creating "bowl" collection
		final List<T> bowl = new ArrayList<T>( list );
		final List<T> sample = new ArrayList<T>( nElements );

		for ( int i=0; i < nElements; i++ ) {
			sample.add(
					bowl.remove(
						random.nextInt(
							bowl.size() ) ) );
		}

		return sample;
	}

	public static boolean intersects(
			final Collection<?> c1,
			final Collection<?> c2) {
		for ( Object o : c1 ) {
			if ( c2.contains( o ) ) return true;
		}
		return false;
	}

	public static <T> Set<T> intersect(
			final Collection<? extends T> c1,
			final Collection<? extends T> c2) {
		final Set<T> set = new HashSet<T>();
		for ( T t : c1 ) {
			if ( c2.contains( t ) ) set.add( t );
		}
		return set;
	}

	public static <T extends Comparable<T>> SortedSet<T> intersectSorted(
			final Collection<? extends T> c1,
			final Collection<? extends T> c2) {
		final SortedSet<T> set = new TreeSet<T>();
		for ( T t : c1 ) {
			if ( c2.contains( t ) ) set.add( t );
		}
		return set;
	}

	/**
	 * makes sense only if iteration order deterministic!
	 */
	public static <K,V> Map.Entry<K,V> getRandomElement(
			final Random random,
			final Map<K,V> map) {
		return getRandomElement( false , random , map );
	}

	/**
	 * makes sense only if iteration order deterministic!
	 */
	public static <K,V> Map.Entry<K,V> removeRandomElement(
			final Random random,
			final Map<K,V> map) {
		return getRandomElement( true , random , map );
	}


	/**
	 * makes sense only if iteration order deterministic!
	 */
	private static <K,V> Map.Entry<K,V> getRandomElement(
			final boolean remove,
			final Random random,
			final Map<K,V> map) {
		if ( map.isEmpty() ) throw new IllegalArgumentException( "map is empty!" );
		final int index = random.nextInt( map.size() );

		final Iterator<Map.Entry<K,V>> it = map.entrySet().iterator();
		int i=0;

		while ( i++ < index ) it.next();

		final Map.Entry<K,V> elem = it.next();
		if ( remove ) it.remove();
		return elem;
	}

	/**
	 * makes sense only if iteration order deterministic!
	 */
	public static <T> T removeElement(
			final int index,
			final Collection<T> coll) {
		return getElement( true , index , coll );
	}

	/**
	 * makes sense only if iteration order deterministic!
	 */
	public static <T> T getElement(
			final int index,
			final Collection<T> coll) {
		return getElement( false , index , coll );
	}

	/**
	 * makes sense only if iteration order deterministic!
	 */
	private static <T> T getElement(
			final boolean remove,
			final int index,
			final Collection<T> coll) {
		if ( index >= coll.size() ) throw new IndexOutOfBoundsException( index+" >= "+coll.size() );

		final Iterator<T> it = coll.iterator();
		int i=0;

		while ( i++ < index ) it.next();

		final T elem = it.next();
		if ( remove ) it.remove();
		return elem;
	}

	public static List<String> toString(
			final List<? extends Object> list) {
		final List<String> strings = new ArrayList<String>( list.size() );
		for ( Object o : list ) strings.add( o.toString() );
		return strings;
	}
}

