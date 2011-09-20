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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.scoring;

import java.util.ArrayList;
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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
import cadyts.utilities.math.BasicStatistics;

/**
 * @author yu
 * 
 */
public abstract class Events2Score4PC extends EventsToScore implements
		CadytsChoice {
	public static List<String> attrNameList = new ArrayList<String>();
	public static List<Double> paramScaleFactorList = new ArrayList<Double>();

	private static final String PARAM_SCALE_FACTOR_INDEX = "paramScaleFactor_";

	protected final Config config;
	/** Map<personId,Map<Plan,attr>> */
	protected Map<Id/* agent */, Map<Plan, Double>> legDursCar = new HashMap<Id, Map<Plan, Double>>(),
			legDursPt = new HashMap<Id, Map<Plan, Double>>(),
			legDursWalk = new HashMap<Id, Map<Plan, Double>>(),
			actAttrs = new HashMap<Id, Map<Plan, Double>>(),
			stuckAttrs = new HashMap<Id, Map<Plan, Double>>(),
			distancesCar = new HashMap<Id, Map<Plan, Double>>(),
			distancesPt = new HashMap<Id, Map<Plan, Double>>(),
			distancesWalk = new HashMap<Id, Map<Plan, Double>>();
	protected Map<Id/* agent */, Map<Plan, Integer>> carLegNos = new HashMap<Id, Map<Plan, Integer>>(),
			ptLegNos = new HashMap<Id, Map<Plan, Integer>>(),
			walkLegNos = new HashMap<Id, Map<Plan, Integer>>();
	protected Population pop = null;
	protected ScoringFunctionFactory sfFactory = null;
	protected PlanCalcScoreConfigGroup scoring;
	// protected boolean setPersonScore = true;
	protected int maxPlansPerAgent;
	protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
	protected final TreeMap<Id, Integer> agentPlanElementIndex = new TreeMap<Id, Integer>();

	public Events2Score4PC(Config config, ScoringFunctionFactory factory,
			Population population) {
		super(population, factory, config.planCalcScore().getLearningRate());
		attrNameList.clear();
		this.config = config;
		// #####################################
		// travelTime
		initialAttrNameScaleFactor("traveling");
		initialAttrNameScaleFactor("travelingPt");
		initialAttrNameScaleFactor("travelingWalk");

		// actPerforming
		initialAttrNameScaleFactor("performing");

		// initialAttrNameScaleFactor("lateArrival");//TODO in
		// ActivityScoringFunction4PC

		// stuck
		initialAttrNameScaleFactor("stuck");

		// distanceAttr
		initialAttrNameScaleFactor("monetaryDistanceCostRateCar");
		initialAttrNameScaleFactor("monetaryDistanceCostRatePt");
		initialAttrNameScaleFactor("marginalUtlOfDistanceWalk");

		// LegOffsetAttr
		initialAttrNameScaleFactor("constantCar");
		initialAttrNameScaleFactor("constantPt");
		initialAttrNameScaleFactor("constantWalk");
		// #####################################
		scoring = config.planCalcScore();
		pop = population;
		maxPlansPerAgent = config.strategy().getMaxAgentPlanMemorySize();
		sfFactory = factory;
	}

	private void initialAttrNameScaleFactor(String attributeName) {
		attrNameList.add(attributeName);
		String paramScaleFactorIStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				PARAM_SCALE_FACTOR_INDEX + attributeName);
		paramScaleFactorList.add(paramScaleFactorIStr == null ? 1d : Double
				.parseDouble(paramScaleFactorIStr));
	}

	@Override
	public PlanCalcScoreConfigGroup getScoring() {
		return scoring;
	}

	@Override
	public Double getAgentScore(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond().getScore();
	}

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

	/**
	 * set Attr. and Utility (not the score in MATSim) of plans of a person.
	 * This method should be called after removedPlans, i.e. there should be
	 * only choiceSetSize plans in the memory of an agent.
	 * 
	 * @param person
	 * @param monetaryDistanceCostRateCarStats
	 */

	@Override
	public abstract void setPersonAttrs(Person person, BasicStatistics[] stats/* monetaryDistanceCostRateCarStats */);

	@Override
	public abstract void setPersonScore(Person person);

	@Override
	public void reset(List<Tuple<Id, Plan>> toRemoves) {
		for (Tuple<Id, Plan> agentIdPlanPair : toRemoves) {
			Id agentId = agentIdPlanPair.getFirst();
			Plan plan = agentIdPlanPair.getSecond();
			Map<Plan, Double> legDurMapCar = legDursCar.get(agentId), legDurMapPt = legDursPt
					.get(agentId), legDurMapWalk = legDursWalk.get(agentId), actPerformMap = actAttrs
					.get(agentId), stuckAttrMap = stuckAttrs.get(agentId), distanceMapCar = distancesCar
					.get(agentId), distanceMapPt = distancesPt.get(agentId), distanceMapWalk = distancesWalk
					.get(agentId);
			Map<Plan, Integer> carLegNoMap = carLegNos.get(agentId), ptLegNoMap = ptLegNos
					.get(agentId), walkLegNoMap = walkLegNos.get(agentId);

			if (legDurMapCar == null || legDurMapPt == null
					|| legDurMapWalk == null || actPerformMap == null
					|| stuckAttrMap == null || distanceMapCar == null
					|| distanceMapPt == null || distanceMapWalk == null
					|| carLegNoMap == null || ptLegNoMap == null
					|| walkLegNoMap == null) {
				throw new NullPointerException("BSE:\t\twasn't person\t"
						+ agentId + "\tsimulated?????");
			}

			legDurMapCar.remove(plan);
			legDurMapPt.remove(plan);
			legDurMapWalk.remove(plan);

			actPerformMap.remove(plan);

			stuckAttrMap.remove(plan);

			distanceMapCar.remove(plan);
			distanceMapPt.remove(plan);
			distanceMapWalk.remove(plan);

			carLegNoMap.remove(plan);
			ptLegNoMap.remove(plan);
			walkLegNoMap.remove(plan);
		}
	}

	@Override
	public void reset(final int iteration) {
		agentScorers.clear();
		agentPlanElementIndex.clear();
		super.reset(iteration);
	}

	/**
	 * this method will be called in
	 * {@code DummyPlansScoring4PC.notifyScoring(ScoringEvent)}
	 */
	@Override
	public void finish() {
		for (Map.Entry<Id, Tuple<Plan, ScoringFunction>> entry : agentScorers
				.entrySet()) {
			Id agentId = entry.getKey();

			Plan plan = entry.getValue().getFirst();
			ScoringFunction sf = entry.getValue().getSecond();
			sf.finish();
			double score = sf.getScore();
			// **********************codes from {@code EventsToScore}
			/* this line of code must stay under the line of "sf.getScore" */
			ScoringFunctionAccumulator4PC sfa = (ScoringFunctionAccumulator4PC) sf;

			// legTravTimeCar
			Map<Plan, Double> legDurMapCar = legDursCar.get(agentId);
			if (legDurMapCar == null) {
				legDurMapCar = new HashMap<Plan, Double>();
				legDursCar.put(agentId, legDurMapCar);
			}
			legDurMapCar.put(plan, sfa.getTravTimeAttrCar());

			// legTravTimePt
			Map<Plan, Double> legDurMapPt = legDursPt.get(agentId);
			if (legDurMapPt == null) {
				legDurMapPt = new HashMap<Plan, Double>();
				legDursPt.put(agentId, legDurMapPt);
			}
			legDurMapPt.put(plan, sfa.getTravTimeAttrPt());

			// legTravTimeWalk
			Map<Plan, Double> legDurMapWalk = legDursWalk.get(agentId);
			if (legDurMapWalk == null) {
				legDurMapWalk = new HashMap<Plan, Double>();
				legDursWalk.put(agentId, legDurMapWalk);
			}
			legDurMapWalk.put(plan, sfa.getTravTimeAttrWalk());

			// actDuration
			Map<Plan, Double> actAttrMap = actAttrs.get(agentId);
			if (actAttrMap == null) {
				actAttrMap = new HashMap<Plan, Double>();
				actAttrs.put(agentId, actAttrMap);
			}
			actAttrMap.put(plan, sfa.getPerfAttr());

			// stuckAttrs
			Map<Plan, Double> stuckAttrMap = stuckAttrs.get(agentId);
			if (stuckAttrMap == null) {
				stuckAttrMap = new HashMap<Plan, Double>();
				stuckAttrs.put(agentId, stuckAttrMap);
			}
			stuckAttrMap.put(plan, sfa.getStuckAttr());

			// distancesCar
			Map<Plan, Double> distanceMapCar = distancesCar.get(agentId);
			if (distanceMapCar == null) {
				distanceMapCar = new HashMap<Plan, Double>();
				distancesCar.put(agentId, distanceMapCar);
			}
			distanceMapCar.put(plan, sfa.getDistanceCar());

			// distancesPt
			Map<Plan, Double> distanceMapPt = distancesPt.get(agentId);
			if (distanceMapPt == null) {
				distanceMapPt = new HashMap<Plan, Double>();
				distancesPt.put(agentId, distanceMapPt);
			}
			distanceMapPt.put(plan, sfa.getDistancePt());

			// distancesWalk
			Map<Plan, Double> distanceMapWalk = distancesWalk.get(agentId);
			if (distanceMapWalk == null) {
				distanceMapWalk = new HashMap<Plan, Double>();
				distancesWalk.put(agentId, distanceMapWalk);
			}
			distanceMapWalk.put(plan, sfa.getDistanceWalk());

			// carLegNo
			Map<Plan, Integer> carLegNoMap = carLegNos.get(agentId);
			if (carLegNoMap == null) {
				carLegNoMap = new HashMap<Plan, Integer>();
				carLegNos.put(agentId, carLegNoMap);
			}
			carLegNoMap.put(plan, sfa.getCarLegNo());

			// ptLegNo
			Map<Plan, Integer> ptLegNoMap = ptLegNos.get(agentId);
			if (ptLegNoMap == null) {
				ptLegNoMap = new HashMap<Plan, Integer>();
				ptLegNos.put(agentId, ptLegNoMap);
			}
			ptLegNoMap.put(plan, sfa.getPtLegNo());

			// walkLegNo
			Map<Plan, Integer> walkLegNoMap = walkLegNos.get(agentId);
			if (walkLegNoMap == null) {
				walkLegNoMap = new HashMap<Plan, Integer>();
				walkLegNos.put(agentId, walkLegNoMap);
			}
			walkLegNoMap.put(plan, sfa.getWalkLegNo());
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

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startActivity(event.getTime(),
					(Activity) data.getFirst().getPlanElements().get(index));
		}
	}

	public void handleEvent(final ActivityEndEvent event) {
		Tuple<Plan, ScoringFunction> planAndScoringFunction = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (planAndScoringFunction != null) {
			int index = getAgentPlanElementIndex(event.getPersonId());
			planAndScoringFunction.getSecond().endActivity(
					event.getTime(),
					(Activity) planAndScoringFunction.getFirst()
							.getPlanElements().get(index));
		}
	}

	private int getAgentPlanElementIndex(Id personId) {
		Integer index = this.agentPlanElementIndex.get(personId);
		if (index == null) {
			this.agentPlanElementIndex.put(personId, Integer.valueOf(0));
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
		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startLeg(event.getTime(),
					(Leg) data.getFirst().getPlanElements().get(index));
		}
	}
}
