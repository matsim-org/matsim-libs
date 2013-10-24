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
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class RandomStreetParkingWithIllegalParkingAndNoLawEnforcement extends RandomParkingSearch {

	public RandomStreetParkingWithIllegalParkingAndNoLawEnforcement(double maxDistance, Network network) {
		super(maxDistance, network);
		this.parkingType = "illegalParking";
	}

	@Override
	public String getName() {
		return "RandomStreet+IllegalParking+No law enforcement";
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());

		List<Id> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

		boolean endOfLegReached = aem.getCurrentLinkIndex() == linkIds.size() - 1;

		if (endOfLegReached) {

			ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
					.get(aem.getPlanElementIndex() + 3);

			Id parkingId = AgentWithParking.parkingManager.getFreePrivateParking(nextNonParkingAct.getFacilityId(),
					nextNonParkingAct.getType());

			if (parkingId == null) {
				parkingId = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(route.getEndLinkId(), "streetParking");
			}

			if (parkingId != null) {
				useSpecifiedParkingType.put(personId, "streetParking");
			} else {
				useSpecifiedParkingType.put(personId, "illegalParking");
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
