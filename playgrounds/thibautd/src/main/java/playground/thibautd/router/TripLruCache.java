/* *********************************************************************** *
 * project: org.matsim.*
 * TripLruCache.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

/**
 * @author thibautd
 */
public class TripLruCache {
	private static final Logger log =
		Logger.getLogger(TripLruCache.class);

	public static enum LocationType {coord, link, facility;}
	private final boolean considerPerson;
	private final LocationType locationType;

	/**
	 * The cache works this way:
	 * - it stores at least cacheSize elements in an LRU cache 
	 * - mappings are also remembered in a map of softreferences.
	 *
	 * The idea is the following: the "real" cache is the softrefs map,
	 * which keeps objects as long as their key is in the lru; the lru
	 * is here to prevent the most recently used elements to be garbage collected.
	 *
	 * So this is a LRU which keeps elements as long as the garbage collector is
	 * happy with that --- the meaning of "the garbage collector being happy with
	 * that" being JVM dependent...
	 */
	private final Map<Departure, List<? extends PlanElement>> lru;
	private final Map<Departure, SoftEntry> softRefsMap =
		new HashMap<Departure, SoftEntry>();
	private final ReferenceQueue<List<? extends PlanElement>> queue = new ReferenceQueue<List<? extends PlanElement>>();

	public TripLruCache(
			final boolean considerPerson,
			final LocationType locationType,
			final int cacheSize) {
		this.considerPerson = considerPerson;
		this.locationType = locationType;
		this.lru =
			new LinkedHashMap<Departure, List<? extends PlanElement>>( (int) (1.6 * cacheSize) , 0.75f , true ) {
				private static final long serialVersionUID = 1L;
				@Override
				protected boolean removeEldestEntry(final Map.Entry<Departure, List<? extends PlanElement>> eldest) {
					return size() >= cacheSize;
				}
			};
	}

	private void processQueue() {
		int c = 0;
		for ( SoftEntry e = (SoftEntry) queue.poll();
				e != null;
				e = (SoftEntry) queue.poll() ) {
			c++;
			assert !lru.containsKey( e.key );
			softRefsMap.remove( e.key );
		}

		if ( c > 0 && log.isTraceEnabled() ) {
			log.trace( this+": processed "+c+" GC'd references" );
		}
	}

	public List<? extends PlanElement> get( final Departure departure ) {
		processQueue();

		// first get element from lru, to generate an access
		final List<? extends PlanElement> t = lru.get( departure );
		if ( t != null ) return clone( t );

		// was not in the LRU: check if it is in the soft references
		final SoftEntry sr = softRefsMap.get( departure );
		if ( sr == null ) return null;

		final List<? extends PlanElement> trip = sr.get();

		if ( trip == null ) {
			// it seems the GC was triggered while we were having fun here...
			processQueue();
		}

		return clone( trip );
	}

	public void put( final Departure departure , final List<? extends PlanElement> trip ) {
		processQueue();
		final List<? extends PlanElement> clone = clone( trip );
		lru.put( departure , clone );
		softRefsMap.put( departure , new SoftEntry( departure , clone ) );
		processQueue();
	}

	/**
	 * The cached instances need to be "cloned", as the instances returned will be
	 * included in the plans and perhaps modified.
	 */
	private static List<? extends PlanElement> clone(List<? extends PlanElement> trip) {
		final List<PlanElement> clone = new ArrayList<PlanElement>( trip.size() );

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) clone.add( clone( (Leg) pe ) );
			else if ( pe instanceof Activity ) clone.add( clone( (Activity) pe ) );
			else throw new RuntimeException( pe.getClass().getName()+"???" );
		}

		return clone;
	}

	private static Leg clone( final Leg leg) {
		final Leg clone = new LegImpl( leg.getMode() );
		clone.setRoute( leg.getRoute().clone() );
		clone.setDepartureTime( leg.getDepartureTime() );
		clone.setTravelTime( leg.getTravelTime() );
		return clone;
	}

	private static Activity clone( final Activity act) {
		return new ActivityImpl( act );
	}

	public Departure createDeparture(
				final Person person,
				final Facility origin,
				final Facility destination) {
		return new Departure( person , origin , destination );
	}

	public class Departure {
		private final Id personId;
		private final Facility origin;
		private final Facility destination;

		private Departure(
				final Person person,
				final Facility origin,
				final Facility destination) {
			this.personId = person.getId();
			this.origin = origin;
			this.destination = destination;
		}

		@Override
		public boolean equals(final Object o) {
			if ( !o.getClass().equals( getClass() ) ) return false;
			final Departure other = (Departure) o;

			if ( considerPerson && !other.personId.equals( personId ) ) return false;
			if ( !sameLocation( origin , other.origin ) ) return false;
			if ( !sameLocation( destination , other.destination ) ) return false;

			return true;
		}

		private boolean sameLocation(
				final Facility f1,
				final Facility f2) {
			switch ( locationType ) {
			case coord:
				return f1.getCoord().equals( f2.getCoord() );
			case facility:
				return f1.getId().equals( f2.getId() );
			case link:
				return f1.getLinkId().equals( f2.getLinkId() );
			default:
				throw new RuntimeException( ""+locationType );
			}
		}

		@Override
		public int hashCode() {
			int h = 0;
			if ( considerPerson ) h += personId.hashCode();

			switch ( locationType ) {
			case coord:
				h += origin.getCoord().hashCode();
				h += destination.getCoord().hashCode();
				break;
			case facility:
				h += origin.getId().hashCode();
				h += destination.getId().hashCode();
				break;
			case link:
				h += origin.getLinkId().hashCode();
				h += destination.getLinkId().hashCode();
				break;
			default:
				throw new RuntimeException( ""+locationType );
			}

			return h;
		}
	}

	private class SoftEntry extends SoftReference<List<? extends PlanElement>> {
		private final Departure key;

		public SoftEntry(
				final Departure key,
				final List<? extends PlanElement> value) {
			super( value, queue );
			this.key = key;
		}
	}
}

