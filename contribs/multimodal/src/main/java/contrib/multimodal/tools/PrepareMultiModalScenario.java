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

public class PrepareMultiModalScenario {

	private static final Logger log = Logger.getLogger(PrepareMultiModalScenario.class);
	
	public static void run(Scenario scenario) {
		
		Config config = scenario.getConfig();
		if (config.multiModal().isCreateMultiModalNetwork()) {
			log.info("Creating multi modal network.");
			new MultiModalNetworkCreator(config.multiModal()).run(scenario.getNetwork());
		}

		if (config.multiModal().isEnsureActivityReachability()) {
			log.info("Relocating activities that cannot be reached by the transport modes of their from- and/or to-legs...");
			new EnsureActivityReachability(scenario).run(scenario.getPopulation());
		}

		if (config.multiModal().isDropNonCarRoutes()) {
			log.info("Dropping existing routes of modes which are simulated with the multi modal mobsim.");
			new NonCarRouteDropper(config.multiModal()).run(scenario.getPopulation());
		}
	}
}
