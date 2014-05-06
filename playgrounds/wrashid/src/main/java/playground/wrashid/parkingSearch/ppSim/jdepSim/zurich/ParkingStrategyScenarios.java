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

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.AvoidRoutingThroughTolledArea;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_BRD_TakeClosestGarageParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_OptimalScore;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_RandomSelection;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_ARD_TakeClosestGarageParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_TakeClosestParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.Dummy_WorstScore;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.WaitAndRandomSearchAsBackup;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomGarageParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetSearchFromDepature;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingSearchWithWaiting;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy2;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy3;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy7;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.ParkAgent;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomGarageParkingSearchBRD;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomStreetParkingSearchBRD;

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
			allStrategies.add(new AxPo1989_Strategy7(-1, scenario.getNetwork(), "AxPo1989_Strategy7"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 2) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch"));
			allStrategies.add(new AxPo1989_Strategy3(-1, scenario.getNetwork(), "AxPo1989_Strategy3"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 3) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch"));

			double startStrategyAtDistanceFromDestination = 500;
			double startParkingDecision = 250;
			int F1 = 1;
			int F2 = 3;
			double maxDistanceAcceptableForWalk = 400;
			double maxSeachDuration = 10 * 60;
			double increaseAcceptableDistanceInMetersPerMinute = 30;

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 4) {
			int delayBeforeSwitchToStreetParkingSearch = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.4.delayBeforeSwitchToStreetParkingSearch");
			int randomSteetParkingSearchCount = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.4.numberOfRandomSteetParkingSearch");
			for (int i = 0; i < randomSteetParkingSearchCount; i++) {
				RandomStreetParkingSearch strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-" + i);
				strategy.setGroupName("ARD-S");
				allStrategies.add(strategy);
			}

			int randomGarageParkingSearchCount = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.4.numberOfRandomGarageParkingSearch");
			for (int i = 0; i < randomGarageParkingSearchCount; i++) {
				RandomGarageParkingSearch strategy = new RandomGarageParkingSearch(-1, scenario.getNetwork(),
						delayBeforeSwitchToStreetParkingSearch, "ARD-G-" + i);
				strategy.setGroupName("ARD-G");
				allStrategies.add(strategy);
			}

			// allStrategies.add(new RandomGarageParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomGarageParkingSearchBRD", 100));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-1A", 100));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-1B", 100));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-2A", 200));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-2B", 200));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-3A", 300));
			// allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			// scenario.getNetwork(), "RandomStreetParkingSearchBRD-3B", 300));

			/*
			 * allStrategies.add(new RandomGarageParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomGarageParkingSearchBRD-2A", 200.0,
			 * delayBeforeSwitchToStreetParkingSearch));
			 * 
			 * 
			 * allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000A",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000B",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000C",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000D",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000E",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000F",
			 * 1000)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-1000G",
			 * 1000));
			 * 
			 * 
			 * allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500A",
			 * 500)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500B",
			 * 500)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500C",
			 * 500)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500D",
			 * 500)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500E",
			 * 500)); allStrategies.add(new RandomStreetParkingSearchBRD(-1,
			 * scenario.getNetwork(), "RandomStreetParkingSearchBRD-500F",
			 * 500));
			 */
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 5) {
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch1"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch2"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch3"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch4"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch5"));
			allStrategies.add(new RandomStreetParkingSearch(-1, scenario.getNetwork(), "RandomStreetParkingSearch6"));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 6) {
			double startStrategyAtDistanceFromDestination = 500;
			double startParkingDecision = 250;
			int F1 = 1;
			int F2 = 3;
			double maxDistanceAcceptableForWalk = 400;
			double maxSeachDuration = 10 * 60;
			double increaseAcceptableDistanceInMetersPerMinute = 30;

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent1", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent2", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent3", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent4", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent5", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 7) {
			double startStrategyAtDistanceFromDestination = 250;
			double startParkingDecision = 100;
			int F1 = 1;
			int F2 = 3;
			double maxDistanceAcceptableForWalk = 400;
			double maxSeachDuration = 10 * 60;
			double increaseAcceptableDistanceInMetersPerMinute = 30;

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent1", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 8) {
			double startStrategyAtDistanceFromDestination = 250;
			double startParkingDecision = 100;
			int F1 = 1;
			int F2 = 3;
			double maxDistanceAcceptableForWalk = 400;
			double maxSeachDuration = 10 * 60;
			double increaseAcceptableDistanceInMetersPerMinute = 30;

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent1", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent2", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent3", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent4", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent5", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new ParkAgent(-1, scenario.getNetwork(), "ParkAgent6", startStrategyAtDistanceFromDestination,
					startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
					increaseAcceptableDistanceInMetersPerMinute));

			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch1"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch2"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch3"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch4"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch5"));
			allStrategies.add(new RandomGarageParkingSearch(-1, scenario.getNetwork(), 10 * 60, "RandomGarageParkingSearch6"));

		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 9) {
			Dummy_RandomSelection dummy = new Dummy_RandomSelection(-1, scenario.getNetwork(), "Dummy_RandomSelection");
			dummy.setGroupName("dummyGroup");
			allStrategies.add(dummy);
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 10) {
			Dummy_TakeClosestParking dummy = new Dummy_TakeClosestParking(-1, scenario.getNetwork(), "Dummy_TakeClosestParking");
			dummy.setGroupName("dummyGroup");
			allStrategies.add(dummy);
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 11) {
			Dummy_OptimalScore dummy = new Dummy_OptimalScore(-1, scenario.getNetwork(), "Dummy_OptimalScore");
			dummy.setGroupName("dummyGroup");
			allStrategies.add(dummy);
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 12) {
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.12.numberOfStrategiesInEachGroup");

			int delayBeforeSwitchToStreetParkingSearch = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.12.delayBeforeSwitchToStreetParkingSearch");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				addScenario12Strategies(scenario, allStrategies, delayBeforeSwitchToStreetParkingSearch, i);
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 13) {
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.13.numberOfStrategiesInEachGroup");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				RandomParkingSearch strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-A" + i);
				strategy.setSearchBeta(-1.0);
				strategy.setRandomSearchDistance(100.0);
				strategy.setGroupName("ARD-S-1-100");
				allStrategies.add(strategy);

				strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-D" + i);
				strategy.setSearchBeta(1.0);
				strategy.setRandomSearchDistance(100.0);
				strategy.setGroupName("ARD-S-1000.0-100");
				allStrategies.add(strategy);
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 14) {
			RandomParkingSearch strategy = new Dummy_RandomSelection(-1, scenario.getNetwork(), "Dummy_RandomSelection");
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

			strategy = new Dummy_TakeClosestParking(-1, scenario.getNetwork(), "Dummy_TakeClosestParking");
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

			strategy = new Dummy_OptimalScore(-1, scenario.getNetwork(), "Dummy_OptimalScore");
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

			strategy = new Dummy_WorstScore(-1, scenario.getNetwork(), "Dummy_WorstScore");
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

			strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "Dummy_ARD_TakeClosestGarageParking");
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

			strategy = new Dummy_BRD_TakeClosestGarageParking(-1, scenario.getNetwork(), "Dummy_BRD_TakeClosestGarageParking",
					300.0);
			strategy.setGroupName("dummyGroup");
			allStrategies.add(strategy);

		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 15) {
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.15.numberOfStrategiesInEachGroup");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				addScenario15Strategies(scenario, allStrategies, i);
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 16) {
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.16.numberOfStrategiesInEachGroup");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				RandomParkingSearch strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-A" + i);
				strategy.setSearchBeta(-1.0);
				strategy.setRandomSearchDistance(100.0);
				strategy.setGroupName("ARD-S-1-100");
				allStrategies.add(strategy);

				strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-D" + i);
				strategy.setSearchBeta(1.0);
				strategy.setRandomSearchDistance(100.0);
				strategy.setGroupName("ARD-S-1000.0-100");
				allStrategies.add(strategy);
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 17) {
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.17.numberOfStrategiesInEachGroup");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				RandomParkingSearch strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-" + i);
				strategy.setGroupName("ARD-S");
				allStrategies.add(strategy);

				strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "ARD_TakeClosestGarageParking");
				strategy.setGroupName("ARD_TakeClosestGarageParking");
				allStrategies.add(strategy);
			}
		} else if (ZHScenarioGlobal.parkingStrategyScenarioId == 18){
			
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.18.numberOfStrategiesInEachGroup");

			int delayBeforeSwitchToStreetParkingSearch = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.18.delayBeforeSwitchToStreetParkingSearch");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				addScenario12Strategies(scenario, allStrategies, delayBeforeSwitchToStreetParkingSearch, i);
				
				RandomParkingSearch strategy =null;

				strategy = new RandomStreetParkingSearchBRD(-1, scenario.getNetwork(), "BRD(500m)-S-" + i, 500);
				strategy.setGroupName("BRD(500m)-S");
				allStrategies.add(strategy);

				strategy = new RandomStreetSearchFromDepature(-1, scenario.getNetwork(), "RandomStreetSearchFromDepature" + i);
				strategy.setGroupName("RandomStreetSearchFromDepature");
				allStrategies.add(strategy);

				strategy = new AvoidRoutingThroughTolledArea(-1, scenario.getNetwork(), "AvoidRoutingThroughTolledArea" + i);
				strategy.setGroupName("AvoidRoutingThroughTolledArea");
				allStrategies.add(strategy);
				
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 19){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.19.numberOfStrategiesInEachGroup");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				double startStrategyAtDistanceFromDestination = 250;
				double startParkingDecision = 100;
				int F1 = 1;
				int F2 = 3;
				double maxDistanceAcceptableForWalk = 400;
				double maxSeachDuration = 10 * 60;
				double increaseAcceptableDistanceInMetersPerMinute = 30;

				strategy = new ParkAgent(-1, scenario.getNetwork(), "Parkagent-" + i, startStrategyAtDistanceFromDestination,
						startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
						increaseAcceptableDistanceInMetersPerMinute);
				strategy.setGroupName("Parkagent");
				allStrategies.add(strategy);
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 20){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.20.numberOfStrategiesInEachGroup");
			
			int delayBeforeSwitchToStreetParkingSearch = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.20.delayBeforeSwitchToStreetParkingSearch");

			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				strategy = new RandomGarageParkingSearch(-1, scenario.getNetwork(), delayBeforeSwitchToStreetParkingSearch,
						"ARD-G-" + i);
				strategy.setGroupName("ARD-G");
				allStrategies.add(strategy);
				
				strategy = new RandomStreetParkingSearchBRD(-1, scenario.getNetwork(), "BRD(300m)-S-" + i, 300);
				strategy.setGroupName("BRD(300m)-S");
				allStrategies.add(strategy);
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 21){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.21.numberOfStrategiesInEachGroup");
			
			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				strategy = new RandomStreetParkingSearchBRD(-1, scenario.getNetwork(), "BRD(300m)-S-" + i, 300);
				strategy.setGroupName("BRD(300m)-S");
				allStrategies.add(strategy);
				
				strategy = new AxPo1989_Strategy2(-1, scenario.getNetwork(), "BRD(300m)-S-G-" + i, 300.0);
				strategy.setGroupName("BRD(300m)-S-G");
				allStrategies.add(strategy);
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 22){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.22.numberOfStrategiesInEachGroup");
			
			int delayBeforeSwitchToStreetParkingSearch = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.22.delayBeforeSwitchToStreetParkingSearch");
			
			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				strategy = new WaitAndRandomSearchAsBackup(-1, scenario.getNetwork(),
						delayBeforeSwitchToStreetParkingSearch, "ARD-waiting-S-" + i);
				strategy.setGroupName("ARD-waiting-S");
				allStrategies.add(strategy);
				
				strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "ARD-TakeClosestGarageParking-" + i);
				strategy.setGroupName("ARD-TakeClosestGarageParking");
				allStrategies.add(strategy);
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 23){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.23.numberOfStrategiesInEachGroup");
			
			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				strategy = new AxPo1989_Strategy2(-1, scenario.getNetwork(), "BRD(300m)-S-G-" + i, 300.0);
				strategy.setGroupName("BRD(300m)-S-G");
				allStrategies.add(strategy);
				
				strategy = new Dummy_BRD_TakeClosestGarageParking(-1, scenario.getNetwork(), "BRD-TakeClosestGarageParking-" + i,
						300.0);
				strategy.setGroupName("BRD-TakeClosestGarageParking");
				allStrategies.add(strategy);
			}
		}else if (ZHScenarioGlobal.parkingStrategyScenarioId == 24){
			int numberOfStrategiesInEachGroup = ZHScenarioGlobal
					.loadIntParam("parkingStrategyScenarioId.24.numberOfStrategiesInEachGroup");
			
			for (int i = 0; i < numberOfStrategiesInEachGroup; i++) {
				ParkingSearchStrategy strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
				strategy.setGroupName("ARD-illegal-S");
				allStrategies.add(strategy);
				
				strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "ARD-TakeClosestGarageParking-" + i);
				strategy.setGroupName("ARD-TakeClosestGarageParking");
				allStrategies.add(strategy);
				
				double startStrategyAtDistanceFromDestination = 250;
				double startParkingDecision = 100;
				int F1 = 1;
				int F2 = 3;
				double maxDistanceAcceptableForWalk = 400;
				double maxSeachDuration = 10 * 60;
				double increaseAcceptableDistanceInMetersPerMinute = 30;

				strategy = new ParkAgent(-1, scenario.getNetwork(), "Parkagent-" + i, startStrategyAtDistanceFromDestination,
						startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
						increaseAcceptableDistanceInMetersPerMinute);
				strategy.setGroupName("Parkagent");
				allStrategies.add(strategy);
			}
		}

		return allStrategies;
	}

	public static void addScenario15Strategies(Scenario scenario, LinkedList<ParkingSearchStrategy> allStrategies, int i) {
		RandomParkingSearch strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-" + i);
		strategy.setGroupName("ARD-S");
		allStrategies.add(strategy);

		strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "ARD_TakeClosestGarageParking-" + i);
		strategy.setGroupName("ARD_TakeClosestGarageParking");
		allStrategies.add(strategy);

		strategy = new Dummy_BRD_TakeClosestGarageParking(-1, scenario.getNetwork(), "BRD_TakeClosestGarageParking-" + i,
				500.0);
		strategy.setGroupName("BRD_TakeClosestGarageParking");
		allStrategies.add(strategy);

		strategy = new RandomStreetParkingSearchBRD(-1, scenario.getNetwork(), "BRD(500m)-S-" + i, 500);
		strategy.setGroupName("BRD(500m)-S");
		allStrategies.add(strategy);

		strategy = new RandomStreetSearchFromDepature(-1, scenario.getNetwork(), "RandomStreetSearchFromDepature" + i);
		strategy.setGroupName("RandomStreetSearchFromDepature");
		allStrategies.add(strategy);

		strategy = new AvoidRoutingThroughTolledArea(-1, scenario.getNetwork(), "AvoidRoutingThroughTolledArea" + i);
		strategy.setGroupName("AvoidRoutingThroughTolledArea");
		allStrategies.add(strategy);
	}

	public static void addScenario12Strategies(Scenario scenario, LinkedList<ParkingSearchStrategy> allStrategies,
			int delayBeforeSwitchToStreetParkingSearch, int i) {
		ParkingSearchStrategy strategy;
		strategy = new RandomGarageParkingSearch(-1, scenario.getNetwork(), delayBeforeSwitchToStreetParkingSearch,
				"ARD-G-" + i);
		strategy.setGroupName("ARD-G");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new RandomStreetParkingSearch(-1, scenario.getNetwork(), "ARD-S-" + i);
		strategy.setGroupName("ARD-S");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new RandomGarageParkingSearchBRD(-1, scenario.getNetwork(), "BRD(300m)-G-" + i, 300,
				delayBeforeSwitchToStreetParkingSearch);
		strategy.setGroupName("BRD(300m)-G");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new RandomStreetParkingSearchBRD(-1, scenario.getNetwork(), "BRD(300m)-S-" + i, 300);
		strategy.setGroupName("BRD(300m)-S");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new AxPo1989_Strategy2(-1, scenario.getNetwork(), "BRD(300m)-S-G-" + i, 300.0);
		strategy.setGroupName("BRD(300m)-S-G");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new AxPo1989_Strategy7(-1, scenario.getNetwork(), "ARD-illegal-S-" + i);
		strategy.setGroupName("ARD-illegal-S");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new WaitAndRandomSearchAsBackup(-1, scenario.getNetwork(),
				delayBeforeSwitchToStreetParkingSearch, "ARD-waiting-S-" + i);
		strategy.setGroupName("ARD-waiting-S");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new Dummy_ARD_TakeClosestGarageParking(-1, scenario.getNetwork(), "ARD-TakeClosestGarageParking-" + i);
		strategy.setGroupName("ARD-TakeClosestGarageParking");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		strategy = new Dummy_BRD_TakeClosestGarageParking(-1, scenario.getNetwork(), "BRD-TakeClosestGarageParking-" + i,
				300.0);
		strategy.setGroupName("BRD-TakeClosestGarageParking");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);

		double startStrategyAtDistanceFromDestination = 250;
		double startParkingDecision = 100;
		int F1 = 1;
		int F2 = 3;
		double maxDistanceAcceptableForWalk = 400;
		double maxSeachDuration = 10 * 60;
		double increaseAcceptableDistanceInMetersPerMinute = 30;

		strategy = new ParkAgent(-1, scenario.getNetwork(), "Parkagent-" + i, startStrategyAtDistanceFromDestination,
				startParkingDecision, F1, F2, maxDistanceAcceptableForWalk, maxSeachDuration,
				increaseAcceptableDistanceInMetersPerMinute);
		strategy.setGroupName("Parkagent");
		addStrategyAndSetLayerValues(allStrategies, strategy, i);
	}

	public static void addStrategyAndSetLayerValues(LinkedList<ParkingSearchStrategy> allStrategies,
			ParkingSearchStrategy strategy, int layerIndex) {
		double searchBeta = ZHScenarioGlobal.loadDoubleParam("parkingStrategyScenarioId."
				+ ZHScenarioGlobal.parkingStrategyScenarioId + ".layer." + layerIndex + ".searchBeta");

		double randomSearchDistance = ZHScenarioGlobal.loadDoubleParam("parkingStrategyScenarioId."
				+ ZHScenarioGlobal.parkingStrategyScenarioId + ".layer." + layerIndex + ".randomSearchDistance");

		((RandomParkingSearch) strategy).setSearchBeta(searchBeta);
		((RandomParkingSearch) strategy).setRandomSearchDistance(randomSearchDistance);
		allStrategies.add(strategy);
	}
}
