/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.population.jointtrippossibilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibilities;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibilities;

/**
 * @author thibautd
 */
public class JointTripPossibilities {
	private String description;
	private final Map<Id, Map<Od, List<Possibility>>> driverPossibilities =
		new HashMap<Id, Map<Od, List<Possibility>>>();
	private final Map<Id, Map<Od, List<Possibility>>> passengerPossibilities =
		new HashMap<Id, Map<Od, List<Possibility>> >();
	private final List<Possibility> possibilities = new ArrayList<Possibility>();

	public JointTripPossibilities() {
		this( null );
	}

	public JointTripPossibilities(final String description) {
		this.description = description;
	}

	public Collection<Possibility> getDriverPossibilities(final Id agent) {
		return flatten( get( agent , driverPossibilities ).values());
	}

	public Collection<Possibility> getPassengerPossibilities(final Id agent) {
		return flatten( get( agent , passengerPossibilities ).values()  );
	}

	public Collection<Possibility> getDriverPossibilities(
			final Id agent,
			final Od od) {
		return get( agent , driverPossibilities ).get( od );
	}

	public Collection<Possibility> getPassengerPossibilities(
			final Id agent,
			final Od od) {
		return get( agent , passengerPossibilities ).get( od );
	}

	public void add(final Possibility p) {
		possibilities.add( p );
		get(
			p.getDriverOd(),
			get( p.getDriver() , driverPossibilities ) ).add( p );
		get(
			p.getPassengerOd(),
			get( p.getPassenger() , passengerPossibilities )  ).add( p );
	}

	public Collection<Possibility> getAll() {
		return Collections.unmodifiableList( possibilities );
	}

	public String getName() {
		return description;
	}

	public void setName(final String name) {
		this.description = name;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Map<Od, List<Possibility>> get(
			final Id key,
			final Map<Id, Map<Od, List<Possibility>>> m) {
		Map<Od, List<Possibility>> map = m.get( key );

		if (map == null) {
			map = new HashMap<Od, List<Possibility>>();
			m.put( key , map );
		}

		return map;
	}

	private static List<Possibility> get(
			final Od key,
			final Map<Od, List<Possibility>> m) {
		List<Possibility> list = m.get( key );

		if (list == null) {
			list = new ArrayList<Possibility>();
			m.put( key , list );
		}

		return list;
	}

	private static <T extends Object> Collection<T> flatten(
			final Collection< ? extends Collection<T> > cs ) {
		List<T> out = new ArrayList<T>();

		for (Collection<T> c : cs) {
			out.addAll( c );
		}

		return out;
	}


	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * defines on origin/destination pair via link ids
	 */
	public static class Od {
		private final Id o,d;

		public static Od create( final Id o , final Id d ) {
			// TODO: pool ( probably lots of equal instances)
			return new Od( o , d );
		}

		private Od(final Id o, final Id d) {
			this.o = o;
			this.d = d;
		}

		public Id getOriginLinkId() {
			return o;
		}

		public Id getDestinationLinkId() {
			return d;
		}

		@Override
		public boolean equals( final Object other ) {
			return other instanceof Od &&
				((Od) other).o.equals( o ) &&
				((Od) other).d.equals( d );
		}

		@Override
		public int hashCode() {
			return o.hashCode() + d.hashCode();
		}

		@Override
		public String toString() {
			return "od=["+o+" -> "+d+"]";
		}
	}

	/**
	 * Defines a "joint trip possibility", as the union
	 * of an Od for a passenger and a driver (passenger
	 * and drivers may have different ods).
	 */
	public static class Possibility {
		private final  Od driverOd, passengerOd;
		private final Id driver, passenger;

		public Possibility(
				final Id driver,
				final Od driverOd,
				final Id passenger,
				final Od passengerOd) {
			this.driver = driver;
			this.driverOd = driverOd;
			this.passenger = passenger;
			this.passengerOd = passengerOd;
		}

		/**
		 * Gets the driverOd for this instance.
		 *
		 * @return The driverOd.
		 */
		public Od getDriverOd() {
			return this.driverOd;
		}

		/**
		 * Gets the passengerOd for this instance.
		 *
		 * @return The passengerOd.
		 */
		public Od getPassengerOd() {
			return this.passengerOd;
		}

		/**
		 * Gets the driver for this instance.
		 *
		 * @return The driver.
		 */
		public Id getDriver() {
			return this.driver;
		}

		/**
		 * Gets the passenger for this instance.
		 *
		 * @return The passenger.
		 */
		public Id getPassenger() {
			return this.passenger;
		}

		@Override
		public String toString() {
			return "possibility=[driver="+driver+"][driverOd="+driverOd+"][passenger="+passenger+"][passengerOd="+passengerOd+"]";
		}
	}
}

