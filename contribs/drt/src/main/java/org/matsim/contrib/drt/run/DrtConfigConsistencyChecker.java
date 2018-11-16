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
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParamsConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.HasMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.utils.misc.Time;

public class DrtConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Logger log = Logger.getLogger(DrtConfigConsistencyChecker.class);

	@Override
	public void checkConsistency(Config config) {
		new DvrpConfigConsistencyChecker().checkConsistency(config);

		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);
		if (drtCfg != null) {
			if (multiModeDrtCfg != null) {
				throw new RuntimeException("Either DrtConfigGroup or MultiModeDrtConfigGroup must be defined");
			}
			checkDrtConfigConsistency(drtCfg, config.global());
		} else {
			if (multiModeDrtCfg == null) {
				throw new RuntimeException("Either DrtConfigGroup or MultiModeDrtConfigGroup must be defined");
			}
			multiModeDrtCfg.getDrtConfigGroups().stream().forEach(c -> checkDrtConfigConsistency(c, config.global()));
			if (!HasMode.areModesUnique(multiModeDrtCfg.getDrtConfigGroups().stream())) {
				throw new RuntimeException("Drt modes are not unique");
			}
		}
		if (Time.isUndefinedTime(config.qsim().getEndTime())
				&& config.qsim().getSimEndtimeInterpretation() != EndtimeInterpretation.onlyUseEndtime) {
			// Not an issue if all request rejections are immediate (i.e. happen during request submission)
			log.warn("qsim.endTime should be specified and qsim.simEndtimeInterpretation should be 'onlyUseEndtime'"
					+ " if postponed request rejection is allowed. Otherwise, rejected passengers"
					+ " (who are stuck endlessly waiting for a DRT vehicle) will prevent QSim from stopping");
		}
		if (config.qsim().getNumberOfThreads() != 1) {
			throw new RuntimeException("Only a single-threaded QSim allowed");
		}
	}

	private void checkDrtConfigConsistency(DrtConfigGroup drtCfg, GlobalConfigGroup global) {
		if (drtCfg.getMaxWaitTime() < drtCfg.getStopDuration()) {
			throw new RuntimeException(
					DrtConfigGroup.MAX_WAIT_TIME + " must not be smaller than " + DrtConfigGroup.STOP_DURATION);
		}
		if (drtCfg.getOperationalScheme() == OperationalScheme.stopbased && drtCfg.getTransitStopFile() == null) {
			throw new RuntimeException(DrtConfigGroup.TRANSIT_STOP_FILE
					+ " must not be null when "
					+ DrtConfigGroup.OPERATIONAL_SCHEME
					+ " is "
					+ DrtConfigGroup.OperationalScheme.stopbased);
		}
		if (drtCfg.getNumberOfThreads() > Runtime.getRuntime().availableProcessors()) {
			throw new RuntimeException(
					DrtConfigGroup.NUMBER_OF_THREADS + " is higher than the number of logical cores available to JVM");
		}
		if (global.getNumberOfThreads() < drtCfg.getNumberOfThreads()) {
			log.warn("Consider increasing global.numberOfThreads to at least the value of drt.numberOfThreads"
					+ " in order to speed up the DRT route update during the replanning phase.");
		}
		if (drtCfg.getParameterSets(MinCostFlowRebalancingParams.SET_NAME).size() > 1) {
			throw new RuntimeException("More then one rebalancing parameter sets is specified");
		}
		new MinCostFlowRebalancingParamsConsistencyChecker().checkConsistency(drtCfg);
	}
}
