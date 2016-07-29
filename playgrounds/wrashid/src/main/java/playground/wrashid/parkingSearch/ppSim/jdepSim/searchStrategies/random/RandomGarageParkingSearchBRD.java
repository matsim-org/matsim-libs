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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomGarageParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;

public class RandomGarageParkingSearchBRD  extends RandomGarageParkingSearch  {

	private double distanceToDestinationForStartingRandomSearch;

	public RandomGarageParkingSearchBRD(double maxDistance, Network network, String name,
			double distanceToDestinationForStartingRandomSearch, int delayBeforeSwitchToStreetParkingSearch) {
		super(maxDistance, network,  delayBeforeSwitchToStreetParkingSearch,name);
		this.distanceToDestinationForStartingRandomSearch = distanceToDestinationForStartingRandomSearch;
		this.parkingType = "garageParking";
	}

	
	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Activity nextAct = (Activity) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);
		
		if (GeneralLib.getDistance(getCurrentLink(aem).getCoord(), network.getLinks().get(nextAct.getLinkId()).getCoord())<distanceToDestinationForStartingRandomSearch){
			throughAwayRestOfRoute(aem);
		}
		super.handleAgentLeg(aem);
	}

}

