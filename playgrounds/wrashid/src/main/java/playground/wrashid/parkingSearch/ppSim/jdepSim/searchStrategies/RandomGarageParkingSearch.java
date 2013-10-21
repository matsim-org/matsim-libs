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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

public class RandomGarageParkingSearch extends RandomParkingSearch{
	
	
	
	public RandomGarageParkingSearch(double maxDistance, Network network) {
		super(maxDistance, network);
		this.parkingType="garageParking";
	}

	@Override
	public String getName() {
		return "RandomGarageParkingSearch";
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		super.handleAgentLeg(aem);
		Id personId = aem.getPerson().getId();
		if (startSearchTime.containsKey(personId)){
			double searchDuration=GeneralLib.getIntervalDuration(startSearchTime.get(personId),aem.getMessageArrivalTime());
			
			// allow parking at street if no garage parking found after 5 min
			if (searchDuration>5*60){
				this.parkingType="streetParking";
			}
			
		}
	}

}

