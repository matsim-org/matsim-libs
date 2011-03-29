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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general2;

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

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.paramCorrection.BseParamCalibrationControlerListener;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

/**
 * @author yu
 * 
 */
public class Events2Score4PC_mnl2 extends Events2Score4PC2 implements
		MultinomialLogitChoice {

	private final static Logger log = Logger
			.getLogger(Events2Score4PC_mnl2.class);

	protected MultinomialLogit mnl;

	public Events2Score4PC_mnl2(Config config,
			ScoringFunctionFactory sfFactory, Population pop) {
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

		Map<Plan, Double> travTimeAttrCar = travTimeAttrCars.get(agentId), perfAttr = perfAttrs
				.get(agentId), lnPathSizeAttr = lnPathSizeAttrs.get(agentId);

		Map<Plan, Integer> speedBumpNbAttr = speedBumpNbAttrs.get(agentId), leftTurnNbAttr = leftTurnNbAttrs
				.get(agentId), intersectionNbAttr = intersectionNbAttrs
				.get(agentId);

		// nullpoint check
		if (travTimeAttrCar == null || perfAttr == null
				|| lnPathSizeAttr == null || speedBumpNbAttr == null
				|| leftTurnNbAttr == null || intersectionNbAttr == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + agentId
					+ "\tsimulated?????");
		}

		List<? extends Plan> plans = person.getPlans();
		// if (plans.size() <= this.mnl.getChoiceSetSize()) {
		for (Plan plan : travTimeAttrCar.keySet()) {
			Double travTime = travTimeAttrCar.get(plan), perf = perfAttr
					.get(plan), lnPathSize = lnPathSizeAttr.get(plan);

			Integer speedBumpNb = speedBumpNbAttr.get(plan), leftTurnNb = leftTurnNbAttr
					.get(plan), intersectionNb = intersectionNbAttr.get(plan);

			// nullpoint check
			if (travTime == null || perf == null || lnPathSize == null
					|| speedBumpNb == null || leftTurnNb == null
					|| intersectionNb == null) {
				throw new NullPointerException(
						"BSE:\t\tfergot to save some attr?");
			}

			// NaN check
			if (Double.isNaN(travTime) || Double.isNaN(perf)
					|| Double.isNaN(lnPathSize) || Double.isNaN(speedBumpNb)
					|| Double.isNaN(leftTurnNb) || Double.isNaN(intersectionNb)) {
				throw new RuntimeException(
						"\tNaN Exception:\nattr of traveling\t" + travTime
								+ "\nattr of performing\t" + perf
								+ "\nattr of betaLnPathSize\t" + lnPathSize
								+ "\nnb of betaSpeedBumpNb\t" + speedBumpNb
								+ "\nnb of betaLeftTurnNb\t" + leftTurnNb
								+ "\nnb of betaIntersectionNb\t"
								+ intersectionNb);
			}

			int choiceIdx = plans.indexOf(plan);
			if (plans.size() <= maxPlansPerAgent)/* with mnl */{
				// choice set index & size check
				if (choiceIdx < 0 || choiceIdx >= mnl.getChoiceSetSize()) {
					throw new IndexOutOfBoundsException(
							"IndexOutofBound, choiceIdx<0 or >=choiceSetSize!\nperson "
									+ agentId + " the " + choiceIdx + ". Plan");
				}

				// ######set attributes to MultinomialLogit#########
				// travelTime
				int attrNameIndex = attrNameList
						.indexOf(ScoringParameters.TRAVELING);
				mnl.setAttribute(choiceIdx, attrNameIndex, travTime
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf(ScoringParameters.PERFORMING);
				mnl.setAttribute(choiceIdx, attrNameIndex, perf
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf(ScoringParameters.BETA_LN_PATH_SIZE);
				mnl.setAttribute(choiceIdx, attrNameIndex, lnPathSize
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf(ScoringParameters.BETA_SPEED_BUMP_NB);
				mnl.setAttribute(choiceIdx, attrNameIndex, (double) speedBumpNb
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf(ScoringParameters.BETA_LEFT_TURN_NB);
				mnl.setAttribute(choiceIdx, attrNameIndex, (double) leftTurnNb
						/ paramScaleFactorList.get(attrNameIndex));

				attrNameIndex = attrNameList
						.indexOf(ScoringParameters.BETA_INTERSECTION_NB);
				mnl.setAttribute(choiceIdx, attrNameIndex,
						(double) intersectionNb
								/ paramScaleFactorList.get(attrNameIndex));
			}
		}
	}

	@Override
	public void setPersonScore(Person person) {
		Id agentId = person.getId();
		Map<Plan, Double> travTimeAttrCar = travTimeAttrCars.get(agentId), perfAttr = perfAttrs
				.get(agentId), lnPathSizeAttr = lnPathSizeAttrs.get(agentId);

		Map<Plan, Integer> speedBumpNbAttr = speedBumpNbAttrs.get(agentId), leftTurnNbAttr = leftTurnNbAttrs
				.get(agentId), intersectionNbAttr = intersectionNbAttrs
				.get(agentId);

		// nullpoint check
		if (travTimeAttrCar == null || perfAttr == null
				|| lnPathSizeAttr == null || speedBumpNbAttr == null
				|| leftTurnNbAttr == null || intersectionNbAttr == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + agentId
					+ "\tsimulated?????");
		}

		for (Plan plan : travTimeAttrCar.keySet()) {
			Double travTime = travTimeAttrCar.get(plan), perf = perfAttr
					.get(plan), lnPathSize = lnPathSizeAttr.get(plan);

			Integer speedBumpNb = speedBumpNbAttr.get(plan), leftTurnNb = leftTurnNbAttr
					.get(plan), intersectionNb = intersectionNbAttr.get(plan);

			// nullpoint check
			if (travTime == null || perf == null || lnPathSize == null
					|| speedBumpNb == null || leftTurnNb == null
					|| intersectionNb == null) {
				throw new NullPointerException(
						"BSE:\t\tfergot to save some attr?");
			}

			// NaN check
			if (Double.isNaN(travTime) || Double.isNaN(perf)
					|| Double.isNaN(lnPathSize) || Double.isNaN(speedBumpNb)
					|| Double.isNaN(leftTurnNb) || Double.isNaN(intersectionNb)) {
				throw new RuntimeException(
						"\tNaN Exception:\nattr of traveling\t" + travTime
								+ "\nattr of performing\t" + perf
								+ "\nattr of betaLnPathSize\t" + lnPathSize
								+ "\nnb of betaSpeedBumpNb\t" + speedBumpNb
								+ "\nnb of betaLeftTurnNb\t" + leftTurnNb
								+ "\nnb of betaIntersectionNb\t"
								+ intersectionNb);
			}

			// calculate utility of the plan
			Vector attrVector = new Vector(attrNameList.size());
			for (String attrName : attrNameList) {
				int attrNameIndex;
				if (attrName.equals(ScoringParameters.TRAVELING)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.TRAVELING);
					attrVector.set(attrNameIndex, travTime
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals(ScoringParameters.PERFORMING)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.PERFORMING);
					attrVector.set(attrNameIndex, perf
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals(ScoringParameters.BETA_LN_PATH_SIZE)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.BETA_LN_PATH_SIZE);
					attrVector.set(attrNameIndex, lnPathSize
							/ paramScaleFactorList.get(attrNameIndex));
				}

				else if (attrName.equals(ScoringParameters.BETA_SPEED_BUMP_NB)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.BETA_SPEED_BUMP_NB);
					attrVector.set(attrNameIndex, (double) speedBumpNb
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName.equals(ScoringParameters.BETA_LEFT_TURN_NB)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.BETA_LEFT_TURN_NB);
					attrVector.set(attrNameIndex, leftTurnNb
							/ paramScaleFactorList.get(attrNameIndex));
				} else if (attrName
						.equals(ScoringParameters.BETA_INTERSECTION_NB)) {
					attrNameIndex = attrNameList
							.indexOf(ScoringParameters.BETA_INTERSECTION_NB);
					attrVector.set(attrNameIndex, intersectionNb
							/ paramScaleFactorList.get(attrNameIndex));
				}
			}

			double util = mnl
					.getCoeff()
					/* s. the attributes order in Events2Score4PC2.attrNameList */.innerProd(
							attrVector);

			plan.setScore(util);
		}
	}

	private MultinomialLogit createMultinomialLogit(Config config) {
		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), // =4
		attributeCount = Integer.parseInt(config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME, "attributeCount"));

		PlanCalcScoreConfigGroup scoring = config.planCalcScore();// TODO {@code
		// ScoringParameters.java}

		// initialize MultinomialLogit
		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize,
				attributeCount);
		mnl.setUtilityScale(scoring.getBrainExpBeta());

		for (int i = 0; i < choiceSetSize; i++) {
			mnl.setASC(i, 0);
		}

		int attrNameIndex = attrNameList.indexOf(ScoringParameters.TRAVELING);
		mnl.setCoefficient(attrNameIndex, scoring.getTraveling_utils_hr()
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList.indexOf(ScoringParameters.PERFORMING);
		mnl.setCoefficient(attrNameIndex, scoring.getPerforming_utils_hr()
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList
				.indexOf(ScoringParameters.BETA_LN_PATH_SIZE);
		String betaLnPathSizeStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				ScoringParameters.BETA_LN_PATH_SIZE);
		double betaLnPathSize = betaLnPathSizeStr != null ? Double
				.parseDouble(betaLnPathSizeStr) : 0d;
		mnl.setCoefficient(attrNameIndex, betaLnPathSize
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList
				.indexOf(ScoringParameters.BETA_SPEED_BUMP_NB);
		String betaSpeedBumpNbStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				ScoringParameters.BETA_SPEED_BUMP_NB);
		double betaSpeedBumpNb = betaSpeedBumpNbStr != null ? Double
				.parseDouble(betaSpeedBumpNbStr) : 0d;
		mnl.setCoefficient(attrNameIndex, betaSpeedBumpNb
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList
				.indexOf(ScoringParameters.BETA_LEFT_TURN_NB);
		String betaLeftTurnNbStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				ScoringParameters.BETA_LEFT_TURN_NB);
		double betaLeftTurnNb = betaLeftTurnNbStr != null ? Double
				.parseDouble(betaLeftTurnNbStr) : 0d;
		mnl.setCoefficient(attrNameIndex, betaLeftTurnNb
				* paramScaleFactorList.get(attrNameIndex));

		attrNameIndex = attrNameList
				.indexOf(ScoringParameters.BETA_INTERSECTION_NB);
		String betaIntersectionNbStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				ScoringParameters.BETA_INTERSECTION_NB);
		double betaIntersectionNb = betaIntersectionNbStr != null ? Double
				.parseDouble(betaIntersectionNbStr) : 0d;
		mnl.setCoefficient(attrNameIndex, betaIntersectionNb
				* paramScaleFactorList.get(attrNameIndex));

		return mnl;
	}
}
