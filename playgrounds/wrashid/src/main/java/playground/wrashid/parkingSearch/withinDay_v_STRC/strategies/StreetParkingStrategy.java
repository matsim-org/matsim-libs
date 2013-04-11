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

import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;

public class StreetParkingStrategy extends GarageParkingStrategy{

	public StreetParkingStrategy(ParkingInfrastructure_v2 parkingInfrastructure, ScenarioImpl scenarioData) {
		super(parkingInfrastructure, scenarioData);
	}

	@Override
	public boolean acceptParking(PlanBasedWithinDayAgent agent, Id facilityId) {
			String parkingType = parkingInfrastructure.getParkingTypes().get(facilityId);
			
			if (parkingType.equalsIgnoreCase("streetParking")){
				return true;
			} else {
				return false;
			}
	}

	

}

