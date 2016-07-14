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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;


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
	
	public static <T> Id<T> create(final Id<?> id, final Class<T> type) {
		if (id == null) {
			return null;
		}
		return create(id.toString(), type);
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
//		if (res == 0) {   // FIXME temporary relax the check until the Id migration has taken place
//			if (equals(o)) {
//				return 0;
//			}
//			throw new IllegalArgumentException("The ids are equal but of different types.");
//		}
		return res;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Id) {
			return this.compareTo((Id) obj) == 0;
		}
		return false;
//		return this == obj; // FIXME temporary relax the check until the Id migration has taken place
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

	// helper classes for some common cases:
	public static Id<Person> createPersonId( final long key ) {
		return create( key, Person.class ) ;
	}
	public static Id<Person> createPersonId( final Id<?> id ) {
		return create( id, Person.class ) ;
	}
	public static Id<Person> createPersonId( final String str ) {
		return create( str, Person.class ) ;
	}
	public static Id<Link> createLinkId( final long key ) {
		return create( key, Link.class ) ;
	}
	public static Id<Link> createLinkId( final Id<?> id ) {
		return create( id, Link.class ) ;
	}
	public static Id<Link> createLinkId( final String str ) {
		return create( str, Link.class ) ;
	}
	public static Id<Node> createNodeId( final long key ) {
		return create( key, Node.class ) ;
	}
	public static Id<Node> createNodeId( final Id<?> id ) {
		return create( id, Node.class ) ;
	}
	public static Id<Node> createNodeId( final String str ) {
		return create( str, Node.class ) ;
	}
	public static Id<Vehicle> createVehicleId( final long key ) {
		return create( key, Vehicle.class ) ;
	}
	public static Id<Vehicle> createVehicleId( final Id<?> id ) {
		return create( id, Vehicle.class ) ;
	}
	public static Id<Vehicle> createVehicleId( final String str ) {
		return create( str, Vehicle.class ) ;
	}
	
}