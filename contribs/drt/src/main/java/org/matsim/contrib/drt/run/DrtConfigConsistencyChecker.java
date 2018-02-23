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

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.utils.misc.Time;

public class DrtConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Logger log = Logger.getLogger(DrtConfigConsistencyChecker.class);

	@Override
	public void checkConsistency(Config config) {
		new DvrpConfigConsistencyChecker().checkConsistency(config);

		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		if (Time.isUndefinedTime(config.qsim().getEndTime())
				&& config.qsim().getSimEndtimeInterpretation() != EndtimeInterpretation.onlyUseEndtime) {
			// Not an issue if all request rejections are immediate (i.e. happen during request submission)
			log.warn("qsim.endTime should be specified and qsim.simEndtimeInterpretation should be 'onlyUseEndtime'"
					+ " if postponed request rejection is allowed. Otherwise, rejected passengers"
					+ " (who are stuck endlessly waiting for a DRT vehicle) will prevent QSim from stopping");
		}
		if (drtCfg.getOperationalScheme() == OperationalScheme.stationbased && drtCfg.getTransitStopFile() == null) {
			throw new RuntimeException(DrtConfigGroup.TRANSIT_STOP_FILE + " must not be null when "
					+ DrtConfigGroup.OPERATIONAL_SCHEME + " is " + DrtConfigGroup.OperationalScheme.stationbased);
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
