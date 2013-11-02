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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomGarageParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingSearchWithWaiting;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy3;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy7;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.ParkAgent;

public class ParkingStrategyScenarios {

	public static LinkedList<ParkingSearchStrategy> getScenarioStrategies(Scenario scenario) {
		LinkedList<ParkingSearchStrategy> allStrategies = new LinkedList<ParkingSearchStrategy>();

		if (ZHScenarioGlobal.parkingStrategyScenarioId == 1) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.1.delayBeforeSwitchToStreetParkingSearch"),
					"RandomGarageParkingSearch"));
			// allStrategies.add(new
			// RandomStreetParkingWithIllegalParkingAndLawEnforcement(500.0,
			// scenario.getNetwork()));
			// allStrategies.add(new
			// RandomStreetParkingWithIllegalParkingAndNoLawEnforcement(500.0,
			// scenario.getNetwork()));
			allStrategies.add(new RandomStreetParkingSearchWithWaiting(-1, scenario.getNetwork(), ZHScenarioGlobal
					.loadDoubleParam("parkingStrategyScenarioId.1.maxWaitingTime"), ZHScenarioGlobal
					.loadDoubleParam("parkingStrategyScenarioId.1.availabilityCheckIntervall"),
					"RandomStreetParkingSearchWithWaiting"));
			// allStrategies.add(new
			// PrivateParkingWithWaitAndRandomSearchAsBackup(500.0,
			// scenario.getNetwork(),5*60));
			allStrategies.add(new AxPo1989_Strategy7(-1, scenario.getNetwork(), ZHScenarioGlobal
					.loadDoubleParam("parkingStrategyScenarioId.1.expectedIllegalParkingFeeForWholeDay"), "AxPo1989_Strategy7"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 2) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch"));
			allStrategies.add(new AxPo1989_Strategy3(-1, scenario.getNetwork(), "AxPo1989_Strategy3"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 3) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch"));
			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 4) {

			int randomSteetParkingSearchCount = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.4.numberOfRandomSteetParkingSearch");
			for (int i = 0; i < randomSteetParkingSearchCount; i++) {
				allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch" + i));
			}

			int randomGarageParkingSearchCount = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.4.numberOfRandomGarageParkingSearch");
			for (int i = 0; i < randomGarageParkingSearchCount; i++) {
				allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), ZHScenarioGlobal
						.loadIntParam("parkingStrategyScenarioId.4.delayBeforeSwitchToStreetParkingSearch"),
						"RandomGarageParkingSearch" + i));
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 5) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch1"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch2"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch3"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch4"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch5"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch6"));
		}

		return allStrategies;
	}
}
