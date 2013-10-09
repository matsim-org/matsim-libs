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

import java.util.Random;

import org.matsim.api.core.v01.population.Person;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;

public class IllegalParking implements ParkingSearchStrategy{
	
	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		aem.processLegInDefaultWay();
		
		Person person = aem.getPerson();
		
		Random rand=new Random();
		
		// TODO: add score only at end of search (store it locally during search)!
		
		if (aem.getPlanElementIndex() >1 && aem.getPlanElementIndex() % 2 == 0){
			AgentWithParking.parkingStrategyManager.updateScore(person.getId(), aem.getPlanElementIndex()-1, 3*rand.nextDouble());
		}
	}

	@Override
	public String getName() {
		return "IllegalParking";
	}

}

