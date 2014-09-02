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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentEventMessage;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

// only accept private parking, thereafter wait (penalty), thereafter normal street parking search
public class WaitAndRandomSearchAsBackup extends RandomParkingSearch{
	
	
	private double delayBeforeSwitchToStreetParkingSearch;

	public WaitAndRandomSearchAsBackup(double maxDistance, Network network, double delayBeforeSwitchToStreetParkingSearch, String name) {
		super(maxDistance, network,name);
		this.delayBeforeSwitchToStreetParkingSearch = delayBeforeSwitchToStreetParkingSearch;
		this.parkingType="streetParking";
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

		boolean endOfLegReached = aem.getCurrentLinkIndex() == linkIds.size() - 1;

		if (endOfLegReached) {

			if (!startSearchTime.containsKey(personId)) {
				startSearchTime.put(personId, aem.getMessageArrivalTime());
			}

			ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextNonParkingAct.getFacilityId(),
					nextNonParkingAct.getType());

			double waitingTime = getSearchTime(aem);
			
			if (parkingId != null || waitingTime >= delayBeforeSwitchToStreetParkingSearch) {
				super.handleAgentLeg(aem);
			} else {
				aem.setMessageArrivalTime(aem.getMessageArrivalTime() + delayBeforeSwitchToStreetParkingSearch);
				AgentEventMessage.getMessageQueue().schedule(aem);
			}
		}else {
			super.handleAgentLeg(aem);
		}

	}

}

