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

package org.matsim.contrib.dvrp.run;

import org.apache.log4j.Logger;
import org.matsim.contrib.dynagent.run.DynQSimConfigConsistencyChecker;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class DvrpConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Logger log = Logger.getLogger(DvrpConfigConsistencyChecker.class);

	@Override
	public void checkConsistency(Config config) {
		new DynQSimConfigConsistencyChecker().checkConsistency(config);

		if (!config.qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles()) {
			log.warn("Typically, vrp paths are calculated from startLink to endLink"
					+ "(not from startNode to endNode). That requires making some assumptions"
					+ "on how much time travelling on the first and last links takes. "
					+ "The current implementation assumes freeflow travelling on the last link, "
					+ "which is actually the case in QSim, and a 1-second stay on the first link. "
					+ "The latter expectation is optimistic, and to make it more likely,"
					+ "departing vehicles must be inserted befor driving ones "
					+ "(though that still does not guarantee 1-second stay");
		}

		if (config.qsim().isRemoveStuckVehicles()) {
			throw new RuntimeException("Stuck DynAgents cannot be removed from simulation");
		}

		DvrpConfigGroup dvrpCfg = DvrpConfigGroup.get(config);
		double alpha = dvrpCfg.getTravelTimeEstimationAlpha();
		if (alpha > 1 || alpha <= 0) {
			throw new RuntimeException("travelTimeEstimationAlpha must be in (0,1]");
		}

		if (dvrpCfg.getTravelTimeEstimationBeta() < 0) {
			throw new RuntimeException("travelTimeEstimationBeta must be zero or positive");
		}
	}
}
