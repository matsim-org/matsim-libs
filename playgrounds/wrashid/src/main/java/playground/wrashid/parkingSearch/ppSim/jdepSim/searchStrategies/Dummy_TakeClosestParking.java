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

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

public class Dummy_TakeClosestParking extends RandomParkingSearch {
	private HashSet<Id> parkingFound;

	public Dummy_TakeClosestParking(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
	}
	
	public void resetForNewIteration() {
		super.resetForNewIteration();
		parkingFound=new HashSet<Id>();
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		
		boolean endOfLegReached = aem.endOfLegReached();

		if (endOfLegReached) {
		if (!parkingFound.contains(personId)) {
			parkingFound.add(personId);

			ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextAct.getFacilityId(),
					nextAct.getType());
			
			if (parkingId == null) {
				parkingId = AgentWithParking.parkingManager.getClosestFreeParkingFacilityId(nextAct.getLinkId());
			}
			
			parkVehicle(aem, parkingId);
		}} else {
			super.handleAgentLeg(aem);
		}
		
	}

}
