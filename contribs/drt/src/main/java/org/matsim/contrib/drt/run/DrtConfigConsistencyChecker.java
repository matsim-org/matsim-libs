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

package org.matsim.contrib.drt.run;

import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class DrtConfigConsistencyChecker implements ConfigConsistencyChecker {

	@Override
	public void checkConsistency(Config config) {
		new DvrpConfigConsistencyChecker().checkConsistency(config);

		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		if (drtCfg.getMaxTravelTimeAlpha() < 1) {
			throw new RuntimeException(DrtConfigGroup.MAX_TRAVEL_TIME_ALPHA + " may not be less than 1.0");
		}
		if (drtCfg.getMaxTravelTimeBeta() < 0) {
			throw new RuntimeException(DrtConfigGroup.MAX_TRAVEL_TIME_BETA + " must be zero or positive");
		}
		if (drtCfg.getMaxWaitTime() < 0) {
			throw new RuntimeException(DrtConfigGroup.MAX_WAIT_TIME + " must be zero or positive");
		}
		if (drtCfg.getOperationalScheme() == OperationalScheme.stationbased && drtCfg.getMaxWalkDistance() < 0) {
			throw new RuntimeException(DrtConfigGroup.MAX_WALK_DISTANCE + " must be zero or positive"
					+ " when the operational scheme is " + DrtConfigGroup.OperationalScheme.stationbased);
		}
		if (drtCfg.getStopDuration() < 0) {
			throw new RuntimeException(DrtConfigGroup.STOP_DURATION + " must be zero or positive");
		}
		if (drtCfg.getNumberOfThreads() > Runtime.getRuntime().availableProcessors()) {
			throw new RuntimeException(
					DrtConfigGroup.NUMBER_OF_THREADS + " is higher than the number of logical cores available to JVM");
		}
		if (config.qsim().getNumberOfThreads() != 1) {
			throw new RuntimeException("Only a single-threaded QSim allowed");
		}
	}
}
