/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerConfigLoader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import java.util.Map;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public final class StrategyManagerConfigLoader {
	private StrategyManagerConfigLoader(){} // container for static methods only, do not instantiate

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);


	@Deprecated // try to use "load" with inject syntax, see below.  kai, aug'15
	public static void load(final Controler controler, final StrategyManager manager) {
		throw new RuntimeException("Not supported.");
	}

}
