/* *********************************************************************** *
 * project: org.matsim.*
 * Events2Score4onlyTravPt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.scoring.ScoringFunctionAccumulator4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;

/**
 * @author yu
 * 
 */
public abstract class Events2Score4TravPerf extends EventsToScore implements
		CadytsChoice {
	/** Map<personId,Map<Plan,attr>> */
	protected Map<Id, Map<Plan, Double>> legDursCar = new HashMap<Id, Map<Plan, Double>>(),
			legDursPt = new HashMap<Id, Map<Plan, Double>>(),
			actAttrs = new HashMap<Id, Map<Plan, Double>>(),
			stuckAttrs = new HashMap<Id, Map<Plan, Double>>();
	protected Population pop = null;
	protected ScoringFunctionFactory sfFactory = null;
	protected PlanCalcScoreConfigGroup scoring;
	// protected boolean setPersonScore = true;
	protected int maxPlansPerAgent;
	protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
	protected final TreeMap<Id, Integer> agentPlanElementIndex = new TreeMap<Id, Integer>();

	public Events2Score4TravPerf(Population population,
			ScoringFunctionFactory factory, double learningRate,
			int maxPlansPerAgent, PlanCalcScoreConfigGroup scoring) {
		super(population, factory, scoring.getLearningRate());
		this.scoring = scoring;
		pop = population;
		this.maxPlansPerAgent = maxPlansPerAgent;
		sfFactory = factory;
	}

	public PlanCalcScoreConfigGroup getScoring() {
		return scoring;
	}

	public Double getAgentScore(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond().getScore();
	}

	protected Tuple<Plan, ScoringFunction> getScoringDataForAgent(
			final Id agentId) {
		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
		if (data == null) {
			Person person = pop.getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			data = new Tuple<Plan, ScoringFunction>(person.getSelectedPlan(),
					sfFactory.createNewScoringFunction(person
							.getSelectedPlan()));
			agentScorers.put(agentId, data);
		}
		return data;
	}

	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond();
	}

	public void setSfFactory(ScoringFunctionFactory sfFactory) {
		this.sfFactory = sfFactory;
	}

	/**
	 * set Attr. and Utility (not the score in MATSim) of plans of a person.
	 * This method should be called after removedPlans, i.e. there should be
	 * only choiceSetSize plans in the memory of an agent.
	 * 
	 * @param person
	 * @param performStats
	 * @param travelingCarStats
	 */
	public abstract void setPersonAttrs(Person person);

	public abstract void setPersonScore(Person person);

	public void reset(List<Tuple<Id, Plan>> toRemoves) {
		for (Tuple<Id, Plan> agentIdPlanPair : toRemoves) {
			Id agentId = agentIdPlanPair.getFirst();
			Plan plan = agentIdPlanPair.getSecond();
			Map<Plan, Double> legDurMapCar = legDursCar.get(agentId), legDurMapPt = legDursPt
					.get(agentId), actPerformMap = actAttrs.get(agentId), stuckAttrMap = stuckAttrs
					.get(agentId);
			if (legDurMapCar == null || legDurMapPt == null
					|| actPerformMap == null || stuckAttrMap == null) {
				throw new NullPointerException("BSE:\t\twasn't person\t"
						+ agentId + "\tsimulated?????");
			}

			legDurMapCar.remove(plan);
			legDurMapPt.remove(plan);
			actPerformMap.remove(plan);
			stuckAttrMap.remove(plan);
		}
	}

	public void reset(final int iteration) {
		agentScorers.clear();
		agentPlanElementIndex.clear();
		super.reset(iteration);
	}

	/**
	 * this method will be called in {@code
	 * DummyPlansScoring4PC.notifyScoring(ScoringEvent)}
	 */
	@Override
	public void finish() {
		for (Map.Entry<Id, Tuple<Plan, ScoringFunction>> entry : agentScorers
				.entrySet()) {
			Id pId = entry.getKey();
			Plan plan = entry.getValue().getFirst();
			ScoringFunction sf = entry.getValue().getSecond();
			sf.finish();
			double score = sf.getScore();
			/* this line of code must stay under the line of "sf.getScore" */
			ScoringFunctionAccumulator4PC sfa = (ScoringFunctionAccumulator4PC) sf;

			// legTravTimeCar
			Map<Plan, Double> legDurMapCar = legDursCar.get(pId);
			if (legDurMapCar == null) {
				legDurMapCar = new HashMap<Plan, Double>();
				legDursCar.put(pId, legDurMapCar);
			}
			legDurMapCar.put(plan, sfa.getTravTimeAttrCar());

			// legTravTimePt
			Map<Plan, Double> legDurMapPt = legDursPt.get(pId);
			if (legDurMapPt == null) {
				legDurMapPt = new HashMap<Plan, Double>();
				legDursPt.put(pId, legDurMapPt);
			}
			legDurMapPt.put(plan, sfa.getTravTimeAttrPt());

			// actDuration
			Map<Plan, Double> actAttrMap = actAttrs.get(pId);
			if (actAttrMap == null) {
				actAttrMap = new HashMap<Plan, Double>();
				actAttrs.put(pId, actAttrMap);
			}
			actAttrMap.put(plan, sfa.getPerfAttr());
			// stuckAttrs
			Map<Plan, Double> stuckAttrMap = stuckAttrs.get(pId);
			if (stuckAttrMap == null) {
				stuckAttrMap = new HashMap<Plan, Double>();
				stuckAttrs.put(pId, stuckAttrMap);
			}
			stuckAttrMap.put(plan, sfa.getStuckAttr());

			// *********************codes from {@code EventsToScore}
			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
			} else {
				double learningRate = scoring.getLearningRate();
				plan.setScore(learningRate * score + (1 - learningRate)
						* oldScore);
			}
			// System.out.println("SCORING:\tscoringFunction:\t"
			// + sf.getClass().getName() + "\tscore:\t" + score);
		}
	}

	public void handleEvent(final ActivityStartEvent event) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startActivity(event.getTime(),
					(Activity) data.getFirst().getPlanElements().get(index));
		}
	}

	protected int increaseAgentPlanElementIndex(final Id personId) {
		Integer index = agentPlanElementIndex.get(personId);
		if (index == null) {
			agentPlanElementIndex.put(personId, Integer.valueOf(1));
			return 1;
		}
		agentPlanElementIndex.put(personId, Integer.valueOf(1 + index
				.intValue()));
		return 1 + index.intValue();
	}

	public void handleEvent(final AgentDepartureEvent event) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startLeg(event.getTime(),
					(Leg) data.getFirst().getPlanElements().get(index));
		}
	}
}
