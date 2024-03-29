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

package org.matsim.contrib.dynagent.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;

public class DynQSimConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Logger log = LogManager.getLogger(DynQSimConfigConsistencyChecker.class);

	@Override
	public void checkConsistency(Config config) {
		if (config.qsim().getStartTime().orElse(0) != 0) {//undefined means 0
			// If properly handled this should not be a concern
			log.warn("QSim.startTime should be 0:00:00 (or 0). This is what typically DynAgents assume");
		}
		if (config.qsim().getTimeStepSize() != 1) {
			// If properly handled this should not be a concern
			log.warn("QSim.timeStepSize should be 1. This is what typically DynAgents assume");
		}
		if (config.qsim().getSimStarttimeInterpretation() != StarttimeInterpretation.onlyUseStarttime) {
			throw new RuntimeException("DynAgents require simulation to start from the very beginning."
					+ " Set 'QSim.simStarttimeInterpretation' to "
					+ StarttimeInterpretation.onlyUseStarttime);
		}
	}
}
