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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalStayHomePlan.scoring;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.demandModifications.StayHomePlan;
import playground.yu.demandModifications.StayHomePlanASC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
import cadyts.utilities.math.BasicStatistics;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

/**
 * @author yu
 *
 */
public class Events2Score4PC_mnl extends Events2Score4PC implements
		MultinomialLogitChoice {

	private final static Logger log = Logger
			.getLogger(Events2Score4PC_mnl.class);

	protected MultinomialLogit mnl;

	public Events2Score4PC_mnl(Config config, ScoringFunctionFactory sfFactory,
			Population pop) {
		super(config, sfFactory, pop);
		mnl = createMultinomialLogit(config);
	}

	@Override
	public MultinomialLogit getMultinomialLogit() {
		return mnl;
	}

	public void setMultinomialLogit(MultinomialLogit mnl) {
		this.mnl = mnl;
	}

	/**
	 * set Attr. and Utility (not the score in MATSim) of plans of a person.
	 * This method should be called after removedPlans, i.e. there should be
	 * only choiceSetSize plans in the memory of an agent. - Caution!!! this
	 * method should be called after calling of setPersonScore(Person)
	 *
	 * @param person
	 */
	@Override
	public void setPersonAttrs(Person person, BasicStatistics[] stats) {
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
				 * ASC (utilityCorrection, ASC for "stay home" Plan, ...)
				 */
				// #############################################
				Map<String, Object> customAttrs = plan.getCustomAttributes();
				mnl.setASC(
						choiceIdx,
						(Double) customAttrs
								.get(BseStrategyManager.UTILITY_CORRECTION)
								+ (!StayHomePlan.isAStayHomePlan(plan) ? 0d
										: (Double) customAttrs
												.get(StayHomePlanASC.STAY_HOME_ASC)));
			}
		}
	}

	/**
	 * this method should be called before calling setPersonAttrs(Person)
	 */
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
						+ ptLegNo + "\nwalk leg No.\t" + walkLegNo);
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

			Map<String, Object> customAttrs = plan.getCustomAttributes();
			double util = mnl.getCoeff()/*
										 * s. the attributes order in
										 * Events2Score4PC2.attrNameList
										 */.innerProd(attrVector)
					+ (Double) customAttrs
							.get(BseStrategyManager.UTILITY_CORRECTION)
					+ (!StayHomePlan.isAStayHomePlan(plan) ? 0d
							: (Double) customAttrs
									.get(StayHomePlanASC.STAY_HOME_ASC));
			plan.setScore(util);
		}// end for
	}

	private MultinomialLogit createMultinomialLogit(Config config) {
		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), // =4
		attributeCount = Integer.parseInt(config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"attributeCount"));

		PlanCalcScoreConfigGroup scoring = config.planCalcScore();
		double traveling = scoring.getTraveling_utils_hr();
		double betaStuck = Math.min(
				Math.min(scoring.getLateArrival_utils_hr(),
						scoring.getEarlyDeparture_utils_hr()),
				Math.min(traveling, scoring.getWaiting_utils_hr()));

		// initialize MultinomialLogit
		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize,// =4
				attributeCount);// =5 [travCar,travPt,travWalk,Perf,Stuck]

		mnl.setUtilityScale(scoring.getBrainExpBeta());

		for (int i = 0; i < choiceSetSize; i++) {
			mnl.setASC(i, 0);
		}
		// travelTime
		int attrNameIndex = attrNameList.indexOf("traveling");
		mnl.setCoefficient(attrNameIndex,
				traveling * paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("travelingPt");
		mnl.setCoefficient(attrNameIndex, scoring.getTravelingPt_utils_hr()
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("travelingWalk");
		mnl.setCoefficient(attrNameIndex, scoring.getTravelingWalk_utils_hr()
				* paramScaleFactorList.get(attrNameIndex));

		//
		attrNameIndex = attrNameList.indexOf("performing");
		mnl.setCoefficient(attrNameIndex, scoring.getPerforming_utils_hr()
				* paramScaleFactorList.get(attrNameIndex));
		//
		attrNameIndex = attrNameList.indexOf("stuck");
		mnl.setCoefficient(attrNameIndex,
				betaStuck * paramScaleFactorList.get(attrNameIndex));

		// distances
		attrNameIndex = attrNameList.indexOf("monetaryDistanceCostRateCar");
		mnl.setCoefficient(
				attrNameIndex,
				scoring.getMonetaryDistanceCostRateCar()
						* scoring.getMarginalUtilityOfMoney()
						* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("monetaryDistanceCostRatePt");
		mnl.setCoefficient(
				attrNameIndex,
				scoring.getMonetaryDistanceCostRatePt()
						* scoring.getMarginalUtilityOfMoney()
						* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("marginalUtlOfDistanceWalk");
		mnl.setCoefficient(
				attrNameIndex,
				scoring.getMarginalUtlOfDistanceWalk()
						* paramScaleFactorList.get(attrNameIndex));

		// constants
		attrNameIndex = attrNameList.indexOf("constantCar");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantCar()
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("constantPt");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantPt()
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf("constantWalk");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantWalk()
				* paramScaleFactorList.get(attrNameIndex));

		return mnl;
	}
}
