/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.ffcs.parking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;
import playground.jbischoff.parking.choice.ParkingChoiceLogic;
import playground.jbischoff.parking.routing.ParkingRouter;

/**
 * @author jbischoff
 *
 */

public class FFCSorRandomParkingChoiceLogic implements ParkingChoiceLogic {

	private Network network;
	private final Random random = MatsimRandom.getLocalInstance();
	private FacilityBasedFreefloatingParkingManager pmanager;
	private FreefloatingCarsharingManager ffmanager;
	private final ParkingRouter pr; 
	private boolean lookedForRoute = false;
	private LinkNetworkRouteImpl route = null;
	private int routeIdx = 0;
	
	/**
	 * {@link Network} the network
	 */
	public FFCSorRandomParkingChoiceLogic(Network network, FacilityBasedFreefloatingParkingManager manager, FreefloatingCarsharingManager ffmanager, ParkingRouter parkingRouter) {
		
		this.network = network;
		this.pmanager = manager;
		this.ffmanager = ffmanager;
		this.pr = parkingRouter;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
	
		if (!ffmanager.isFFCSVehicle(vehicleId)){
				return getRandomLinkId(currentLinkId);
		}
		else {
			if (!lookedForRoute){
				ActivityFacility parking = pmanager.findNearestFreeCarsharingParkingFacility(currentLinkId);
				lookedForRoute = true;
				if (parking!=null){
					route = (LinkNetworkRouteImpl) pr.getRouteFromParkingToDestination(parking.getLinkId(), 0.0, currentLinkId);
				} 
			}
			if (route!=null){
				if (routeIdx<route.getLinkIds().size()){
				Id<Link> nextLink = route.getLinkIds().get(routeIdx);
				routeIdx++;
				return nextLink;
				}
				}
			
			return getRandomLinkId(currentLinkId);
			
			
		}

	}

	private Id<Link> getRandomLinkId(Id<Link> currentLinkId) {
		Link currentLink = network.getLinks().get(currentLinkId);
		List<Id<Link>> keys = new ArrayList<>(currentLink.getToNode().getOutLinks().keySet());
		Id<Link> randomKey = keys.get(random.nextInt(keys.size()));
		return randomKey;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.choice.ParkingChoiceLogic#reset()
	 */
	@Override
	public void reset() {

		lookedForRoute=false;
		routeIdx=0;
		route = null;
	}
}
