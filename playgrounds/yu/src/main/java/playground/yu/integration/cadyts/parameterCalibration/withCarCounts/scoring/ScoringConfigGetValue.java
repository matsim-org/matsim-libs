/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringConfigGetValue.java
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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * @author yu
 *
 */
public class ScoringConfigGetValue {
	private final static Logger log = Logger.getLogger(ScoringConfigGetValue.class);
	private static PlanCalcScoreConfigGroup scoringCfgGroup;
	private static Config config;

	private static final String PERFORMING = "performing";
	private static final String TRAVELING = "traveling";
	private static final String TRAVELING_PT = "travelingPt";
	private static final String TRAVELING_WALK = "travelingWalk";

	private static final String MARGINAL_UTL_OF_DISTANCE_WALK = "marginalUtlOfDistanceWalk";

	private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney";
	private static final String MONETARY_DISTANCE_COST_RATE_CAR = "monetaryDistanceCostRateCar";
	private static final String MONETARY_DISTANCE_COST_RATE_PT = "monetaryDistanceCostRatePt";

	private static final String OFFSET_CAR = "offsetCar";
	private static final String OFFSET_PT = "offsetPt";
	private static final String OFFSET_WALK = "offsetWalk";

	public static void setConfig(Config config) {
		ScoringConfigGetValue.config = config;
		ScoringConfigGetValue.scoringCfgGroup = config.charyparNagelScoring();
	}

	/*
	 * <param name="parameterName_0" value="traveling" /> <param
	 * name="parameterName_1" value="travelingPt" /> <param
	 * name="parameterName_2" value="travelingWalk" />
	 *
	 * <param name="parameterName_3" value="performing" />
	 *
	 * <param name="parameterName_4" value="offsetCar" /> <param
	 * name="parameterName_5" value="offsetPt" /> <param name="parameterName_6"
	 * value="offsetWalk" />
	 *
	 * <param name="parameterName_0" value="monetaryDistanceCostRateCar" />
	 * <param name="parameterName_8" value="monetaryDistanceCostRatePt" />
	 *
	 * <param name="parameterName_9" value="marginalUtlOfDistanceWalk" />
	 */
	public static String getValue(String key) {
		if (PERFORMING.equals(key)) {
			return Double.toString(scoringCfgGroup.getPerforming_utils_hr());
		} else if (TRAVELING.equals(key)) {
			return Double.toString(scoringCfgGroup.getTraveling_utils_hr());
		} else if (TRAVELING_PT.equals(key)) {
			return Double.toString(scoringCfgGroup.getTravelingPt_utils_hr());
		} else if (TRAVELING_WALK.equals(key)) {
			return Double.toString(scoringCfgGroup.getTravelingWalk_utils_hr());
		} else if (MARGINAL_UTL_OF_DISTANCE_WALK.equals(key)) {
			return Double.toString(scoringCfgGroup
					.getMarginalUtlOfDistanceWalk());
		} else if (MONETARY_DISTANCE_COST_RATE_CAR.equals(key)) {
			return Double.toString(scoringCfgGroup
					.getMonetaryDistanceCostRateCar());
		} else if (MONETARY_DISTANCE_COST_RATE_PT.equals(key)) {
			return Double.toString(scoringCfgGroup
					.getMonetaryDistanceCostRatePt());
		} else if (OFFSET_CAR.equals(key)) {
			return config.findParam("bse", key);
		} else if (OFFSET_PT.equals(key)) {
			return config.findParam("bse", key);
		} else if (OFFSET_WALK.equals(key)) {
			return config.findParam("bse", key);
		} else {
			log.info(key + "\tcan not be calibrated by the current code.");
			throw new RuntimeException();
		}
	}
}
