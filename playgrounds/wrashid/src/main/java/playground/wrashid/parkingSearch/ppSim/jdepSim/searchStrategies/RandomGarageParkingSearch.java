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
	
	
	
	private int delayBeforeSwitchToStreetParkingSearch;

	public RandomGarageParkingSearch(double maxDistance, Network network,int delayBeforeSwitchToStreetParkingSearch, String name) {
		super(maxDistance, network,name);
		this.delayBeforeSwitchToStreetParkingSearch = delayBeforeSwitchToStreetParkingSearch;
		this.parkingType="garageParking";
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		super.handleAgentLeg(aem);
		Id personId = aem.getPerson().getId();
		if (startSearchTime.containsKey(personId)){
			double searchDuration=getSearchTime(aem);
			
			if (searchDuration>delayBeforeSwitchToStreetParkingSearch){
				useSpecifiedParkingType.put(personId, "streetParking");
			}
		}
	}
	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

}

