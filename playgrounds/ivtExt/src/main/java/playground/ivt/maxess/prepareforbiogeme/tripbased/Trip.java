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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author thibautd
 */
public class Trip {
	private static final Logger log = Logger.getLogger( Trip.class );
	private final ActivityFacility origin;
	private final ActivityFacility destination;
	private final List<? extends PlanElement> trip;

	public Trip(
			final ActivityFacility origin,
			final List<? extends PlanElement> trip,
			final ActivityFacility destination ) {
		if ( origin == null || destination == null ) {
			throw new IllegalArgumentException( "null facility in OD "+origin+" - "+destination );
		}
		this.origin = origin;
		this.destination = destination;
		this.trip = trip;

		if ( log.isTraceEnabled() ) log.trace( "Created "+this );
	}

	public ActivityFacility getOrigin() {
		return origin;
	}

	public ActivityFacility getDestination() {
		return destination;
	}

	public List<? extends PlanElement> getTrip() {
		return Collections.unmodifiableList( trip );
	}

	public List<Leg> getLegsOnly() {
		final List<Leg> legs= new ArrayList<>();
		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) legs.add( (Leg) pe );
		}
		return legs;
	}

	@Override
	public String toString() {
		return "Trip{" +
				"origin=" + origin +
				", destination=" + destination +
				", trip=" + trip +
				'}';
	}
}
