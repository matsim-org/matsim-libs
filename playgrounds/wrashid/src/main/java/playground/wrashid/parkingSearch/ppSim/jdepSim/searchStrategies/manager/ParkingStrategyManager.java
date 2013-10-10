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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class ParkingStrategyManager {

	private static final Logger log = Logger.getLogger(ParkingStrategyManager.class);
	
	double executionProbabilityOfBestStrategy=0.9;
	WithinDayAgentUtils withinDayAgentUtils = new WithinDayAgentUtils();
	LinkedList<ParkingSearchStrategy> allStrategies;
	HashMap<Id, HashMap<Integer,HashMap<Id,EvaluationContainer>>> strategyEvaluations=new HashMap<Id, HashMap<Integer,HashMap<Id,EvaluationContainer>>>();
	// personId, legIndex, linkId
	
	public ParkingStrategyManager(LinkedList<ParkingSearchStrategy> allStrategies){
		this.allStrategies = allStrategies;
	}
	
	// this needs to be invoked at the starting of an iteration (does not assume plan adaption with parking act/walk leg)
	public void prepareStrategiesForNewIteration(Person person, int currentIterationNumber){
		Random random = MatsimRandom.getLocalInstance();
		Id agentId = person.getSelectedPlan().getPerson().getId();
		for (int i=0;i<person.getSelectedPlan().getPlanElements().size();i++){
			PlanElement pe=person.getSelectedPlan().getPlanElements().get(i);
						
			if (pe instanceof LegImpl){
				LegImpl leg=(LegImpl) pe;
				
				if (leg.getMode().equals(TransportMode.car)){
					Id nextActLinkId=((ActivityImpl) person.getSelectedPlan().getPlanElements().get(i+3)).getLinkId();
				
					HashMap<Integer, HashMap<Id, EvaluationContainer>> agentHashMap = strategyEvaluations.get(agentId);
					
					if (agentHashMap==null){
						strategyEvaluations.put(agentId, new HashMap<Integer, HashMap<Id,EvaluationContainer>>());
						agentHashMap = strategyEvaluations.get(agentId);
					}
					
					HashMap<Id, EvaluationContainer> legIndexHashMap = agentHashMap.get(i);
					
					if (legIndexHashMap==null){
						agentHashMap.put(i, new HashMap<Id,EvaluationContainer>());
						legIndexHashMap = agentHashMap.get(i);
					}
					
					if (legIndexHashMap.size()==0){
						// initialize 
						legIndexHashMap.put(nextActLinkId, createEvaulationContainerForAgentAtLeg(person,i,currentIterationNumber));
					} else if (legIndexHashMap.size()==1){
						Id linkIdInHM=null;
						for (Id linkId:legIndexHashMap.keySet()){
							linkIdInHM=linkId;
						}
						
						if (linkIdInHM==nextActLinkId){
							// continue with evaluation of strategy
							EvaluationContainer evaluationContainer = legIndexHashMap.get(nextActLinkId);
							if (evaluationContainer.getLastIterationContainerUsed()+1==currentIterationNumber){
								// continue with strategy optimization
								evaluationContainer.setLastIterationContainerUsed(currentIterationNumber);
								
								if (random.nextDouble()<executionProbabilityOfBestStrategy){
									evaluationContainer.selectBestStrategyForExecution();
								} else {
									evaluationContainer.selectLongestNonExecutedStrategyForExecution();
								}
							}else {
								// the activity location changed -> reset
								legIndexHashMap.clear();
								legIndexHashMap.put(nextActLinkId, createEvaulationContainerForAgentAtLeg(person,i,currentIterationNumber));
							}
						} else {
							// the activity location changed -> reset
							legIndexHashMap.clear();
							legIndexHashMap.put(nextActLinkId, createEvaulationContainerForAgentAtLeg(person,i,currentIterationNumber));
						}
					} else {
						DebugLib.stopSystemAndReportInconsistency("something went really wrong...");
					}
				}
			}
		}
		
		
		
	}

	public void updateScore(Id personId, int legIndex, double score){
		HashMap<Id, EvaluationContainer> legIndexHashMap = strategyEvaluations.get(personId).get(legIndex);
		Id linkIdInHM=null;
		for (Id linkId:legIndexHashMap.keySet()){
			linkIdInHM=linkId;
		}
		legIndexHashMap.get(linkIdInHM).updateScoreOfSelectedStrategy(score);
	}
	
	
	
	private EvaluationContainer createEvaulationContainerForAgentAtLeg(Person person, int legIndex, int iterationNumber) {
		EvaluationContainer evaluationContainer=new EvaluationContainer(allStrategies);
		evaluationContainer.setLastIterationContainerUsed(iterationNumber);
		return evaluationContainer;
	}

	public void printStrategyStatistics(){
		IntegerValueHashMap<String> numberOfTimesStrategySelected=new IntegerValueHashMap<String>();
		log.info(" --- start strategy stats ---");
		
		for (Id personId:strategyEvaluations.keySet()){
			for (Integer legIndex:strategyEvaluations.get(personId).keySet()){
				for (Id linkId:strategyEvaluations.get(personId).get(legIndex).keySet()){
					EvaluationContainer evaluationContainer = strategyEvaluations.get(personId).get(legIndex).get(linkId);
					
					//log.info("legIndex: " + legIndex);
					//evaluationContainer.printAllScores();
					
					numberOfTimesStrategySelected.increment(evaluationContainer.getCurrentSelectedStrategy().strategy.getName());
				}
			}
		}
		
		
		numberOfTimesStrategySelected.addToLog();
		log.info(" --- end strategy stats ---");
	}

	public ParkingSearchStrategy getParkingStrategyForCurrentLeg(Person person, int currentPlanElementIndex) {
		Id nextActLinkId=((ActivityImpl) person.getSelectedPlan().getPlanElements().get(currentPlanElementIndex+1)).getLinkId();
		return strategyEvaluations.get(person.getId()).get(currentPlanElementIndex).get(nextActLinkId).getCurrentSelectedStrategy().strategy;		
	}
	
}

