/* *********************************************************************** *
 * project: org.matsim.*
 * DriverRoutingModule.java
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
package org.matsim.contrib.socnetsim.jointtrips.router;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;

/**
 * @author thibautd
 */
public class DriverRoutingModule implements RoutingModule {
	private final RoutingModule carRoutingModule;
	private final String mode;
	private final PopulationFactory popFactory;

	public DriverRoutingModule(
			final String mode,
			final PopulationFactory popFactory,
			final RoutingModule carRoutingModule) {
		this.mode = mode;
		this.popFactory = popFactory;
		this.carRoutingModule = carRoutingModule;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility, 
			final double departureTime,
			final Person person) {
		List<? extends PlanElement> trip =
			carRoutingModule.calcRoute(
					fromFacility,
					toFacility, 
					departureTime,
					person);

		if (trip.size() != 1) {
			throw new RuntimeException( "unexpected trip size for trip "+trip+" for mode "+mode );
		}

		Leg carLeg = (Leg) trip.get( 0 );
		NetworkRoute netRoute = (NetworkRoute) carLeg.getRoute();

		Leg leg = popFactory.createLeg( mode );
		DriverRoute dRoute = new DriverRoute( netRoute , Collections.<Id<Person>>emptyList() );
		leg.setRoute( dRoute );
		leg.setDepartureTime( departureTime );
		leg.setTravelTime( dRoute.getTravelTime() );

		return Collections.singletonList( leg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
}

