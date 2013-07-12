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

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;

/**
 * Design thoughts:<ul>
 * <li>yyyy The module includes walk times and walk distances.  But rather than putting them into a separate leg, they are
 * included "silently" in the pt leg, which thus pretends to go door to door. kai, jul'13
 * </ul>
 * 
 * @author thomas
 *
 */
public class MatrixBasedPtRoutingModule implements RoutingModule{
	
	// 1) read a file with every transit stop
	// 2) read a file with travel times and general costs from every stop to every other stop ("matrix")
	// 3) route:
	//    - find the nearest transit stop to the coordinates of the previous activity (via QuadTree)
	//    - find the nearest transit stop to the coordinates of the next activity (via QuadTree)
	//    - determine the time and generalized cost of the trip (walk + "matrix" entry + walk)
	// 4) return that route as pseudo transit, i.e. such that matsim executes this as teleportation

	private PopulationFactoryImpl populationFactory;
	private GenericRouteFactory genericRouteFactory;
	private NetworkImpl network;
	private PtMatrix ptMatrix;
	
	public MatrixBasedPtRoutingModule(final Controler controler, PtMatrix ptMatrix){
		this.populationFactory = new PopulationFactoryImpl(controler.getScenario());
		this.genericRouteFactory = new GenericRouteFactory();
		this.network = (NetworkImpl) controler.getScenario().getNetwork();
		this.ptMatrix= ptMatrix;
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person){

		// create new leg with mode pt
		Leg newLeg = populationFactory.createLeg( TransportMode.pt );
		
		// set generic route for teleportation
		Id startLinkId = network.getNearestLinkExactly(fromFacility.getCoord()).getId();
		Id endLinkId = network.getNearestLinkExactly(toFacility.getCoord()).getId();
		final Route route = genericRouteFactory.createRoute(startLinkId, endLinkId);

		// set departure time
		newLeg.setDepartureTime( departureTime );

		// set travel time
		double travelTime = this.ptMatrix.getTotalTravelTime_seconds(fromFacility.getCoord(), toFacility.getCoord());
		newLeg.setTravelTime( travelTime );
		// set arrival time
//		newLeg.setArrivalTime( departureTime + travelTime );  // should not be necessary. kai, jul'12
		
		// set distance
		double distance = this.ptMatrix.getTotalTravelDistance_meter(fromFacility.getCoord(), toFacility.getCoord()) ;
		route.setDistance(distance) ;

		newLeg.setRoute(route);
		
		// done
		return Arrays.asList( newLeg );
	}
	
	@Override
	public StageActivityTypes getStageActivityTypes(){
		return EmptyStageActivityTypes.INSTANCE;
	}
}
