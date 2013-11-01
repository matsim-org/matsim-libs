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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskAddLastPartToRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadPool;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy3;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.EvaluationContainer;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.StrategyEvaluation;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingManagerZH;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class ParkingMemory {

	private static final Logger log = Logger.getLogger(ParkingMemory.class);

	private static TwoHashMapsConcatenated<Id, Integer, ParkingMemory> parkingMemories = new TwoHashMapsConcatenated<Id, Integer, ParkingMemory>();

	public static ParkingMemory getParkingMemory(Id personId, Integer legIndex) {
		if (parkingMemories.get(personId, legIndex) == null) {
			parkingMemories.put(personId, legIndex, new ParkingMemory());
		}

		return parkingMemories.get(personId, legIndex);
	}

	public static void resetMemory() {
		parkingMemories = new TwoHashMapsConcatenated<Id, Integer, ParkingMemory>();
	}

	public static void prepareForNextIteration() {
		log.info("starting prep parking memory");
		RerouteThreadPool rtPool = new RerouteThreadPool(ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning, Message.ttMatrix,
				ZHScenarioGlobal.scenario.getNetwork());

		HashMap<Id, HashMap<Integer, EvaluationContainer>> strategyEvaluations = AgentWithParking.parkingStrategyManager
				.getStrategyEvaluations();

		for (Id personId : strategyEvaluations.keySet()) {
			for (Integer legIndex : strategyEvaluations.get(personId).keySet()) {
				EvaluationContainer evaluationContainer = strategyEvaluations.get(personId).get(legIndex);

				for (StrategyEvaluation se : evaluationContainer.getEvaluations()) {

					if (se.strategy.getClass() == AxPo1989_Strategy3.class) {
						ParkingMemory parkingMemory = getParkingMemory(personId, legIndex);

						if (ZHScenarioGlobal.iteration > 0) {
							List<PlanElement> planElements = ZHScenarioGlobal.scenario.getPopulation().getPersons().get(personId)
									.getSelectedPlan().getPlanElements();
							LegImpl leg = (LegImpl) planElements.get(legIndex);

							ActivityImpl previousAct = (ActivityImpl) planElements.get(legIndex - 3);

							int nextCarLegIndex = getNextCarLegIndex(planElements, legIndex);

							if (nextCarLegIndex != -1) {
								ActivityImpl actAfterNextCarLeg = (ActivityImpl) planElements.get(nextCarLegIndex + 3);

								Id linkOfParking = AgentWithParking.parkingManager
										.getLinkOfParking(parkingMemory.closestFreeGarageParkingAtTimeOfArrival);

								if (!linkOfParking.toString().equalsIgnoreCase(actAfterNextCarLeg.getLinkId().toString())) {
									rtPool.addTask(new RerouteTaskAddLastPartToRoute(previousAct.getEndTime(), leg, linkOfParking));
								}
							}
						}
					}

				}
			}
		}

		rtPool.start();

		log.info("end prep parking memory");
	}

	private static int getNextCarLegIndex(List<PlanElement> planElements, Integer currentCarLegIndex) {
		int i = currentCarLegIndex + 1;
		while (i < planElements.size()) {
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().endsWith(TransportMode.car)) {
					return i;
				}
			}
			i++;
		}
		return -1;
	}

	public Id closestFreeGarageParkingAtTimeOfArrival;
}
