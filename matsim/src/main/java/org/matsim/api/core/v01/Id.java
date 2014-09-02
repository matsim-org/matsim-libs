/* *********************************************************************** *
 * project: org.matsim.*
 * IdI.java
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

package org.matsim.api.core.v01;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Represents a unique identifier.
 * 
 * Note that Ids should not contain any whitespace characters (spaces, tabs, newlines, ...), 
 * as this may lead to problems when Ids are written to file and read back in.
 * 
 *  @author mrieser / Senozon AG
 */
public abstract class Id<T> implements Comparable<Id<T>> {

	private final static Map<Class<?>, Map<String, Id<?>>> cache = new ConcurrentHashMap<Class<?>, Map<String, Id<?>>>();
	
	
	public static <T> Id<T> create(final long key, final Class<T> type) {
		return create(Long.toString(key), type);
	}
	
	/**
	 * This method supports a cache where ids are stored and re-used per type.   
	 */
	public static <T> Id<T> create(final String key, final Class<T> type) {
		Map<String, Id<?>> map = cache.get(type);
		if (map == null) {
			map = new ConcurrentHashMap<String, Id<?>>();
			cache.put(type, map);
		}
		Id<?> id = map.get(key);
		if (id == null) {
			id = new IdImpl<T>(key);
			map.put(key, id);
		}
		
		return (Id<T>) id;
	}
	
	/**
	 * @return <code>0</code> when the two objects being compared are the same objects, other values according to their ids being compared to each other.
	 * 
	 * @throws IllegalArgumentException when the two objects being compared have the same id, but are not the same object because this means they must have different generic types
	 */
	@Override
	public int compareTo(Id<T> o) throws IllegalArgumentException {
		int res = this.toString().compareTo(o.toString());
		if (res == 0) {
			if (equals(o)) {
				return 0;
			}
			throw new IllegalArgumentException("The ids are equal but of different types.");
		}
		return res;
	}
	
	@Override
	public boolean equals(Object obj) {
		return this == obj;
		// all other objects have to be different by definition, as long as the cache is correctly implemented
	}

	
	/**
	 * The default implementation to be used for Ids.
	 * Have this as a separate class instead of integrated into the Id class
	 * to allow for future optimization of Ids.
	 * 
	 * @author mrieser
	 *
	 * @param <T>
	 */
	private static class IdImpl<T> extends Id<T> {

		private final String id; 
		
		/*package*/ IdImpl(final String id) {
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
	
}