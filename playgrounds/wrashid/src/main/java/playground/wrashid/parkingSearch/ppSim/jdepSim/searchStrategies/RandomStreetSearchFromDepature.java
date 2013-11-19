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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

public class RandomStreetSearchFromDepature extends RandomParkingSearch {

	public RandomStreetSearchFromDepature(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType="streetParking";
		setSearchBeta(-1.0);
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		throughAwayRestOfRoute(aem);
		super.handleAgentLeg(aem);
		
	}

}
