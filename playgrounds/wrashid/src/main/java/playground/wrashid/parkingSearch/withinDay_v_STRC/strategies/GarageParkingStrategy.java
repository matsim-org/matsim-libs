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
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.scenario.ScenarioImpl;

import playground.christoph.parking.withinday.replanner.strategy.RandomParkingSearch;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;

public class GarageParkingStrategy implements FullParkingSearchStrategy{

	protected ParkingInfrastructure_v2 parkingInfrastructure;
	private RandomParkingSearch randomParkingSearch;

	public GarageParkingStrategy(ParkingInfrastructure_v2 parkingInfrastructure, ScenarioImpl scenarioData){
		this.parkingInfrastructure = parkingInfrastructure;
		
		randomParkingSearch = new RandomParkingSearch(scenarioData.getNetwork());
	}

	@Override
	public void applySearchStrategy(PlanBasedWithinDayAgent agent, double time) {
		randomParkingSearch.applySearchStrategy(agent, time);
	}

	@Override
	public boolean acceptParking(PlanBasedWithinDayAgent agent, Id facilityId) {
		String parkingType = parkingInfrastructure.getParkingTypes().get(facilityId);
		
		if (parkingType.equalsIgnoreCase("garageParking")){
			return true;
		} else {
			return false;
		}
	}

}

