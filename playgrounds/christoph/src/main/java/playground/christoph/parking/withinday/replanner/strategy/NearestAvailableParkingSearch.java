/* *********************************************************************** *
 * project: org.matsim.*
 * NearestAvailableParkingSearch.java
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

package playground.christoph.parking.withinday.replanner.strategy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingRouter;

public class NearestAvailableParkingSearch implements ParkingSearchStrategy {

	private static final Logger log = Logger.getLogger(NearestAvailableParkingSearch.class);
	
	private final Network network;
	private final ParkingRouter parkingRouter;
	private final ParkingInfrastructure parkingInfrastructure;
	
	public NearestAvailableParkingSearch(Network network, ParkingRouter parkingRouter, ParkingInfrastructure parkingInfrastructure) {
		this.network = network;
		this.parkingRouter = parkingRouter;
		this.parkingInfrastructure = parkingInfrastructure;
	}
	
	@Override
	public void applySearchStrategy(PlanBasedWithinDayAgent agent, double time) {
		
		Id currentLinkId = agent.getCurrentLinkId();
		Link currentLink = this.network.getLinks().get(currentLinkId);
		
		ActivityFacility parkingFacility = this.parkingInfrastructure.getClosestFreeParkingFacility(currentLink.getCoord());
		Id endLinkId = parkingFacility.getLinkId();
		
		Leg leg = agent.getCurrentLeg();
		NetworkRoute route = (NetworkRoute) leg.getRoute();	
		
		Person person = agent.getSelectedPlan().getPerson();
		Vehicle vehicle = null;
		parkingRouter.extendRoute(route, endLinkId, time, person, vehicle, TransportMode.car);
	}
}