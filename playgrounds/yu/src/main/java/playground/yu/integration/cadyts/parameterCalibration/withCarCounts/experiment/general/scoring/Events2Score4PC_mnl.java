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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.scoring;

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

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.paramCorrection.PCCtlListener;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
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
	 * only choiceSetSize plans in the memory of an agent.
	 * 
	 * @param person
	 */
	@Override
	public void setPersonAttrs(Person person) {
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
					log
							.warn("IndexOutofBound, choiceIdx<0 or >=choiceSetSize!\nperson "
									+ agentId + " the " + choiceIdx + ". Plan");
					throw new RuntimeException();
				}

				// set attributes to MultinomialLogit
				// travelTime
				mnl.setAttribute(choiceIdx, attrNameList.indexOf("traveling"),
						legDurCar);
				mnl.setAttribute(choiceIdx,
						attrNameList.indexOf("travelingPt"), legDurPt);
				mnl.setAttribute(choiceIdx, attrNameList
						.indexOf("travelingWalk"), legDurWalk);

				//
				mnl.setAttribute(choiceIdx, attrNameList.indexOf("performing"),
						perfAttr);

				mnl.setAttribute(choiceIdx, attrNameList.indexOf("stuck"),
						stuckAttr);

				// distances
				mnl.setAttribute(choiceIdx, attrNameList
						.indexOf("monetaryDistanceCostRateCar"), distanceCar);
				mnl.setAttribute(choiceIdx, attrNameList
						.indexOf("monetaryDistanceCostRatePt"), distancePt);
				mnl.setAttribute(choiceIdx, attrNameList
						.indexOf("marginalUtlOfDistanceWalk"), distanceWalk);

				// offsets
				mnl.setAttribute(choiceIdx, attrNameList.indexOf("offsetCar"),
						carLegNo);
				mnl.setAttribute(choiceIdx, attrNameList.indexOf("offsetPt"),
						ptLegNo);
				mnl.setAttribute(choiceIdx, attrNameList.indexOf("offsetWalk"),
						walkLegNo);
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
				if (attrName.equals("traveling")) {
					attrVector
							.set(attrNameList.indexOf("traveling"), legDurCar);
				} else if (attrName.equals("travelingPt")) {
					attrVector.set(attrNameList.indexOf("travelingPt"),
							legDurPt);
				} else if (attrName.equals("travelingWalk")) {
					attrVector.set(attrNameList.indexOf("travelingWalk"),
							legDurWalk);
				}

				else if (attrName.equals("performing")) {
					attrVector
							.set(attrNameList.indexOf("performing"), perfAttr);
				} else if (attrName.equals("stuck")) {
					attrVector.set(attrNameList.indexOf("stuck"), stuckAttr);
				}

				else if (attrName.equals("monetaryDistanceCostRateCar")) {
					attrVector.set(attrNameList
							.indexOf("monetaryDistanceCostRateCar"),
							distanceCar);
				} else if (attrName.equals("monetaryDistanceCostRatePt")) {
					attrVector.set(attrNameList
							.indexOf("monetaryDistanceCostRatePt"), distancePt);
				} else if (attrName.equals("marginalUtlOfDistanceWalk")) {
					attrVector
							.set(attrNameList
									.indexOf("marginalUtlOfDistanceWalk"),
									distanceWalk);

				}

				else if (attrName.equals("offsetCar")) {
					attrVector.set(attrNameList.indexOf("offsetCar"), carLegNo);
				} else if (attrName.equals("offsetPt")) {
					attrVector.set(attrNameList.indexOf("offsetPt"), ptLegNo);
				} else if (attrName.equals("offsetWalk")) {
					attrVector.set(attrNameList.indexOf("offsetWalk"),
							walkLegNo);
				}
			}

			double util = mnl
					.getCoeff()
					/* s. the attributes order in Events2Score4PC.attrNameList */.innerProd(
							attrVector);
			plan.setScore(util);
		}
	}

	private MultinomialLogit createMultinomialLogit(Config config) {
		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), // =4
		attributeCount = Integer.parseInt(config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "attributeCount"));

		PlanCalcScoreConfigGroup scoring = config.charyparNagelScoring();
		double traveling = scoring.getTraveling_utils_hr();
		double betaStuck = Math.min(Math.min(scoring.getLateArrival_utils_hr(),
				scoring.getEarlyDeparture_utils_hr()), Math.min(traveling,
				scoring.getWaiting_utils_hr()));

		// initialize MultinomialLogit
		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize,// =4
				attributeCount);// =5 [travCar,travPt,travWalk,Perf,Stuck]

		mnl.setUtilityScale(scoring.getBrainExpBeta());

		for (int i = 0; i < choiceSetSize; i++) {
			mnl.setASC(i, 0);
		}
		// travelTime
		mnl.setCoefficient(attrNameList.indexOf("traveling"), traveling);
		mnl.setCoefficient(attrNameList.indexOf("travelingPt"), scoring
				.getTravelingPt_utils_hr());
		mnl.setCoefficient(attrNameList.indexOf("travelingWalk"), scoring
				.getTravelingWalk_utils_hr());

		//
		mnl.setCoefficient(attrNameList.indexOf("performing"), scoring
				.getPerforming_utils_hr());
		//
		mnl.setCoefficient(attrNameList.indexOf("stuck"), betaStuck);

		// distances
		mnl.setCoefficient(attrNameList.indexOf("monetaryDistanceCostRateCar"),
		// scoring.getMarginalUtlOfDistanceCar());
				scoring.getMonetaryDistanceCostRateCar()
						* scoring.getMarginalUtilityOfMoney());

		mnl.setCoefficient(attrNameList.indexOf("monetaryDistanceCostRatePt"),
		// scoring.getMarginalUtlOfDistancePt());
				scoring.getMonetaryDistanceCostRatePt()
						* scoring.getMarginalUtilityOfMoney());

		mnl.setCoefficient(attrNameList.indexOf("marginalUtlOfDistanceWalk"),
				scoring.getMarginalUtlOfDistanceWalk());

		// offsets
		String offsetCarStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "offsetCar");
		mnl.setCoefficient(attrNameList.indexOf("offsetCar"),
				offsetCarStr == null ? 0d : Double.parseDouble(offsetCarStr));

		String offsetPtStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "offsetPt");
		mnl.setCoefficient(attrNameList.indexOf("offsetPt"),
				offsetPtStr == null ? 0d : Double.parseDouble(offsetPtStr));

		String offsetWalkStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "offsetWalk");
		mnl.setCoefficient(attrNameList.indexOf("offsetWalk"),
				offsetWalkStr == null ? 0d : Double.parseDouble(offsetWalkStr));

		return mnl;
	}
}
