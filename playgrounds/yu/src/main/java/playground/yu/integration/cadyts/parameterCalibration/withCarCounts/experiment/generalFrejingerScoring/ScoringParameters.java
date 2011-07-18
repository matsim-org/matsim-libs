/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringParameters.java
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;

/**
 * imitates and expands {@code CharyparNagelScoringParameters}
 *
 * @author yu
 *
 */
public class ScoringParameters implements MatsimParameters {
	private final CharyparNagelScoringParameters params;
	public static final String PERFORMING = "performing",
			TRAVELING = "traveling", BETA_SPEED_BUMP_NB = "betaSpeedBumpNb",
			BETA_LEFT_TURN_NB = "betaLeftTurnNb",
			BETA_INTERSECTION_NB = "betaIntersectionNb",
			BETA_LN_PATH_SIZE = "betaLnPathSize";

	public final double marginalUtilityOfTraveling_s, betaSpeedBumpNb,
			betaLeftTurnNb, betaIntersectionNb, betaLnPathSize;

	public ScoringParameters(Config config) {
		super();
		params = new CharyparNagelScoringParameters(config.planCalcScore());

		marginalUtilityOfTraveling_s = params.marginalUtilityOfTraveling_s;

		String betaNbSpeedBumpsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				BETA_SPEED_BUMP_NB);
		betaSpeedBumpNb = betaNbSpeedBumpsStr != null ? Double
				.parseDouble(betaNbSpeedBumpsStr) : 0d;

		String betaNbLeftTurnsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				BETA_LEFT_TURN_NB);
		betaLeftTurnNb = betaNbLeftTurnsStr != null ? Double
				.parseDouble(betaNbLeftTurnsStr) : 0d;

		String betaNbIntersectionsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				BETA_INTERSECTION_NB);
		betaIntersectionNb = betaNbIntersectionsStr != null ? Double
				.parseDouble(betaNbIntersectionsStr) : 0d;

		String betaLnPathSizeStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				BETA_LN_PATH_SIZE);
		betaLnPathSize = betaLnPathSizeStr != null ? Double
				.parseDouble(betaLnPathSizeStr) : 0d;
	}
}
