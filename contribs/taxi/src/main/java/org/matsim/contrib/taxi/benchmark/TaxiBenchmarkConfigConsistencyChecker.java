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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class TaxiBenchmarkConfigConsistencyChecker implements ConfigConsistencyChecker {
	@Override
	public void checkConsistency(Config config) {
		new TaxiConfigConsistencyChecker().checkConsistency(config);
		new DvrpBenchmarkConfigConsistencyChecker().checkConsistency(config);
	}
}
