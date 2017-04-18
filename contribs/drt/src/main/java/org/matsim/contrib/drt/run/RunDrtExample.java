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

package org.matsim.contrib.drt.run;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 */
public class RunDrtExample {
	
	
	public static void run(Config config, boolean otfvis) {
		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();
		DrtControlerCreator.createControler(config, otfvis).run();;
	}

	

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("drt_example/drtconfig.xml", new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		run(config,true);

	}
}
