/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareMultiModalScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class PrepareMultiModalScenario {

	private static final Logger log = Logger.getLogger(PrepareMultiModalScenario.class);
	
	public static void run(Scenario scenario) {
		Config config = scenario.getConfig();
		log.info("setting up multi modal simulation");
		
		MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
		
		// set Route Factories
//		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
//        for (String mode : CollectionUtils.stringToArray(multiModalConfigGroup.getSimulatedModes())) {
//			((PopulationFactoryImpl) scenario.getPopulation().getFactory()).setRouteFactory(mode, factory);
//		}
        
        if (multiModalConfigGroup.isCreateMultiModalNetwork()) {
			log.info("Creating multi modal network.");
            new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
		}

        if (multiModalConfigGroup.isEnsureActivityReachability()) {
			log.info("Relocating activities that cannot be reached by the transport modes of their from- and/or to-legs...");
			EnsureActivityReachability ensureActivityReachability = new EnsureActivityReachability(scenario);
			ensureActivityReachability.run(scenario.getPopulation());
			ensureActivityReachability.printRelocateCount();
		}

        if (multiModalConfigGroup.isDropNonCarRoutes()) {
			log.info("Dropping existing routes of modes which are simulated with the multi modal mobsim.");
            new NonCarRouteDropper(multiModalConfigGroup).run(scenario.getPopulation());
		}
	}
}
