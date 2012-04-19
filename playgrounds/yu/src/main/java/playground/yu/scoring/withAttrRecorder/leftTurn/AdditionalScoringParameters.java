/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;

import playground.yu.integration.cadyts.CalibrationConfig;

public class AdditionalScoringParameters {
	public final double constantLeftTurn;

	public AdditionalScoringParameters(final Config config) {
		String constLeftTurnStr = config.findParam(
				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
				CalibrationConfig.CONSTANT_LEFT_TURN);
		if (constLeftTurnStr != null) {
			constantLeftTurn = Double.parseDouble(constLeftTurnStr);
		} else {
			constantLeftTurn = 0d;
		}
	}
}