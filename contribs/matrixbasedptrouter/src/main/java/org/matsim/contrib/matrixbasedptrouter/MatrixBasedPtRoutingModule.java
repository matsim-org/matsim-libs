/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

/**
 * 
 */
package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Design thoughts:<ul>
 * <li>yyyy The module includes walk times and walk distances.  But rather than putting them into a separate leg, they are
 * included "silently" in the pt leg, which thus pretends to go door to door. kai, jul'13
 * </ul>
 * 
 * @author thomas
 *
 */
public final class MatrixBasedPtRoutingModule implements RoutingModule {
	
	// 1) read a file with every transit stop
	// 2) read a file with travel times and general costs from every stop to every other stop ("matrix")
	// 3) route:
	//    - find the nearest transit stop to the coordinates of the previous activity (via QuadTree)
	//    - find the nearest transit stop to the coordinates of the next activity (via QuadTree)
	//    - determine the time and generalized cost of the trip (walk + "matrix" entry + walk)
	// 4) return that route as pseudo transit, i.e. such that matsim executes this as teleportation

	private final GenericRouteFactory genericRouteFactory;
	private final PtMatrix ptMatrix;
	private final Scenario scenario;

	@Inject
	public MatrixBasedPtRoutingModule(Scenario scenario, PtMatrix ptMatrix) {
		this.scenario = scenario;
		this.genericRouteFactory = new GenericRouteFactory();
		this.ptMatrix = ptMatrix;
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		Leg newLeg = scenario.getPopulation().getFactory().createLeg( TransportMode.pt );
		Id<Link> startLinkId = fromFacility.getLinkId();
		Id<Link> endLinkId = toFacility.getLinkId();
		newLeg.setDepartureTime( departureTime );
		double travelTime = this.ptMatrix.getTotalTravelTime_seconds(fromFacility.getCoord(), toFacility.getCoord());
		newLeg.setTravelTime( travelTime );
		
		final Route route = genericRouteFactory.createRoute(startLinkId, endLinkId);
		double distance = this.ptMatrix.getTotalTravelDistance_meter(fromFacility.getCoord(), toFacility.getCoord()) ;
		route.setDistance(distance) ;
		newLeg.setRoute(route);
		
		return Arrays.asList( newLeg );
	}
	
	@Override
	public StageActivityTypes getStageActivityTypes(){
		return EmptyStageActivityTypes.INSTANCE;
	}
}
