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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Represents a unique identifier.
 *
 * Note that Ids should not contain any whitespace characters (spaces, tabs, newlines, ...),
 * as this may lead to problems when Ids are written to file and read back in.
 *
 *  @author mrieser / Senozon AG
 */
public abstract class Id<T> implements Comparable<Id<T>> {

	private final static ConcurrentMap<Class<?>, ConcurrentMap<String, Id<?>>> cacheId = new ConcurrentHashMap<>();
	private final static ConcurrentMap<Class<?>, List<Id<?>>> cacheIndex = new ConcurrentHashMap<>();

	/** Resets all internal caches used by this class.
	 * <em>This method must only be called from JUnit-Tests.</em>
	 */
	public static void resetCaches() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		boolean fromJUnit = false;
		for (StackTraceElement element : elements) {
			if (element.getClassName().contains("junit.")) {
				fromJUnit = true;
				break;
			}
		}
		if (!fromJUnit) {
			throw new RuntimeException("This method can only be called from JUnit-Tests, but not in normal code!");
		}
		cacheId.clear();
		cacheIndex.clear();
	}

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
		Gbl.assertNotNull(key);

		ConcurrentMap<String, Id<?>> mapId = cacheId.computeIfAbsent(type, k -> new ConcurrentHashMap<>(1000));

		Id<?> id = mapId.get(key);
		if (id == null) {
			//Double-Checked Locking works: mapId is concurrent and IdImpl is immutable
			synchronized (mapId) {
				id = mapId.get(key);
				if (id == null) {
					// cannot use cacheIndex.computeIfAbsent():
					// split into cacheIndex.get() and put() so that mapIndex.add() "happens-before" mapIndex.get()
					// alternatives:
					// (1) synchronise mapIndex.get() calls (on mapIndex)
					// (2) use cacheIndex.compute() instead of get() and put() ==> less readable code...
					List<Id<?>> mapIndex = mapId.isEmpty() ? new ArrayList<>(1000) : cacheIndex.get(type);
					int index = mapIndex.size();
					id = new IdImpl<T>(key, index);
					mapIndex.add(id);
					cacheIndex.put(type, mapIndex);
					mapId.put(key, id);
				}
			}
		}
		return (Id<T>)id;
	}

	public abstract int index();

	public static <T> Id<T> get(int index, final Class<T> type) {
		List<Id<?>> mapIndex = cacheIndex.get(type);
		return mapIndex == null ? null : (Id<T>)mapIndex.get(index);
	}

	public static <T> Id<T> get(String id, final Class<T> type) {
		Map<String, Id<?>> mapId = cacheId.get(type);
		return mapId == null ? null : (Id<T>)mapId.get(id);
	}

	public static <T> int getNumberOfIds(final Class<T> type) {
		Map<String, Id<?>> mapId = cacheId.get(type);
		return mapId == null ? 0 : mapId.size();
	}

	/**
	 * @return <code>0</code> when the two objects being compared are the same objects, other values according to their ids being compared to each other.
	 *
	 * @throws IllegalArgumentException when the two objects being compared have the same id, but are not the same object because this means they must have different generic types
	 */
	@Override
	public int compareTo(Id<T> o) throws IllegalArgumentException {
		return this.toString().compareTo(o.toString());
//		return Integer.compare(this.index(), o.index()); // this would be more efficient, but changes some test results due to different ordering
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
		public int index() {
			return this.index;
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
			// this.index  would be an alternative implementation for the hashCode
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

	public static Id<VehicleType> createVehicleTypeId( final long key ) { return create( key, VehicleType.class ); }
	public static Id<VehicleType> createVehicleTypeId( final Id<?> id ) {
		return create( id, VehicleType.class ) ;
	}
	public static Id<VehicleType> createVehicleTypeId( final String str ) {
		return create( str, VehicleType.class ) ;
	}

}
