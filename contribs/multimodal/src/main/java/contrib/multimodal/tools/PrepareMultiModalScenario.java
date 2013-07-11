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

package contrib.multimodal.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.utils.collections.CollectionUtils;

public class PrepareMultiModalScenario {

	private static final Logger log = Logger.getLogger(PrepareMultiModalScenario.class);
	
	public static void run(Scenario scenario) {
		Config config = scenario.getConfig();
		log.info("setting up multi modal simulation");
		
		// set Route Factories
		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
        MultiModalConfigGroup multiModalConfigGroup4 = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        for (String mode : CollectionUtils.stringToArray(multiModalConfigGroup4.getSimulatedModes())) {
			((PopulationFactoryImpl) scenario.getPopulation().getFactory()).setRouteFactory(mode, factory);
		}
        MultiModalConfigGroup multiModalConfigGroup3 = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup3.isCreateMultiModalNetwork()) {
			log.info("Creating multi modal network.");
            MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
            new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
		}

        MultiModalConfigGroup multiModalConfigGroup2 = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup2.isEnsureActivityReachability()) {
			log.info("Relocating activities that cannot be reached by the transport modes of their from- and/or to-legs...");
			new EnsureActivityReachability(scenario).run(scenario.getPopulation());
		}

        MultiModalConfigGroup multiModalConfigGroup1 = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup1.isDropNonCarRoutes()) {
			log.info("Dropping existing routes of modes which are simulated with the multi modal mobsim.");
            MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
            new NonCarRouteDropper(multiModalConfigGroup).run(scenario.getPopulation());
		}
	}
}
