/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.assignment;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;

public class AssignmentETaxiOptimizerParams extends AssignmentTaxiOptimizerParams {
	public static final String MIN_RELATIVE_SOC = "minRelativeSoc";
	public static final String SOC_CHECK_TIME_STEP = "socCheckTimeStep";

	public final double minRelativeSoc;// XXX or absolute?
	public final int socCheckTimeStep;

	public AssignmentETaxiOptimizerParams(Configuration optimizerConfig) {
		super(optimizerConfig);

		// 30% SOC (=6 kWh) is enough to travel 40 km (all AUX off);
		// alternatively, in cold winter, it is enough to travel for 1 hour
		// (for approx. 20 km => 3kWh) with 3 kW-heating on
		minRelativeSoc = optimizerConfig.getDouble(MIN_RELATIVE_SOC, 0.3);

		// in cold winter, 3kW heating consumes 1.25% SOC every 5 min
		socCheckTimeStep = optimizerConfig.getInt(SOC_CHECK_TIME_STEP, 300);
	}
}
