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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentEventMessage;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

// wait on first link if needed, afterwards random search.
public class RandomStreetParkingSearchWithWaiting extends RandomParkingSearch {

	private double maxWaitingTime;
	private double availabilityCheckIntervall;

	public RandomStreetParkingSearchWithWaiting(double maxDistance, Network network, double maxWaitingTime,
			double availabilityCheckIntervall) {
		super(maxDistance, network);
		this.maxWaitingTime = maxWaitingTime;
		this.availabilityCheckIntervall = availabilityCheckIntervall;
		this.parkingType = "streetParking";
	}

	@Override
	public String getName() {
		return "RandomStreetSearch+WithWaiting";
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

		List<Id> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

		boolean endOfLegReached = aem.getCurrentLinkIndex() == linkIds.size() - 1;

		if (endOfLegReached) {

			if (!startSearchTime.containsKey(personId)) {
				startSearchTime.put(personId, aem.getMessageArrivalTime());
			}

			ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextNonParkingAct.getFacilityId(),
					nextNonParkingAct.getType());

			if (parkingId == null) {
				parkingId = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(route.getEndLinkId(), "streetParking");
			}

			double waitingTime = GeneralLib.getIntervalDuration(startSearchTime.get(personId), aem.getMessageArrivalTime());
			if (startSearchTime.get(personId)==aem.getMessageArrivalTime()){
				waitingTime=0;
			}
			
			if (parkingId != null || waitingTime > maxWaitingTime) {
				super.handleAgentLeg(aem);
			} else {
				aem.setMessageArrivalTime(aem.getMessageArrivalTime() + availabilityCheckIntervall);
				AgentEventMessage.getMessageQueue().schedule(aem);
			}
		}else {
			super.handleAgentLeg(aem);
		}

	}

}
