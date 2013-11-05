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
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

public class Dummy_TakeClosestParking implements ParkingSearchStrategy {

	public Dummy_TakeClosestParking() {
		resetForNewIteration();
	}

	HashSet<Id> parkingFound;

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		if (!parkingFound.contains(personId)) {
			parkingFound.add(personId);

			ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextAct.getFacilityId(),
					nextAct.getType());
			
			if (parkingId == null) {
				parkingId = AgentWithParking.parkingManager.getClosestFreeParkingFacilityId(nextAct.getLinkId());
			}
			AgentWithParking.parkingManager.parkVehicle(personId, parkingId);
		}
		aem.processLegInDefaultWay();
	}

	@Override
	public String getName() {
		return "Dummy_TakeClosestParking";
	}

	@Override
	public String getGroupName() {
		return "GroupName";
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		parkingFound.remove(personId);
	}

	@Override
	public void resetForNewIteration() {
		parkingFound = new HashSet<Id>();
	}

	@Override
	public void tollAreaEntered(AgentWithParking aem) {

	}

}
