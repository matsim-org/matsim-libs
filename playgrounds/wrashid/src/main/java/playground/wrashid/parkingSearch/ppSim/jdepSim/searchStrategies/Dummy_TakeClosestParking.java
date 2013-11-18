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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class Dummy_TakeClosestParking extends RandomParkingSearch {
	protected HashSet<Id> parkingFound;

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

			DebugLib.traceAgent(personId);
			
			ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextAct.getFacilityId(),
					nextAct.getType());
			
			if (isInvalidParking(aem, parkingId)) {
				parkingId =  AgentWithParking.parkingManager.getClosestParkingFacilityNotOnLink(nextAct.getCoord(),aem.getInvalidLinkForParking());
			}
			
			parkVehicleAndLogSearchTime(aem, personId, parkingId);
		}} else {
			super.handleAgentLeg(aem);
		}
		
	}

	public void parkVehicleAndLogSearchTime(AgentWithParking aem, Id personId, Id parkingId) {
		double searchTime = getSearchTime(aem, parkingId);
		triggerSeachTimeStart(personId, aem.getMessageArrivalTime()-searchTime);
		
		parkVehicle(aem, parkingId);
	}

	public static double getSearchTime(AgentWithParking aem, Id parkingId) {
		Link currentLink = aem.getCurrentLink();
		double travelTime = ZHScenarioGlobal.ttMatrix.getTravelTime(aem.getMessageArrivalTime(), currentLink.getId());
		double speed=currentLink.getLength()/travelTime;
		
		Id linkOfParking = AgentWithParking.parkingManager.getLinkOfParking(parkingId);
		double distance = GeneralLib.getDistance(ZHScenarioGlobal.scenario.getNetwork().getLinks().get(linkOfParking).getCoord(), currentLink.getCoord());
		
		double searchTime = distance/speed*ZHScenarioGlobal.loadDoubleParam("Dummy_SearchTimeFactor");
		return searchTime;
	}
	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		super.handleParkingDepartureActivity(aem);
		parkingFound.remove(aem.getPerson().getId());
	}

}
