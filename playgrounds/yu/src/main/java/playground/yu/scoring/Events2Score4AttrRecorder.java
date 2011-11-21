/* *********************************************************************** *
 * project: org.matsim.*
 * MnlChoice.java
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
package playground.yu.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAdapter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author yu
 * 
 */
public class Events2Score4AttrRecorder extends EventsToScore {
	public final static List<String> attrNameList = new ArrayList<String>();
	// public final static List<Double> paramScaleFactorList = new
	// ArrayList<Double>();

	// private static final String PARAM_SCALE_FACTOR_INDEX =
	// "paramScaleFactor_";

	protected final Config config;
	// /** Map<personId,Map<Plan,attr>> */
	// protected Map<Id/* agent */, Map<Plan, Double>> legDursCar = new
	// HashMap<Id, Map<Plan, Double>>(),
	// legDursPt = new HashMap<Id, Map<Plan, Double>>(),
	// legDursWalk = new HashMap<Id, Map<Plan, Double>>(),
	// actAttrs = new HashMap<Id, Map<Plan, Double>>(),
	// stuckAttrs = new HashMap<Id, Map<Plan, Double>>(),
	// distancesCar = new HashMap<Id, Map<Plan, Double>>(),
	// distancesPt = new HashMap<Id, Map<Plan, Double>>(),
	// distancesWalk = new HashMap<Id, Map<Plan, Double>>();
	// protected Map<Id/* agent */, Map<Plan, Integer>> carLegNos = new
	// HashMap<Id, Map<Plan, Integer>>(),
	// ptLegNos = new HashMap<Id, Map<Plan, Integer>>(),
	// walkLegNos = new HashMap<Id, Map<Plan, Integer>>();
	protected Population pop = null;
	protected ScoringFunctionFactory sfFactory = null;
	protected PlanCalcScoreConfigGroup scoring;
	// protected boolean setPersonScore = true;
	protected int maxPlansPerAgent;
	protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
	protected final TreeMap<Id, Integer> agentPlanElementIndex = new TreeMap<Id, Integer>();

	protected int iteration = -1;

	// boolean setUCinMNL = true;

	public Events2Score4AttrRecorder(Config config,
			ScoringFunctionFactory sfFactory, Scenario scenario) {
		super(scenario, sfFactory, config.planCalcScore().getLearningRate());
		attrNameList.clear();
		this.config = config;
		// #####################################
		// travelTime
		attrNameList.add("traveling");
		attrNameList.add("travelingPt");
		attrNameList.add("travelingWalk");

		// actPerforming
		attrNameList.add("performing");

		// attrNameList.addScaleFactor("lateArrival");//TODO in
		// ActivityScoringFunction4PC

		// stuck
		attrNameList.add("stuck");

		// distanceAttr
		attrNameList.add("monetaryDistanceCostRateCar");
		attrNameList.add("monetaryDistanceCostRatePt");
		attrNameList.add("marginalUtlOfDistanceWalk");

		// LegOffsetAttr
		attrNameList.add("constantCar");
		attrNameList.add("constantPt");
		attrNameList.add("constantWalk");
		// #####################################
		scoring = config.planCalcScore();
		pop = scenario.getPopulation();
		maxPlansPerAgent = config.strategy().getMaxAgentPlanMemorySize();
		this.sfFactory = sfFactory;

		// *********************************************************
		// mnl = createMultinomialLogit(config);
		//
		// String setUCinMNLStr = config.findParam(
		// BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
		// "setUCinMNL");
		// if (setUCinMNLStr != null) {
		// setUCinMNL = Boolean.parseBoolean(setUCinMNLStr);
		// System.out.println("BSE:\tsetUCinMNL\t=\t" + setUCinMNL);
		// } else {
		// System.out.println("BSE:\tsetUCinMNL\t= default value\t"
		// + setUCinMNL);
		// }
	}

	// the local agentScorers has to be used
	@Override
	public Double getAgentScore(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond().getScore();
	}

