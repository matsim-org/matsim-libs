/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.benchmark;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import com.google.common.collect.ImmutableSet;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpBenchmarks {
	public static void adjustConfig(Config config) {
		DvrpConfigGroup.get(config).setNetworkModes(ImmutableSet.of());// to switch off network filtering
		config.addConfigConsistencyChecker(new DvrpBenchmarkConfigConsistencyChecker());
	}

	public static void initController(Controler controler) {
		controler.setModules(new DvrpBenchmarkControlerModule());
		controler.addOverridingModule(new DvrpModule(new DvrpBenchmarkTravelTimeModule()));
		controler.addOverridingQSimModule(new DvrpBenchmarkQSimModule());
	}
}
