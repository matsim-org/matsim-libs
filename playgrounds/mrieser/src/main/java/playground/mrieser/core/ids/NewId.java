/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.ids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.Person;


/**
 * @author mrieser / Senozon AG
 */
public abstract class NewId<T> implements Comparable<NewId<T>> {

	private final static Map<Class<?>, Map<String, NewId<?>>> cache = new ConcurrentHashMap<Class<?>, Map<String, NewId<?>>>();
	
	/**
	 * This method is not intended to be used but just to showcase how the mechanism is designed to work in principle.
	 * 
	 * The main problem with this method is, that it does no caching of ids what can lead to huge memory consumption in some cases.
	 * 
	 *  The method should be removed for the final version of this class.
	 */
	public static <T> NewId<T> createSimple(final String key, final Class<T> type) {
		return new NewIdImpl<T>(key);		
	}
	
	
	/**
	 * This method supports a cache where ids are stored and re-used per type.   
	 */
	public static <T> NewId<T> create(final String key, final Class<T> type) {
		Map<String, NewId<?>> map = cache.get(type);
		if (map == null) {
			map = new ConcurrentHashMap<String, NewId<?>>();
			cache.put(type, map);
		}
		NewId<?> id = map.get(key);
		if (id == null) {
			id = createId(key, type);
			map.put(key, id);
		}
		
		return (NewId<T>) id;
	}
	
	/**
	 * Utility method to directly create a person Id.
	 * This could be done, but I dislike that a class like Id would 
	 * than have dependencies on Person, Link, Node, ActivityFacility, TransitLine, ...
	 */
	public static NewId<Person> createPersonId(final String key) {
		return create(key, Person.class);
	}

	/**
	 * This method could as well just consist of its last line.
	 * The other lines before are already an optimization: try to see if the id to be generated
	 * is numeric and use an integer to store it in that case instead of an expensive String.
	 */
	private static <T> NewId<T> createId(final String key, final Class<T> type) {
		try {
			int i = Integer.valueOf(key);
			return new NewIdInteger<T>(i);
		} catch (NumberFormatException e) {
			// ignore, we just tried...
		}
		return new NewIdImpl<T>(key);
	}

	// default implementation of compareTo
	@Override
	public int compareTo(NewId<T> o) {
		int res = this.toString().compareTo(o.toString());
		if (res == 0) {
			if (equals(o)) {
				return 0;
			}
			throw new IllegalArgumentException("The ids are equal but of different types.");
		}
		return res;
	}
	
	// default implementation of equals
	@Override
	public boolean equals(Object obj) {
		return this == obj;
		// all other objects have to be different by definition, as long as the cache is correctly implemented
	}
	
	
	// =====================================================
	// DEFAULT IMPLEMENTATION
	// =====================================================
	
	private static class NewIdImpl<T> extends NewId<T> {

		private final String id; 
		
		/*package*/ NewIdImpl(final String id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}
		
		@Override
		public String toString() {
			return this.id;
		}
	}	

	
	// =====================================================
	// OPTIMIZED IMPLEMENTATION FOR NUMERIC IDs
	// =====================================================
	
	/**
	 * Optimized implementation for storing numeric ids.
	 * This implementation uses far less memory than a string-based storage of the id.
	 * 
	 * @param <T>
	 */
	private static class NewIdInteger<T> extends NewId<T> {

		private final int id; 

		/*package*/ NewIdInteger(final int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			return this.id;
		}

		@Override
		public String toString() {
			return Integer.toString(this.id);
		}
		
//		@Override
//		public int compareTo(NewId<T> o) {
//			if (o instanceof NewIdInteger) {
//				return ((NewIdInteger<T>) o).id - this.id; 
//			}
//			return super.compareTo(o);
//		}
	}	

}