	// the local agentScorers has to be used
	protected Tuple<Plan, ScoringFunction> getPlanAndScoringFunctionForAgent(
			final Id agentId) {
		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
		if (data == null) {
			Person person = pop.getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			data = new Tuple<Plan, ScoringFunction>(
					person.getSelectedPlan(),
					sfFactory.createNewScoringFunction(person.getSelectedPlan()));
			agentScorers.put(agentId, data);
		}
		return data;
	}

	@Override
	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond();
	}

	public void setSfFactory(ScoringFunctionFactory sfFactory) {
		this.sfFactory = sfFactory;
	}

	// the local agentScorers has to be used
	@Override
	public void reset(final int iteration) {
		agentScorers.clear();
		agentPlanElementIndex.clear();
		super.reset(iteration);
		this.iteration = iteration;
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			((ScoringFunctionAdapter) data.getSecond()).startActivity(
					event.getTime(), (Activity) data.getFirst()
							.getPlanElements().get(index));
		}
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		Tuple<Plan, ScoringFunction> planAndScoringFunction = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (planAndScoringFunction != null) {
			int index = getAgentPlanElementIndex(event.getPersonId());
			((ScoringFunctionAdapter) planAndScoringFunction.getSecond())
					.endActivity(event.getTime(),
							(Activity) planAndScoringFunction.getFirst()
									.getPlanElements().get(index));
		}
	}

	protected int getAgentPlanElementIndex(Id personId) {
		Integer index = agentPlanElementIndex.get(personId);
		if (index == null) {
			agentPlanElementIndex.put(personId, Integer.valueOf(0));
			return 0;
		}
		return index.intValue();
	}

	protected int increaseAgentPlanElementIndex(final Id personId) {
		Integer index = agentPlanElementIndex.get(personId);
		if (index == null) {
			agentPlanElementIndex.put(personId, Integer.valueOf(1));
			return 1;
		}
		agentPlanElementIndex.put(personId,
				Integer.valueOf(1 + index.intValue()));
		return 1 + index.intValue();
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		super.handleEvent(event);
		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			((ScoringFunctionAdapter) data.getSecond()).startLeg(
					event.getTime(), (Leg) data.getFirst().getPlanElements()
							.get(index));
		}
	}

	@Override
	public void finish() {
		for (Tuple<Plan, ScoringFunction> plansScorFunction : agentScorers
				.values()) {

			Plan plan = plansScorFunction.getFirst();
			Map<String, Object> attrs = plan.getCustomAttributes();

			ScoringFunction sf = plansScorFunction.getSecond();
			sf.finish();
			double score = sf.getScore();
			// **********************codes from {@code EventsToScore}
			/* this line of code must stay under the line of "sf.getScore" */
			ScoringFunctionAccumulatorWithAttrRecorder sfa = (ScoringFunctionAccumulatorWithAttrRecorder) sf;

			// legTravTimeCar
			attrs.put("traveling", sfa.getTravTimeAttrCar());

			// legTravTimePt
			attrs.put("travelingPt", sfa.getTravTimeAttrPt());

			// legTravTimeWalk
			attrs.put("travelingWalk", sfa.getTravTimeAttrWalk());

			// actDuration
			attrs.put("performing", sfa.getPerfAttr());

			// stuckAttrs
			attrs.put("stuck", sfa.getStuckAttr());

			// distancesCar
			attrs.put("monetaryDistanceCostRateCar", sfa.getDistanceCar());

			// distancesPt
			attrs.put("monetaryDistanceCostRatePt", sfa.getDistancePt());

			// distancesWalk
			attrs.put("marginalUtlOfDistanceWalk", sfa.getDistanceWalk());

			// carLegNo
			attrs.put("constantCar", sfa.getCarLegNo());

			// ptLegNo
			attrs.put("constantPt", sfa.getPtLegNo());

			// walkLegNo
			attrs.put("constantWalk", sfa.getWalkLegNo());
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
}
