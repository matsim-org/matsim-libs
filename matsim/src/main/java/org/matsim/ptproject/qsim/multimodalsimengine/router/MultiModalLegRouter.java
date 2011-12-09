/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalLegHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.multimodalsimengine.router;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTime;

public class MultiModalLegRouter implements LegRouter {
	
	private Set<String> walkModeRestrictions;
	private Set<String> bikeModeRestrictions;
	private Set<String> rideModeRestrictions;
	private Set<String> ptModeRestrictions;
	
	private final IntermodalLeastCostPathCalculator routeAlgo;
	private final ModeRouteFactory modeRouteFactory;
	private final RouteFactory routeFactory;
	
	private final MultiModalTravelTime travelTime;
	private final PersonalizableTravelCost travelCost;

	private final LegRouter legRouter;
	
	public MultiModalLegRouter(final Network network, final MultiModalTravelTime travelTime, 
			final PersonalizableTravelCost travelCost, final IntermodalLeastCostPathCalculator routeAlgo) {
		
		this.travelTime = travelTime;
		this.travelCost = travelCost;
		this.routeAlgo = routeAlgo;
		
		this.routeFactory = new LinkNetworkRouteFactory();
		this.modeRouteFactory = new ModeRouteFactory();
		modeRouteFactory.setRouteFactory(TransportMode.car, routeFactory);
		modeRouteFactory.setRouteFactory(TransportMode.walk, routeFactory);
		modeRouteFactory.setRouteFactory(TransportMode.bike, routeFactory);
		modeRouteFactory.setRouteFactory(TransportMode.pt, routeFactory);
		modeRouteFactory.setRouteFactory(TransportMode.ride, routeFactory);
		
		this.legRouter = new NetworkLegRouter(network, this.routeAlgo, this.modeRouteFactory);
		
		initModeRestrictions();
	}
	
	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		String legMode = leg.getMode();
		
		if (TransportMode.car.equals(legMode)) {
			// nothing to do
		} else if (TransportMode.walk.equals(legMode)) {
			routeAlgo.setModeRestriction(walkModeRestrictions);
		} else if (TransportMode.bike.equals(legMode)) {
			routeAlgo.setModeRestriction(bikeModeRestrictions);
		} else if (TransportMode.pt.equals(legMode)) {
			routeAlgo.setModeRestriction(ptModeRestrictions);
		} else if (TransportMode.ride.equals(legMode)) {
			routeAlgo.setModeRestriction(rideModeRestrictions);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legMode + "'.");
		}
		
		// set Person in TravelTime and TravelCost
		travelTime.setPerson(person);
		travelCost.setPerson(person);
		
		// set transport mode in TravelTime
		travelTime.setTransportMode(legMode);
		
		return legRouter.routeLeg(person, leg, fromAct, toAct, depTime);
	}


	private void initModeRestrictions() {	
		/*
		 * Walk
		 */	
		walkModeRestrictions = new HashSet<String>();
		walkModeRestrictions.add(TransportMode.walk);
		walkModeRestrictions.add(TransportMode.car);
				
		/*
		 * Bike
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		bikeModeRestrictions = new HashSet<String>();
		bikeModeRestrictions.add(TransportMode.bike);
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.car);
	
		
		/*
		 * PT
		 * If possible, we use "real" TravelTimes from previous iterations - if not,
		 * freeSpeedTravelTimes are used.
		 */
//		PersonalizableTravelCost ptTravelCost = costFactory.createTravelCostCalculator(travelTime, cnScoringGroup);
//		if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
//			BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
//			ptTravelCost.setTravelTime(bufferedTravelTime);
//		}
		
		
		/*
		 * We assume PT trips are possible on every road that can be used by cars.
		 * 
		 * Additionally we also allow pt trips to use walk and / or bike only links.
		 * On those links the traveltimes are quite high and we can assume that they
		 * are only use e.g. to walk from the origin to the bus station or from the
		 * bus station to the destination.
		 */
		ptModeRestrictions = new HashSet<String>();
		ptModeRestrictions.add(TransportMode.car);
		ptModeRestrictions.add(TransportMode.bike);
		ptModeRestrictions.add(TransportMode.walk);
		
		
		/*
		 * Ride
		 * If possible, we use "real" TravelTimes from previous iterations - if not,
		 * freeSpeedTravelTimes are used.
		 */
//		PersonalizableTravelCost rideTravelCost = costFactory.createTravelCostCalculator(travelTime, cnScoringGroup);
//		if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
//			BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
//			rideTravelCost.setTravelTime(bufferedTravelTime);
//		}
		
		/*
		 * We assume ride trips are possible on every road that can be used by cars.
		 */
		rideModeRestrictions = new HashSet<String>();
		rideModeRestrictions.add(TransportMode.car);
	}

}
