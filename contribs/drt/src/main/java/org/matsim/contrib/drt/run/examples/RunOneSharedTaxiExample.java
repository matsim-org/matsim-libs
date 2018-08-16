/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run.examples;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
public class RunOneSharedTaxiExample {
	private static final String CONFIG_FILE = "one_shared_taxi/one_shared_taxi_config.xml";

	public static void run(boolean otfvis, int lastIteration) {
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		config.controler().setWriteEventsInterval(lastIteration);
		DrtControlerCreator.createControler(config, otfvis).run();
	}

	public static void main(String[] args) {
		run(false, 2); // switch to 'true' to turn on visualisation
	}
}
