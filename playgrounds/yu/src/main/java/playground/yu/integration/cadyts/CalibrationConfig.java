/* *********************************************************************** *
 * project: org.matsim.*
 * CalibrationConfig.java
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
package playground.yu.integration.cadyts;

/**
 * stores some default value of config parameters for Calibration
 *
 * @author yu
 *
 */
public interface CalibrationConfig {
	final static int DEFAULT_CALIBRATION_START_TIME = 1,
			DEFAULT_CALIBRATION_END_TIME = 24;
	final static String BSE_CONFIG_MODULE_NAME = "bse",
			CONSTANT_LEFT_TURN = "constantLeftTurn";
	final static String PARAM_NAME_INDEX = "parameterName_",
			PARAM_STDDEV_INDEX = "paramStddev_";
}
