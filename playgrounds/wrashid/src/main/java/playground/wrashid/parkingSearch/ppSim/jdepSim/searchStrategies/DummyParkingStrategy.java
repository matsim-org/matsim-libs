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

// usable e.g. for generating events file of scenario without search traffic
public class DummyParkingStrategy implements ParkingSearchStrategy {

	public DummyParkingStrategy() {
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

			Id closestFreeParkingFacilityId = AgentWithParking.parkingManager.getClosestFreeParkingFacilityId(nextAct.getLinkId());
			AgentWithParking.parkingManager.parkVehicle(personId, closestFreeParkingFacilityId);
		}
		aem.processLegInDefaultWay();
	}

	@Override
	public String getName() {
		return "DummyParkingStrategy";
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
