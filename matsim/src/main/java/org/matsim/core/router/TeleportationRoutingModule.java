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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;



/**
 * @author thibautd
 * @author sebhoerl
 */
public class TeleportationRoutingModule implements RoutingModule {

	private final String mode;

	private final double beelineDistanceFactor;
	private final double networkTravelSpeed;
	private final String personSpeedAttribute;
	private final Scenario scenario;

	public TeleportationRoutingModule(
			final String mode,
			final Scenario scenario,
			final double networkTravelSpeed,
			final double beelineDistanceFactor,
			final String personSpeedAttribute) {
		this.networkTravelSpeed = networkTravelSpeed;
		this.beelineDistanceFactor = beelineDistanceFactor;
		this.personSpeedAttribute = personSpeedAttribute;
		this.mode = mode;
		this.scenario = scenario ;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();
		
		Leg newLeg = this.scenario.getPopulation().getFactory().createLeg( this.mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = routeLeg(
				person,
				newLeg,
				fromFacility,
				toFacility,
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList(newLeg);
	}

	@Override
	public String toString() {
		return "[TeleportationRoutingModule: mode="+this.mode+"]";
	}


	/* package */ double routeLeg( Person person, Leg leg, Facility fromFacility, Facility toFacility, double depTime ) {
		// yyyy there is a test that uses the above; todo: make that test use the official RoutingModule interface.  kai, nov'19

		// make simple assumption about distance and walking speed

		final Coord fromActCoord = 	FacilitiesUtils.decideOnCoord( fromFacility, scenario.getNetwork(), scenario.getConfig() ) ;
		Gbl.assertNotNull( fromActCoord );
		final Coord toActCoord = FacilitiesUtils.decideOnCoord( toFacility, scenario.getNetwork(), scenario.getConfig() ) ;
		Gbl.assertNotNull( toActCoord );
		double dist = CoordUtils.calcEuclideanDistance( fromActCoord, toActCoord );
		// create an empty route, but with realistic travel time

		Id<Link> fromFacilityLinkId = fromFacility.getLinkId();
		if ( fromFacilityLinkId==null ) {
			// (yyyy there is a test that does not have a context, and thus the call below fails.  todo: adapt test.  kai, nov'19)
			fromFacilityLinkId = FacilitiesUtils.decideOnLink( fromFacility, scenario.getNetwork() ).getId() ;
		}

		Id<Link> toFacilityLinkId = toFacility.getLinkId();
		if ( toFacilityLinkId==null ) {
			// (yyyy: same as above)
			toFacilityLinkId = FacilitiesUtils.decideOnLink( toFacility, scenario.getNetwork() ).getId() ;
		}

		Route route = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(Route.class, fromFacilityLinkId, toFacilityLinkId );
		double estimatedNetworkDistance = dist * this.beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / getTravelSpeed(person));
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		Leg r = (leg);
		r.setTravelTime( depTime + travTime - r.getDepartureTime().seconds()); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

	private double getTravelSpeed(Person person) {
		if (personSpeedAttribute != null) {
			Double personSpeed = (Double) person.getAttributes().getAttribute(personSpeedAttribute);
			
			if (personSpeed != null) {
				return personSpeed;
			}
		}

		return this.networkTravelSpeed;
	}

}
