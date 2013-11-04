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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomStreetParkingSearchBRD;


// street before reaching destination, then switch to garage parking then back to street (backup)
// need multiple strategies for generating good routes to garage parking close-by.
public class AxPo1989_Strategy2 extends RandomStreetParkingSearchBRD {

	public AxPo1989_Strategy2(double maxDistance, Network network, String name, double distanceToDestinationForStartingRandomSearch) {
		super(maxDistance, network, name, distanceToDestinationForStartingRandomSearch);
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (startSearchTime.containsKey(personId)){
			double searchDuration=getSearchTime(aem);
			
			if (searchDuration>5*60){
				useSpecifiedParkingType.put(personId, "garageParking");
			}
			
			if (searchDuration>15*60){
				useSpecifiedParkingType.put(personId, "streetParking");
			}
		}
		
		super.handleAgentLeg(aem);
	}
	
	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

}

