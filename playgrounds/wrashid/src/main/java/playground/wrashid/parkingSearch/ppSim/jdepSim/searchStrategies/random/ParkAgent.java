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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;

public class ParkAgent extends RandomParkingSearch {

	double startStrategyAtDistanceFromDestination = 500;

	HashMap<Id, ParkAgentAttributes> attributes;

	public ParkAgent(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType = "streetParking";
	}
	
	public void resetForNewIteration() {
		super.resetForNewIteration();
		attributes = new HashMap<Id, ParkAgent.ParkAgentAttributes>();
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 3);
		
		if (!attributes.containsKey(personId) && GeneralLib.getDistance(getCurrentLink(aem).getCoord(), network.getLinks().get(nextAct.getLinkId()).getCoord())<startStrategyAtDistanceFromDestination){
			Id parkingLinkId = getParkingLinkId(aem, getParkingFilterType(personId));
			ParkAgentAttributes parkingAttr = new ParkAgentAttributes();
			if (parkingLinkId==null || !parkingLinkId.toString().contains("privateParkings")){
				parkingAttr.takePrivateParking=false;
			} else {
				parkingAttr.takePrivateParking=true;
			}
			
			attributes.put(personId, parkingAttr);
			super.handleAgentLeg(aem);
		}else if (attributes.containsKey(personId)) {
			ParkAgentAttributes parkingAttr = attributes.get(personId);
			
			if (parkingAttr.takePrivateParking){
				super.handleAgentLeg(aem);
			} else {
				Id freeParkingFacilityOnLink = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(getNextLink(aem).getId(), "streetParking");
				boolean isInvalidLink = aem.isInvalidLinkForParking();
				
				if (!isInvalidLink && freeParkingFacilityOnLink!=null && random.nextDouble()<0.1){
					throughAwayRestOfRoute(aem);
					parkVehicle(aem, freeParkingFacilityOnLink);
				} else {
					super.handleAgentLeg(aem);
				}
			}
		}else {
			super.handleAgentLeg(aem);
		}
	}

	private static class ParkAgentAttributes {
		boolean takePrivateParking = true;
	}

}
