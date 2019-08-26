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
import org.matsim.core.gbl.Gbl;
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

	private final static Map<Class<?>, Map<String, Id<?>>> cache_id = new ConcurrentHashMap<Class<?>, Map<String, Id<?>>>();
	private final static Map<Class<?>, Map<Integer, Id<?>>> cache_index = new ConcurrentHashMap<Class<?>, Map<Integer, Id<?>>>();

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
		Map<String, Id<?>> map_id = cache_id.get(type);
		Map<Integer, Id<?>> map_index = cache_index.get(type);

		if (map_id == null) {
			map_id = new ConcurrentHashMap<String, Id<?>>();
			map_index = new ConcurrentHashMap<Integer, Id<?>>();
			cache_id.put(type, map_id);
			cache_index.put(type, map_index);
		}

		Gbl.assertNotNull(key);

		Id<?> id = map_id.get(key);

		if (id == null) {
			int index = map_index.size(); // TODO - this is not thread safe
			id = new IdImpl<T>(key, index);
			map_id.put(key, id);
			map_index.put(index, id);
		}

		return (Id<T>) id;
	}

	public static <T> Id<T> get(int index, final Class<T> type) {
		Map<Integer, Id<?>> map_index = cache_index.get(type);

		if (map_index == null) {
			return null;
		}

		return (Id<T>)map_index.get(index);
	}

	public static <T> Id<T> get(String id, final Class<T> type) {
		Map<String, Id<?>> map_id = cache_id.get(type);

		if (map_id == null) {
			return null;
		}

		return (Id<T>)map_id.get(id);
	}

	public static <T> int getNumberOfIds(final Class<T> type) {
		return cache_index.get(type).size();
	}
	
	/**
	 * @return <code>0</code> when the two objects being compared are the same objects, other values according to their ids being compared to each other.
	 * 
	 * @throws IllegalArgumentException when the two objects being compared have the same id, but are not the same object because this means they must have different generic types
	 */
	@Override
	public int compareTo(Id<T> o) throws IllegalArgumentException {
		return Integer.compare(this.hashCode(), o.hashCode());
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
		private final int index;
		
		/*package*/ IdImpl(final String id, final int index) {
			this.id = id;
			this.index = index;
		}

		@Override
		public int hashCode() {
			return this.index;
		}
		
		@Override
		public String toString() {
			return this.id;
		}
	}
	
	public static <T> String writeId( Id<T> id ) {
		if ( id==null ) {
			return "null" ;
		}
		return id.toString() ;
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
