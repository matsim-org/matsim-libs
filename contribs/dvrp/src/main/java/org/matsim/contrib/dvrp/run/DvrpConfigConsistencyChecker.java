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
			// Typically, vrp paths are calculated from startLink to endLink
			// (not from startNode to endNode). That requires making some assumptions
			// on how much time travelling on the first and last links takes.
			// The current implementation assumes:
			// (a) free-flow travelling on the last link, which is actually the case in QSim, and
			// (b) a 1-second stay on the first link (spent on moving over the first node).
			// The latter expectation is assumes that departing vehicles must be inserted before driving ones
			// (though that still does not guarantee 1-second stay since the vehicle may need to wait if the next
			// link is fully congested)
			log.warn(" 'QSim.insertingWaitingVehiclesBeforeDrivingVehicles' should be true in order to get"
					+ " more precise travel time estimates. See comments in DvrpConfigConsistencyChecker");
		}
		if (config.qsim().isRemoveStuckVehicles()) {
			throw new RuntimeException("Stuck DynAgents cannot be removed from simulation");
		}
	}
}
