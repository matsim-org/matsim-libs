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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.StrategyStats;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomNumbers;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class ParkingStrategyManager {

	private static final Logger log = Logger.getLogger(ParkingStrategyManager.class);

	public static LinkedList<ParkingSearchStrategy> allStrategies;
	private HashMap<Id, HashMap<Integer, EvaluationContainer>> strategyEvaluations = new HashMap<Id, HashMap<Integer, EvaluationContainer>>();

	private Random globalbRandom;

	// personId, legIndex

	public ParkingStrategyManager(LinkedList<ParkingSearchStrategy> allStrategies) {
		ParkingStrategyManager.allStrategies = allStrategies;
		globalbRandom = RandomNumbers.getGlobalbRandom();
	}

	// this needs to be invoked at the starting of an iteration (does not assume
	// plan adaption with parking act/walk leg)
	public void prepareStrategiesForNewIteration(Person person, int currentIterationNumber) {
		Random random = MatsimRandom.getLocalInstance();
		Id agentId = person.getSelectedPlan().getPerson().getId();
		for (int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {
			PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);

			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;

				if (leg.getMode().equals(TransportMode.car)) {
					HashMap<Integer, EvaluationContainer> agentHashMap = getStrategyEvaluations().get(agentId);

					if (agentHashMap == null) {
						getStrategyEvaluations().put(agentId, new HashMap<Integer, EvaluationContainer>());
						agentHashMap = getStrategyEvaluations().get(agentId);
					}

					EvaluationContainer evaluationContainer = agentHashMap.get(i);

					if (evaluationContainer == null) {
						agentHashMap.put(i, new EvaluationContainer(allStrategies));
						evaluationContainer = agentHashMap.get(i);
					}

					if (ZHScenarioGlobal.loadBooleanParam("convergance.useMATSimLikeConvergence")) {
						handleMATSimApproach(evaluationContainer);
					} else {
						handleNonMATSimApproach(evaluationContainer);
					}
				}
			}
		}
	}

	private void handleMATSimApproach(EvaluationContainer evaluationContainer) {
		boolean preserveAllGroups;
		if (ZHScenarioGlobal
				.loadBooleanParam("convergance.useMATSimLikeConvergence.preserveAllGroups")){
			preserveAllGroups=true;
		} else {
			preserveAllGroups=false;
		}
		
		evaluationContainer.trimStrategySet(ZHScenarioGlobal
				.loadIntParam("convergance.useMATSimLikeConvergence.maxNumberOfPlansInMemory"),preserveAllGroups);

		
		DebugLib.emptyFunctionForSettingBreakPoint();
		
		if (ZHScenarioGlobal.loadIntParam("convergance.useMATSimLikeConvergence.switchToMNLOnlyAfterIteration") < ZHScenarioGlobal.iteration) {
			evaluationContainer.selectNextStrategyAccordingToMNLExp();
		} else {

			double executionProbabilityMNL=0;
			
			if (ZHScenarioGlobal.loadBooleanParam("convergance.useMATSimLikeConvergence.varyExecutionProbabilityOfMNL")){
				
				String interpolationFunction=ZHScenarioGlobal.loadStringParam("convergance.useMATSimLikeConvergence.varyExecutionProbabilityOfMNL.interpolationFunction");
				double initialProbability=ZHScenarioGlobal.loadDoubleParam("convergance.useMATSimLikeConvergence.varyExecutionProbabilityOfMNL.initialProbability");
				double finalProbability=ZHScenarioGlobal.loadDoubleParam("convergance.useMATSimLikeConvergence.varyExecutionProbabilityOfMNL.finalProbability");
				int finalProbabilityReachedAtIteration=ZHScenarioGlobal.loadIntParam("convergance.useMATSimLikeConvergence.varyExecutionProbabilityOfMNL.finalProbabilityReachedAtIteration");
				
				if (interpolationFunction.equalsIgnoreCase("linear")){
					double m=(finalProbability-initialProbability)/finalProbabilityReachedAtIteration;
					executionProbabilityMNL=m*ZHScenarioGlobal.iteration+initialProbability;
				} else if (interpolationFunction.equalsIgnoreCase("quadratic")){
					double a=(finalProbability-initialProbability)/(finalProbabilityReachedAtIteration*finalProbabilityReachedAtIteration);
					executionProbabilityMNL=a*ZHScenarioGlobal.iteration*ZHScenarioGlobal.iteration+initialProbability;
				} else {
					DebugLib.stopSystemAndReportInconsistency();
				}
			} else {
				executionProbabilityMNL = ZHScenarioGlobal
						.loadDoubleParam("convergance.useMATSimLikeConvergence.executionProbabilityMNL");
			}
			
			if (executionProbabilityMNL>1){
				executionProbabilityMNL=1.0;
			}
			
			int randomizationPhaseEndsAtIteration=ZHScenarioGlobal.loadIntParam("convergance.useMATSimLikeConvergence.randomizationPhaseEndsAtIteration");
			
			if (((globalbRandom.nextDouble() < executionProbabilityMNL || !evaluationContainer.allStrategiesHaveBeenExecutedOnce()) && randomizationPhaseEndsAtIteration<ZHScenarioGlobal.iteration) || !evaluationContainer.hasNewStrategies()) {
				if (evaluationContainer.allStrategiesHaveBeenExecutedOnce()) {
					evaluationContainer.selectNextStrategyAccordingToMNLExp();
				} else {
					evaluationContainer.addRandomPlanFromCurrentGroupAndSelectForExecution();
				}
			} else {
				evaluationContainer.addRandomPlanFromCurrentGroupAndSelectForExecution();
			}
		}
		
		DebugLib.emptyFunctionForSettingBreakPoint();
	}

	public void handleNonMATSimApproach(EvaluationContainer evaluationContainer) {
		handleDropWorseGroupStrategies(evaluationContainer);

		if (ZHScenarioGlobal.loadBooleanParam("convergance.evaluateAllStrategiesOnceAtBeginning")
				&& !evaluationContainer.allStrategiesHaveBeenExecutedOnce()) {
			evaluationContainer.selectStrategyNotExecutedTillNow();
		} else {
			/*
			 * if (ZHScenarioGlobal.iteration < ZHScenarioGlobal .loadIntParam(
			 * "convergance.executeRandomStrategy.stopStrategyAtIteration" )) {
			 * evaluationContainer.selectRandomStrategy(); } else
			 */
			if (ZHScenarioGlobal.iteration < ZHScenarioGlobal
					.loadIntParam("convergance.fixedPropbabilityBestStrategy.stopStrategyAtIteration")) {
				evaluationContainer.selectStrategyAccordingToFixedProbabilityForBestStrategy();
			} else if (ZHScenarioGlobal.iteration >= ZHScenarioGlobal
					.loadIntParam("convergance.MNLExpScore.startStrategyAtIteration")) {
				evaluationContainer.selectNextStrategyAccordingToMNLExp();
			} else if (ZHScenarioGlobal.iteration >= ZHScenarioGlobal.loadIntParam("convergance.MNLScore.startStrategyAtIteration")) {
				evaluationContainer.selectNextStrategyAccordingToProbability();
			} else {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}

		if (ZHScenarioGlobal.loadStringParam("convergance.dropStrategy").equalsIgnoreCase("dropAllOnce")) {
			if (ZHScenarioGlobal.loadIntParam("convergance.dropAllOnce.atIteration") == ZHScenarioGlobal.iteration) {
				evaluationContainer
						.trimStrategySet(ZHScenarioGlobal.loadIntParam("convergance.dropStrategy.minNumberOfStrategies"), false);
			}
		} else if (ZHScenarioGlobal.loadStringParam("convergance.dropStrategy").equalsIgnoreCase("dropOneByOne")) {
			if (evaluationContainer.getNumberOfStrategies() > ZHScenarioGlobal
					.loadIntParam("convergance.dropStrategy.minNumberOfStrategies")) {
				int startDrop = ZHScenarioGlobal.loadIntParam("convergance.dropOneByOne.startDroppingAtIteration");
				if (startDrop == ZHScenarioGlobal.iteration) {
					evaluationContainer.dropWostStrategy();
				} else if (startDrop < ZHScenarioGlobal.iteration) {
					int dropEachNthIter = ZHScenarioGlobal.loadIntParam("convergance.dropOneByOne.dropEachNthIteration");
					if ((ZHScenarioGlobal.iteration - startDrop) % dropEachNthIter == 0) {
						evaluationContainer.dropWostStrategy();
					}
				}
			}
		}
	}

	private void handleDropWorseGroupStrategies(EvaluationContainer evaluationContainer) {

		for (int i = 0; i < 10; i++) {
			if (ZHScenarioGlobal.paramterExists("convergance.dropWorseGroupStrategies-" + i + "-AtIteration")) {
				int dropAtIteration = ZHScenarioGlobal.loadIntParam("convergance.dropWorseGroupStrategies-" + i + "-AtIteration");

				if (ZHScenarioGlobal.iteration == dropAtIteration) {
					int numberOfStrategiesToDrop = ZHScenarioGlobal.loadIntParam("convergance.dropWorseGroupStrategies-" + i
							+ "-NumberOfStrategies");

					for (int j = 0; j < numberOfStrategiesToDrop; j++) {
						evaluationContainer.dropWostGroupStrategiesWithoutEliminatingGroup();
					}
				}
			}
		}
	}

	public void updateScore(Id personId, int legIndex, double score) {
		EvaluationContainer evaluationContainer = getStrategyEvaluations().get(personId).get(legIndex);
		if (evaluationContainer == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		evaluationContainer.updateScoreOfSelectedStrategy(score);
	}

	public void printStrategyStatistics() {
		IntegerValueHashMap<String> numberOfTimesStrategySelected = new IntegerValueHashMap<String>();
		log.info(" --- start strategy stats ---");

		for (Id personId : getStrategyEvaluations().keySet()) {
			for (Integer legIndex : getStrategyEvaluations().get(personId).keySet()) {
				EvaluationContainer evaluationContainer = getStrategyEvaluations().get(personId).get(legIndex);
				numberOfTimesStrategySelected.increment(evaluationContainer.getCurrentSelectedStrategy().strategy.getName());
			}
		}

		numberOfTimesStrategySelected.addToLog();
		log.info(" --- end strategy stats ---");
	}

	public ParkingSearchStrategy getParkingStrategyForCurrentLeg(Person person, int currentPlanElementIndex) {
		if (getStrategyEvaluations().get(person.getId()).get(currentPlanElementIndex) == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		return getStrategyEvaluations().get(person.getId()).get(currentPlanElementIndex).getCurrentSelectedStrategy().strategy;
	}

	public void writeStatisticsToFile() {
		ZHScenarioGlobal.strategyScoreStats.addIterationData(getStrategyEvaluations());
		ZHScenarioGlobal.strategyScoreStats.updateStrategySharesWithoutPP();
		ZHScenarioGlobal.strategyScoreStats.updateStrategyGroupsShares();
		ZHScenarioGlobal.strategyScoreStats.writeStrategyScoresToFile();
		ZHScenarioGlobal.strategyScoreStats.writeAllStrategySharesToFile();
		ZHScenarioGlobal.strategyScoreStats.writeNonPPStrategySharesToFile();
		ZHScenarioGlobal.strategyScoreStats.writeGroupSharesToFile();

		if (ZHScenarioGlobal.writeOutputInCurrentIteration()) {
			ZHScenarioGlobal.strategyScoreStats.writeToTextFile(getStrategyEvaluations());
		}
	}

	public void reset() {
		for (ParkingSearchStrategy pss : allStrategies) {
			pss.resetForNewIteration();
		}

	}

	public HashMap<Id, HashMap<Integer, EvaluationContainer>> getStrategyEvaluations() {
		return strategyEvaluations;
	}

	public void setStrategyEvaluations(HashMap<Id, HashMap<Integer, EvaluationContainer>> strategyEvaluations) {
		this.strategyEvaluations = strategyEvaluations;
	}

}
