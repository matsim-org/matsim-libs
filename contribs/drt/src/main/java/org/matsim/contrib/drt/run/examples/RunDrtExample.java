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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 * An example to run the demand responsive transport contribution in MATSim. Provides three different examples.
 * All the example files are located in the resource path of the drt contrib jar. If you have trouble locating them, you may also download them directly from github at:
 * <a href="https://github.com/matsim-org/matsim/tree/master/contribs/drt/src/main/resources">https://github.com/matsim-org/matsim/tree/master/contribs/drt/src/main/resources</a>
 */
public class RunDrtExample {
	
	private static final String COTTBUS_CONFIG_FILE_DOOR2DOOR = "drt_example/drtconfig_door2door.xml";
	@SuppressWarnings("unused")
	private static final String COTTBUS_CONFIG_FILE_STOPBASED = "drt_example/drtconfig_stopbased.xml";
	@SuppressWarnings("unused")
	private static final String MIELEC_CONFIG_FILE = "mielec_2014_02/mielec_drt_config.xml";

	public static void run(Config config, boolean otfvis) {
		DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis).run();
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(COTTBUS_CONFIG_FILE_DOOR2DOOR, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.kinematicWaves);
		run(config, false);
	}
}
