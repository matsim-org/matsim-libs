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
package playground.thibautd.negotiation.locationnegotiation;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import playground.ivt.utils.SoftCache;
import playground.thibautd.negotiation.framework.Proposition;

import java.util.Collection;

/**
 * @author thibautd
 */
public class LocationProposition implements Proposition {
	private final static boolean CACHE = false;

	public enum Type {visit, outOfHome, alone}

	private final Person proposer;
	private final Collection<Person> proposed;

	private final ActivityFacility facility;
	private final Type type;

	/* Use a soft cache for created propositions, not for memory reasons (propositions are quickly discarded),
	 * but for avoiding re-computing utility value if re-stumbling on the same proposition.
	 * This goes together with the cached utility.
	 * This of course makes a difference only if proposition is still in cache when encountered for the second time...
	 */
	private static final SoftCache<LocationProposition,LocationProposition> cache = CACHE ? new SoftCache<>() : null;

	private LocationProposition(
			final Person proposer,
			final Collection<Person> proposed,
			final ActivityFacility facility,
			final Type type ) {
		this.proposer = proposer;
		this.proposed = proposed;
		this.facility = facility;
		this.type = type;
	}

	public static LocationProposition create(
			final Person proposer,
			final Collection<Person> proposed,
			final ActivityFacility facility,
			final Type type ) {
		final LocationProposition proposition = new LocationProposition( proposer, proposed, facility, type );
		return CACHE ? cache.getOrPut( proposition , proposition ) : proposition;
	}

	@Override
	public Person getProposer() {
		return proposer;
	}

	@Override
	public Collection<Person> getProposed() {
		return proposed;
	}

	public ActivityFacility getFacility() {
		return facility;
	}

	public Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		int hash = proposer.hashCode();

		hash *= 37;
		hash += proposed.hashCode();

		hash *= 37;
		hash += facility.hashCode();

		return hash;
	}

	@Override
	public boolean equals( final Object obj ) {
		return obj instanceof LocationProposition &&
				( (LocationProposition) obj ).facility.equals( facility ) &&
				( (LocationProposition) obj ).proposed.equals( proposed ) &&
				( (LocationProposition) obj ).proposer.equals( proposer );
	}

	@Override
	public String toString() {
		return "LocationProposition{" +
				"proposer=" + proposer +
				", proposed=" + proposed +
				", facility=" + facility +
				", type=" + type +
				'}';
	}
}
