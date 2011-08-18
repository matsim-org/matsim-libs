/* *********************************************************************** *
 * project: org.matsim.*
 * MultinomialLogitCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection.PCCtlListener;
import cadyts.utilities.math.MultinomialLogit;

/**
 * creates a MultinomialLogit of cadyts
 *
 * @author yu
 *
 */
public class MultinomialLogitCreator {

	public static MultinomialLogit createMultinomialLogit(Config config) {
		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), // =4
		attributeCount = Integer.parseInt(config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "attributeCount"));

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
		int attrNameIndex = PCEventsToScore.attrNameList.indexOf("traveling");
		mnl.setCoefficient(attrNameIndex, traveling);

		attrNameIndex = PCEventsToScore.attrNameList.indexOf("travelingPt");
		mnl.setCoefficient(attrNameIndex, scoring.getTravelingPt_utils_hr());

		attrNameIndex = PCEventsToScore.attrNameList.indexOf("travelingWalk");
		mnl.setCoefficient(attrNameIndex, scoring.getTravelingWalk_utils_hr());

		//
		attrNameIndex = PCEventsToScore.attrNameList.indexOf("performing");
		mnl.setCoefficient(attrNameIndex, scoring.getPerforming_utils_hr());
		//
		attrNameIndex = PCEventsToScore.attrNameList.indexOf("stuck");
		mnl.setCoefficient(attrNameIndex, betaStuck);

		// distances
		attrNameIndex = PCEventsToScore.attrNameList
				.indexOf("monetaryDistanceCostRateCar");
		mnl.setCoefficient(
				attrNameIndex,
				scoring.getMonetaryDistanceCostRateCar()
						* scoring.getMarginalUtilityOfMoney());

		attrNameIndex = PCEventsToScore.attrNameList
				.indexOf("monetaryDistanceCostRatePt");
		mnl.setCoefficient(
				attrNameIndex,
				scoring.getMonetaryDistanceCostRatePt()
						* scoring.getMarginalUtilityOfMoney());

		attrNameIndex = PCEventsToScore.attrNameList
				.indexOf("marginalUtlOfDistanceWalk");
		mnl.setCoefficient(attrNameIndex,
				scoring.getMarginalUtlOfDistanceWalk());

		// constants
		attrNameIndex = PCEventsToScore.attrNameList.indexOf("constantCar");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantCar());

		attrNameIndex = PCEventsToScore.attrNameList.indexOf("constantPt");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantPt());

		attrNameIndex = PCEventsToScore.attrNameList.indexOf("constantWalk");
		mnl.setCoefficient(attrNameIndex, scoring.getConstantWalk());

		return mnl;
	}

}
