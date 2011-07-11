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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

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
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection.BseParamCalibrationControlerListener;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import cadyts.utilities.math.BasicStatistics;

/**
 * @author yu
 *
 */
public abstract class Events2Score4PC2 extends EventsToScore implements
		CadytsChoice {
	public static List<String> attrNameList = new ArrayList<String>();
	public static List<Double> paramScaleFactorList = new ArrayList<Double>();

	private static final String PARAM_SCALE_FACTOR_INDEX = "paramScaleFactor_";

	private final Config config;
	/** Map<personId,Map<Plan,attr>> */
	// VVVVVVVVVVVVVVVVVVVVVVVVVV ATTRIBUTES VVVVVVVVVVVVVVVVVVVVVVVVVVV
	protected Map<Id/* agent */, Map<Plan, Double>> travTimeAttrCars/* legDuration */= new HashMap<Id, Map<Plan, Double>>(),
			perfAttrs/* Acitivity attr. */= new HashMap<Id, Map<Plan, Double>>(),
			lnPathSizeAttrs/* Ln(PSi) */= new HashMap<Id, Map<Plan, Double>>();

	protected Map<Id/* agent */, Map<Plan, Integer>> speedBumpNbAttrs = new HashMap<Id, Map<Plan, Integer>>(),
			leftTurnNbAttrs = new HashMap<Id, Map<Plan, Integer>>(),
			intersectionNbAttrs = new HashMap<Id, Map<Plan, Integer>>();
	// AAAAAAAAAAAAAAAAAAAAAAAAAA ATTRIBUTES AAAAAAAAAAAAAAAAAAAAAAAAAAA
	protected Population pop = null;
	protected ScoringFunctionFactory sfFactory = null;
	protected PlanCalcScoreConfigGroup scoring;
	// protected boolean setPersonScore = true;
	protected int maxPlansPerAgent;
	protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
	protected final TreeMap<Id/* agent */, Integer/* idx */> agentPlanElementIndex = new TreeMap<Id, Integer>();

	public Events2Score4PC2(Config config, ScoringFunctionFactory factory,
			Population population) {
		super(population, factory, config.planCalcScore().getLearningRate());
		this.config = config;
		// #####################################
		// travelTime
		initialAttrNameScaleFactor("traveling");

		// actPerforming
		initialAttrNameScaleFactor("performing");

		// ln(PSi)
		initialAttrNameScaleFactor("betaLnPathSize");

		// speedBumpNb
		initialAttrNameScaleFactor("betaSpeedBumpNb");

		// leftTurnNb
		initialAttrNameScaleFactor("betaLeftTurnNb");

		// intersectionNb
		initialAttrNameScaleFactor("betaIntersectionNb");

		// initialAttrNameScaleFactor("lateArrival");//TODO in
		// ActivityScoringFunction4PC in the furture

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
	 */
	@Override
	public abstract void setPersonAttrs(Person person, BasicStatistics[] stats);

	@Override
	public abstract void setPersonScore(Person person);

	@Override
	public void reset(List<Tuple<Id, Plan>> toRemoves) {
		for (Tuple<Id, Plan> agentIdPlanPair : toRemoves) {
			Id agentId = agentIdPlanPair.getFirst();
			Plan plan = agentIdPlanPair.getSecond();

			Map<Plan, Double> travTimeAttrCar = travTimeAttrCars.get(agentId), Perfattr = perfAttrs
					.get(agentId), lnPathSizeAttr = lnPathSizeAttrs
					.get(agentId);
			Map<Plan, Integer> speedBumpNbAttr = speedBumpNbAttrs.get(agentId), leftTurnNbAttr = leftTurnNbAttrs
					.get(agentId), intersectionNbAttr = intersectionNbAttrs
					.get(agentId);

			if (travTimeAttrCar == null || Perfattr == null
					|| lnPathSizeAttr == null || speedBumpNbAttr == null
					|| leftTurnNbAttr == null || intersectionNbAttr == null) {
				throw new NullPointerException("BSE:\t\twasn't person\t"
						+ agentId + "\tsimulated?????");
			}

			travTimeAttrCar.remove(plan);
			Perfattr.remove(plan);
			lnPathSizeAttr.remove(plan);
			speedBumpNbAttr.remove(plan);
			leftTurnNbAttr.remove(plan);
			intersectionNbAttr.remove(plan);
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
	 * {@code ???PlansScoring4PC.notifyScoring(ScoringEvent)}
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
			ScoringFunctionAccumulator4PC2 sfa = (ScoringFunctionAccumulator4PC2) sf;

			// legTravTimeCar
			Map<Plan, Double> travTimeAttrCar = travTimeAttrCars.get(agentId);
			if (travTimeAttrCar == null) {
				travTimeAttrCar = new HashMap<Plan, Double>();
				travTimeAttrCars.put(agentId, travTimeAttrCar);
			}
			travTimeAttrCar.put(plan, sfa.getTravTimeAttrCar());

			// actAttr
			Map<Plan, Double> perfAttr = perfAttrs.get(agentId);
			if (perfAttr == null) {
				perfAttr = new HashMap<Plan, Double>();
				perfAttrs.put(agentId, perfAttr);
			}
			perfAttr.put(plan, sfa.getPerfAttr());

			// ln(PSi)
			Map<Plan, Double> lnPathSizeAttr = lnPathSizeAttrs.get(agentId);
			if (lnPathSizeAttr == null) {
				lnPathSizeAttr = new HashMap<Plan, Double>();
				lnPathSizeAttrs.put(agentId, lnPathSizeAttr);
			}
			lnPathSizeAttr.put(plan, sfa.getLnPathSizeAttr());

			// speedBumpNb
			Map<Plan, Integer> speedBumpNbAttr = speedBumpNbAttrs.get(agentId);
			if (speedBumpNbAttr == null) {
				speedBumpNbAttr = new HashMap<Plan, Integer>();
				speedBumpNbAttrs.put(agentId, speedBumpNbAttr);
			}
			speedBumpNbAttr.put(plan, sfa.getNbSpeedBumps());

			// leftTurnNb
			Map<Plan, Integer> leftTurnNbAttr = leftTurnNbAttrs.get(agentId);
			if (leftTurnNbAttr == null) {
				leftTurnNbAttr = new HashMap<Plan, Integer>();
				leftTurnNbAttrs.put(agentId, leftTurnNbAttr);
			}
			leftTurnNbAttr.put(plan, sfa.getNbLeftTurns());

			// intersectionNb
			Map<Plan, Integer> intersectionNbAttr = intersectionNbAttrs
					.get(agentId);
			if (intersectionNbAttr == null) {
				intersectionNbAttr = new HashMap<Plan, Integer>();
				intersectionNbAttrs.put(agentId, intersectionNbAttr);
			}
			intersectionNbAttr.put(plan, sfa.getNbIntersections());

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

		agentPlanElementIndex.put(personId, Integer.valueOf(1 + index));
		return 1 + index;
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
}
