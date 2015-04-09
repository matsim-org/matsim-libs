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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomNumbers;

public class Dummy_RandomSelection extends Dummy_TakeClosestParking {

	public Dummy_RandomSelection(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
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
			
			if (isInvalidParking(aem, parkingId)) {
				double distance=300;
				Collection<PParking> parkings = AgentWithParking.parkingManager.getParkingWithinDistance(nextAct.getCoord(),1000);
				removeInvalidParkings(aem, parkings);
				
				while (parkings.size()==0){
					distance*=2;
					parkings = AgentWithParking.parkingManager.getParkingWithinDistance(nextAct.getCoord(),distance);
				
					removeInvalidParkings(aem, parkings);
				}
				
				random = RandomNumbers.getRandomNumber(personId, aem.getPlanElementIndex(), getName());
				int randomInt = random.nextInt(parkings.size());
				int i=0;
				for (PParking parking:parkings){
					if (i==randomInt){
						parkingId=parking.getId();
						break;
					}
					i++;
				}
			}
			
			parkVehicleAndLogSearchTime(aem, personId, parkingId);
		}} else {
			super.handleAgentLeg(aem);
		}
		
	}

	public static void removeInvalidParkings(AgentWithParking aem, Collection<PParking> parkings) {
		LinkedList<PParking> removeList=new LinkedList<PParking>();
		removePrivateParkings(parkings);
		for (PParking parking:parkings){
			if (isInvalidParking(aem, parking.getId())) {
				removeList.add(parking);
			}
		}
		parkings.removeAll(removeList);
	}
	
	public static void removePrivateParkings(Collection<PParking> parkings){
		LinkedList<PParking> removeList=new LinkedList<PParking>();
		
		for (PParking parking:parkings){
			if (parking.getType().equalsIgnoreCase("private")){
				removeList.add(parking);
			}
		}
		
		parkings.removeAll(removeList);
	}


}

