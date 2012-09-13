///* *********************************************************************** *
// * project: org.matsim.*
// * MultinomialLogitCreator.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.scoring;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.paramCorrection.PCCtlListener;
//import utilities.math.MultinomialLogit;
//
//public class MultinomialLogitCreator {
//	public MultinomialLogit createSingle(Config config) {
//		return create(config, 1);
//	}
//
//	public MultinomialLogit create(Config config) {
//		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize();
//		return this.create(config, choiceSetSize);
//	}
//
//	private MultinomialLogit create(Config config, int choiceSetSize) {
//		int attributeCount = Integer.parseInt(config.findParam(
//				PCCtlListener.BSE_CONFIG_MODULE_NAME, "attributeCount"));
//
//		PlanCalcScoreConfigGroup scoring = config.planCalcScore();
//		double traveling = scoring.getTraveling_utils_hr();
//		double betaStuck = Math.min(
//				Math.min(scoring.getLateArrival_utils_hr(),
//						scoring.getEarlyDeparture_utils_hr()),
//				Math.min(traveling, scoring.getWaiting_utils_hr()));
//
//		// initialize MultinomialLogit
//		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize,// =4
//				attributeCount);// =5 [travCar,travPt,travWalk,Perf,Stuck]
//
//		mnl.setUtilityScale(scoring.getBrainExpBeta());
//
//		for (int i = 0; i < choiceSetSize; i++) {
//			mnl.setASC(i, 0);
//		}
//		// travelTime
//		int attrNameIndex = Events2Score4PC.attrNameList.indexOf("traveling");
//		mnl.setCoefficient(attrNameIndex, traveling);
//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("travelingPt");
//		mnl.setCoefficient(attrNameIndex, scoring.getTravelingPt_utils_hr());
//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("travelingWalk");
//		mnl.setCoefficient(attrNameIndex, scoring.getTravelingWalk_utils_hr());
//
//		//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("performing");
//		mnl.setCoefficient(attrNameIndex, scoring.getPerforming_utils_hr());
//		//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("stuck");
//		mnl.setCoefficient(attrNameIndex, betaStuck);
//
//		// distances
//		attrNameIndex = Events2Score4PC.attrNameList
//				.indexOf("monetaryDistanceCostRateCar");
//		mnl.setCoefficient(
//				attrNameIndex,
//				scoring.getMonetaryDistanceCostRateCar()
//						* scoring.getMarginalUtilityOfMoney());
//
//		attrNameIndex = Events2Score4PC.attrNameList
//				.indexOf("monetaryDistanceCostRatePt");
//		mnl.setCoefficient(
//				attrNameIndex,
//				scoring.getMonetaryDistanceCostRatePt()
//						* scoring.getMarginalUtilityOfMoney());
//
//		attrNameIndex = Events2Score4PC.attrNameList
//				.indexOf("marginalUtlOfDistanceWalk");
//		mnl.setCoefficient(attrNameIndex,
//				scoring.getMarginalUtlOfDistanceWalk());
//
//		// constants
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("constantCar");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantCar());
//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("constantPt");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantPt());
//
//		attrNameIndex = Events2Score4PC.attrNameList.indexOf("constantWalk");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantWalk());
//
//		return mnl;
//	}
//}
