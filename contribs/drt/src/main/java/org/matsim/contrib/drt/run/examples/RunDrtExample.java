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

import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.core.config.Config;

/**
 * @author jbischoff
 * An example to run the demand responsive transport contribution in MATSim. Provides three different examples.
 * All the example files are located in the resource path of the drt contrib jar. If you have trouble locating them, you may also download them directly from github at:
 * <a href="https://github.com/matsim-org/matsim/tree/master/contribs/drt/src/main/resources">https://github.com/matsim-org/matsim/tree/master/contribs/drt/src/main/resources</a>
 */
public class RunDrtExample {
	public static void run(Config config, boolean otfvis) {
		DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis).run();
	}
}
