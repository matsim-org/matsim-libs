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

public class RandomStreetParkingWithIllegalParkingAndLawEnforcement extends RandomStreetParkingWithIllegalParkingAndNoLawEnforcement {

	public RandomStreetParkingWithIllegalParkingAndLawEnforcement(double maxDistance, Network network, String name) {
		super(maxDistance, network,name);
		this.parkingType = "illegalParking";
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		addIllegalParkingScore(aem);

		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

	private void addIllegalParkingScore(AgentWithParking aem) {
		//if (ZHScenarioGlobal.iteration > 100) {
			ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
			
			double parkingDuration = GeneralLib.getIntervalDuration(parkingAttributesForScoring.getParkingArrivalTime(), aem.getMessageArrivalTime());
			if (parkingAttributesForScoring.getParkingArrivalTime()==aem.getMessageArrivalTime()){
				parkingDuration=0;
			}
			
			double disutilityPerSecond=-400.0/(24*60*60);
			scoreInterrupationValue += disutilityPerSecond*parkingDuration;
		//}
	}

	@Override
	public void handleLastParkingScore(AgentWithParking aem) {
		addIllegalParkingScore(aem);

		super.handleLastParkingScore(aem);
	}

}
