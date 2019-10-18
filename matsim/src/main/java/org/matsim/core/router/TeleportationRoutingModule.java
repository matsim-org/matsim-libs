/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;



/**
 * @author thibautd
 */
public class TeleportationRoutingModule implements RoutingModule {

	private final String mode;

	private final double beelineDistanceFactor;
	private final double networkTravelSpeed;
	private final Scenario scenario;

	public TeleportationRoutingModule(
			final String mode,
			final Scenario scenario,
			final double networkTravelSpeed,
			final double beelineDistanceFactor) {
		this.networkTravelSpeed = networkTravelSpeed;
		this.beelineDistanceFactor = beelineDistanceFactor;
		this.mode = mode;
		this.scenario = scenario ;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg newLeg = this.scenario.getPopulation().getFactory().createLeg( this.mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = routeLeg(
				person,
				newLeg,
				new FacilityWrapperActivity( fromFacility ),
				new FacilityWrapperActivity( toFacility ),
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList(newLeg);
	}

	@Override
	public String toString() {
		return "[TeleportationRoutingModule: mode="+this.mode+"]";
	}


	/* package */ double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		// make simple assumption about distance and walking speed


		final Coord fromActCoord = 	PopulationUtils.decideOnCoordForActivity( fromAct, scenario) ;
		Gbl.assertNotNull( fromActCoord );
		final Coord toActCoord = PopulationUtils.decideOnCoordForActivity( toAct, scenario ) ;
		Gbl.assertNotNull( toActCoord );
		double dist = CoordUtils.calcEuclideanDistance( fromActCoord, toActCoord );
		// create an empty route, but with realistic travel time
		Route route = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(Route.class, fromAct.getLinkId(), toAct.getLinkId());
		double estimatedNetworkDistance = dist * this.beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / this.networkTravelSpeed);
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		Leg r = (leg);
		r.setTravelTime( depTime + travTime - r.getDepartureTime() ); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
