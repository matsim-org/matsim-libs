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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general2;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.paramCorrection.BseParamCalibrationControlerListener;

/**
 * imitates and expands {@code CharyparNagelScoringParameters}
 * 
 * @author yu
 * 
 */
public class ScoringParameters implements MatsimParameters {
	private final CharyparNagelScoringParameters params;
	public final double marginalUtilityOfTraveling_s, betaNbSpeedBumps,
			betaNbLeftTurns, betaNbIntersections, betaLnPathSize;

	public ScoringParameters(Config config) {
		super();
		params = new CharyparNagelScoringParameters(config.planCalcScore());

		marginalUtilityOfTraveling_s = params.marginalUtilityOfTraveling_s;

		String betaNbSpeedBumpsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"betaNbSpeedBumps");
		betaNbSpeedBumps = betaNbSpeedBumpsStr != null ? Double
				.parseDouble(betaNbSpeedBumpsStr) : 0d;

		String betaNbLeftTurnsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"betaNbLeftTurns");
		betaNbLeftTurns = betaNbLeftTurnsStr != null ? Double
				.parseDouble(betaNbLeftTurnsStr) : 0d;

		String betaNbIntersectionsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"betaNbIntersections");
		betaNbIntersections = betaNbIntersectionsStr != null ? Double
				.parseDouble(betaNbIntersectionsStr) : 0d;

		String betaLnPathSizeStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"betaLnPathSize");
		betaLnPathSize = betaLnPathSizeStr != null ? Double
				.parseDouble(betaLnPathSizeStr) : 0d;
	}
}
