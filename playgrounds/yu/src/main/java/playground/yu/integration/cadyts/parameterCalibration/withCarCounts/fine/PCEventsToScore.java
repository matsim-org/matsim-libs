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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.fine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.scoring.ScoringFunctionAccumulator4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import playground.yu.utils.io.SimpleWriter;
import cadyts.utilities.math.BasicStatistics;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

/**
 * @author yu
 *
 */
public class PCEventsToScore extends EventsToScore implements CadytsChoice {
	private final static Logger log = Logger.getLogger(PCEventsToScore.class);

	public static List<String> attrNameList = new ArrayList<String>();
	public static List<Double> paramScaleFactorList = new ArrayList<Double>();

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

	private MultinomialLogit mnl;
	private SimpleWriter writer = null;
	private int iteration = -1;

	public PCEventsToScore(Config config, ScoringFunctionFactory factory,
			Population population) {
		super(population, factory, config.planCalcScore().getLearningRate());
		attrNameList.clear();
		this.config = config;

		// #####################################
		// travelTime
		attrNameList.add("traveling");
		attrNameList.add("travelingPt");
		attrNameList.add("travelingWalk");

		// actPerforming
		attrNameList.add("performing");

		// attrNameList.add("lateArrival");//TODO in
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
		pop = population;
		maxPlansPerAgent = config.strategy().getMaxAgentPlanMemorySize();
		sfFactory = factory;

		mnl = MultinomialLogitCreator.createMultinomialLogit(config);
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

	protected Tuple<Plan, ScoringFunction> getScoringDataForAgent(
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
	 * @param monetaryDistanceCostRateCarStats
	 */

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
		this.iteration = iteration;
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
		agentPlanElementIndex.put(personId,
				Integer.valueOf(1 + index.intValue()));
		return 1 + index.intValue();
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(event
				.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startLeg(event.getTime(),
					(Leg) data.getFirst().getPlanElements().get(index));
		}
	}

	/**
	 * set Attr. plans of a person. This method should be called after
	 * removedPlans, i.e. there should be only choiceSetSize plans in the memory
	 * of an agent.
	 *
	 * @param person
	 */
	@Override
	public void setPersonAttrs(Person person, BasicStatistics[] statistics) {
		Id agentId = person.getId();
		Map<Plan, Double> legDurMapCar = legDursCar.get(agentId), legDurMapPt = legDursPt
				.get(agentId), legDurMapWalk = legDursWalk.get(agentId), perfAttrMap = actAttrs
				.get(agentId), stuckAttrMap = stuckAttrs.get(agentId), distanceMapCar = distancesCar
				.get(agentId), distanceMapPt = distancesPt.get(agentId), distanceMapWalk = distancesWalk
				.get(agentId);
		Map<Plan, Integer> carLegNoMap = carLegNos.get(agentId), ptLegNoMap = ptLegNos
				.get(agentId), walkLegNoMap = walkLegNos.get(agentId);
		// nullpoint check
		if (legDurMapCar == null || legDurMapPt == null
				|| legDurMapWalk == null

				|| perfAttrMap == null || stuckAttrMap == null

				|| distanceMapCar == null || distanceMapPt == null
				|| distanceMapWalk == null

				|| carLegNoMap == null || ptLegNoMap == null
				|| walkLegNoMap == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + agentId
					+ "\tsimulated?????");
		}

		List<? extends Plan> plans = person.getPlans();
		// if (plans.size() <= this.mnl.getChoiceSetSize()) {
		for (Plan plan : legDurMapCar.keySet()) {
			Double legDurCar = legDurMapCar.get(plan), legDurPt = legDurMapPt
					.get(plan), legDurWalk = legDurMapWalk.get(plan),

			perfAttr = perfAttrMap.get(plan), stuckAttr = stuckAttrMap
					.get(plan),

			distanceCar = distanceMapCar.get(plan), distancePt = distanceMapPt
					.get(plan), distanceWalk = distanceMapWalk.get(plan);
			Integer carLegNo = carLegNoMap.get(plan), ptLegNo = ptLegNoMap
					.get(plan), walkLegNo = walkLegNoMap.get(plan);

			// nullpoint check
			if (legDurCar == null || legDurPt == null || legDurWalk == null

			|| perfAttr == null || stuckAttr == null

			|| distanceCar == null || distancePt == null
					|| distanceWalk == null

					|| carLegNo == null || ptLegNo == null || walkLegNo == null) {
				throw new NullPointerException(
						"BSE:\t\tfergot to save some attr?");
			}

			// NaN check
			if (Double.isNaN(legDurCar) || Double.isNaN(legDurPt)
					|| Double.isNaN(legDurWalk)

					|| Double.isNaN(perfAttr) || Double.isNaN(stuckAttr)

					|| Double.isNaN(distanceCar) || Double.isNaN(distancePt)
					|| Double.isNaN(distanceWalk)

					|| Double.isNaN(carLegNo) || Double.isNaN(ptLegNo)
					|| Double.isNaN(walkLegNo)) {
				log.warn("\tNaN Exception:\nattr of traveling\t" + legDurCar
						+ "\nattr of travelingPt\t" + legDurPt
						+ "\nattr of travelingWalk\t" + legDurWalk

						+ "\nattr of stuck\t" + stuckAttr
						+ "\nattr of performing\t" + perfAttr

						+ "\ncar distance\t" + distanceCar + "\npt distance\t"
						+ distancePt + "\nwalk distance\t" + distanceWalk

						+ "\ncar leg No.\t" + carLegNo + "\npt leg No.\t"
						+ ptLegNo + "\nwalk leg No.\t" + walkLegNo

				);
				throw new RuntimeException();
				// ///////////////////////////////////////////////////////////////
			}
			int choiceIdx = plans.indexOf(plan);
			if (plans.size() <= maxPlansPerAgent)/* with mnl */{
				// choice set index & size check
				if (choiceIdx < 0 || choiceIdx >= mnl.getChoiceSetSize()) {
					log.warn("IndexOutofBound, choiceIdx<0 or >=choiceSetSize!\nperson "
							+ agentId + " the " + choiceIdx + ". Plan");
					throw new RuntimeException();
				}

				// set attributes to MultinomialLogit
				// travelTime
				int attrNameIndex = attrNameList.indexOf("traveling");
				mnl.setAttribute(choiceIdx, attrNameIndex, legDurCar
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList.indexOf("travelingPt");
				mnl.setAttribute(choiceIdx, attrNameIndex, legDurPt
						/ paramScaleFactorList.get(attrNameIndex));
				if (statistics != null) {
					for (int i = 0; i < statistics.length; i++) {
						statistics[i].add(legDurPt
								/ paramScaleFactorList.get(attrNameIndex));
					}
				}

				attrNameIndex = attrNameList.indexOf("travelingWalk");
				mnl.setAttribute(choiceIdx, attrNameIndex, legDurWalk
						/ paramScaleFactorList.get(attrNameIndex));

				//
				attrNameIndex = attrNameList.indexOf("performing");
				mnl.setAttribute(choiceIdx, attrNameIndex, perfAttr
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList.indexOf("stuck");
				mnl.setAttribute(choiceIdx, attrNameIndex, stuckAttr
						/ paramScaleFactorList.get(attrNameIndex));

				// distances
				attrNameIndex = attrNameList
						.indexOf("monetaryDistanceCostRateCar");
				mnl.setAttribute(choiceIdx, attrNameIndex, distanceCar
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf("monetaryDistanceCostRatePt");
				mnl.setAttribute(choiceIdx, attrNameIndex, distancePt
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf("marginalUtlOfDistanceWalk");
				mnl.setAttribute(choiceIdx, attrNameIndex, distanceWalk
						/ paramScaleFactorList.get(attrNameIndex));

				// offsets
				attrNameIndex = attrNameList.indexOf("constantCar");
				mnl.setAttribute(choiceIdx, attrNameIndex, carLegNo
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList.indexOf("constantPt");
				mnl.setAttribute(choiceIdx, attrNameIndex, ptLegNo
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList.indexOf("constantWalk");
				mnl.setAttribute(choiceIdx, attrNameIndex, walkLegNo
						/ paramScaleFactorList.get(attrNameIndex));

				// ##########################################################
				/*
				 * ASC (utilityCorrection, ASC for "stay home" Plan in the
				 * future...)
				 */
				// #############################################

				Object uc = plan.getCustomAttributes().get(
						BseStrategyManager.UTILITY_CORRECTION);
				mnl.setASC(choiceIdx, uc != null ? (Double) uc : 0d);
			}
		}
	}

	@Override
	public void setPersonScore(Person person) {
		Id agentId = person.getId();
		Map<Plan, Double> legDurMapCar = legDursCar.get(agentId), legDurMapPt = legDursPt
				.get(agentId), legDurMapWalk = legDursWalk.get(agentId),

		actAttrMap = actAttrs.get(agentId), stuckAttrMap = stuckAttrs
				.get(agentId),

		distanceMapCar = distancesCar.get(agentId), distanceMapPt = distancesPt
				.get(agentId), distanceMapWalk = distancesWalk.get(agentId);
		Map<Plan, Integer> carLegNoMap = carLegNos.get(agentId), ptLegNoMap = ptLegNos
				.get(agentId), walkLegNoMap = walkLegNos.get(agentId);
		// nullpoint check
		if (legDurMapCar == null || legDurMapPt == null
				|| legDurMapWalk == null

				|| actAttrMap == null || stuckAttrMap == null

				|| distanceMapCar == null || distanceMapPt == null
				|| distanceMapWalk == null

				|| carLegNoMap == null || ptLegNoMap == null
				|| walkLegNoMap == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + agentId
					+ "\tsimulated?????");
		}

		for (Plan plan : legDurMapCar.keySet()) {
			Double legDurCar = legDurMapCar.get(plan), legDurPt = legDurMapPt
					.get(plan), legDurWalk = legDurMapWalk.get(plan),

			perfAttr = actAttrMap.get(plan), stuckAttr = stuckAttrMap.get(plan),

			distanceCar = distanceMapCar.get(plan), distancePt = distanceMapPt
					.get(plan), distanceWalk = distanceMapWalk.get(plan);
			Integer carLegNo = carLegNoMap.get(plan), ptLegNo = ptLegNoMap
					.get(plan), walkLegNo = walkLegNoMap.get(plan);
			// nullpoint check
			if (legDurCar == null || legDurPt == null || legDurWalk == null

			|| perfAttr == null || stuckAttr == null

			|| distanceCar == null || distancePt == null
					|| distanceWalk == null

					|| carLegNo == null || ptLegNo == null || walkLegNo == null) {
				throw new NullPointerException(
						"BSE:\t\tforgot to save some attr?");
			}
			// NaN check
			if (Double.isNaN(legDurCar) || Double.isNaN(legDurPt)
					|| Double.isNaN(legDurWalk)

					|| Double.isNaN(perfAttr) || Double.isNaN(stuckAttr)

					|| Double.isNaN(distanceCar) || Double.isNaN(distancePt)
					|| Double.isNaN(distanceWalk)

					|| Double.isNaN(carLegNo) || Double.isNaN(ptLegNo)
					|| Double.isNaN(walkLegNo)) {
				log.warn("\tNaN Exception:\nattr of traveling\t" + legDurCar
						+ "\nattr of travelingPt\t" + legDurPt
						+ "\nattr of travelingWalk\t" + legDurWalk

						+ "\nattr of stuck\t" + stuckAttr
						+ "\nattr of performing\t" + perfAttr

						+ "\ncar distance\t" + distanceCar + "\npt distance\t"
						+ distancePt + "\nwalk distance\t" + distanceWalk

						+ "\ncar leg No.\t" + carLegNo + "\npt leg No.\t"
						+ ptLegNo + "\nwalk leg No.\t" + walkLegNo

				);
				throw new RuntimeException();
				// ///////////////////////////////////////////////////////////////
			}
			// calculate utility of the plan
			Vector attrVector = new Vector(attrNameList.size());
			for (String attrName : attrNameList) {
				int attrNameIndex;
				if (attrName.equals("traveling")) {
					attrNameIndex = attrNameList.indexOf("traveling");
					attrVector.set(attrNameIndex, legDurCar
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("travelingPt")) {
					attrNameIndex = attrNameList.indexOf("travelingPt");
					attrVector.set(attrNameIndex, legDurPt
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("travelingWalk")) {
					attrNameIndex = attrNameList.indexOf("travelingWalk");
					attrVector.set(attrNameIndex, legDurWalk
							/ paramScaleFactorList.get(attrNameIndex));
				}

				else if (attrName.equals("performing")) {
					attrNameIndex = attrNameList.indexOf("performing");
					attrVector.set(attrNameIndex, perfAttr
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("stuck")) {
					attrNameIndex = attrNameList.indexOf("stuck");
					attrVector.set(attrNameIndex, stuckAttr
							/ paramScaleFactorList.get(attrNameIndex));
				}

				else if (attrName.equals("monetaryDistanceCostRateCar")) {
					attrNameIndex = attrNameList
							.indexOf("monetaryDistanceCostRateCar");
					attrVector.set(attrNameIndex, distanceCar
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("monetaryDistanceCostRatePt")) {
					attrNameIndex = attrNameList
							.indexOf("monetaryDistanceCostRatePt");
					attrVector.set(attrNameIndex, distancePt
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("marginalUtlOfDistanceWalk")) {
					attrNameIndex = attrNameList
							.indexOf("marginalUtlOfDistanceWalk");
					attrVector.set(attrNameIndex, distanceWalk
							/ paramScaleFactorList.get(attrNameIndex));

				}

				else if (attrName.equals("constantCar")) {
					attrNameIndex = attrNameList.indexOf("constantCar");
					attrVector.set(attrNameIndex, carLegNo
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("constantPt")) {
					attrNameIndex = attrNameList.indexOf("constantPt");
					attrVector.set(attrNameIndex, ptLegNo
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals("constantWalk")) {
					attrNameIndex = attrNameList.indexOf("constantWalk");
					attrVector.set(attrNameIndex, walkLegNo
							/ paramScaleFactorList.get(attrNameIndex));
				}
			}

			Object uc = plan.getCustomAttributes().get(
					BseStrategyManager.UTILITY_CORRECTION);
			double utilCorrection = uc != null ? (Double) uc : 0d;

			Vector coeff = mnl.getCoeff();
			double util = coeff/*
								 * s. the attributes order in
								 * Events2Score4PC2.attrNameList
								 */
			.innerProd(attrVector) + utilCorrection
			/* utilityCorrection is also an important ASC */;
			plan.setScore(util);
			if (writer != null) {
				writer.writeln("/////CALC-DETAILS of PERSON\t" + person.getId()
						+ "\t////////////////\n/////coeff\tattr");
				for (int i = 0; i < coeff.size(); i++) {
					writer.writeln("/////\t" + coeff.get(i) + "\t"
							+ attrVector.get(i) + "\t/////");
				}
				writer.writeln("/////\tUtiliy Correction\t=\t" + utilCorrection
						+ "\t/////\n/////\tscore before replanning\t=\t" + util
						+ "\t/////");
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Route route = ((Leg) pe).getRoute();
						if (route instanceof NetworkRoute) {
							writer.write("/////\tRoute :\t"
									+ ((NetworkRoute) route).getLinkIds()
									+ "\t");
							if (plan.isSelected()) {
								writer.write("selected");
							}
							writer.writeln("/////\n");
						}
						break;
					}
				}
				writer.writeln("///////////////////////////////////////");
				writer.flush();
			}
		}
	}

	/**
	 * should be called after that all setPersonScore(Person) have been called
	 * in every iteration.
	 */
	public void closeWriter() {
		if (writer != null) {
			writer.close();
		}
	}

	public void createWriter() {

		ControlerConfigGroup ctlCfg = config.controler();
		if (iteration <= ctlCfg.getLastIteration()
				&& iteration > ctlCfg.getLastIteration() - 100) {
			// outputCalcDetail = true;
			ControlerIO ctlIO = new ControlerIO(ctlCfg.getOutputDirectory());
			writer = new SimpleWriter(ctlIO.getIterationFilename(iteration,
					"scoreCalcDetails.log.gz"));

			StringBuilder head = new StringBuilder("AgentID");
			for (String attrName : attrNameList) {
				head.append("\t");
				head.append(attrName);
			}
			head.append("\tselected");
			writer.writeln(head);

		}
	}
}
