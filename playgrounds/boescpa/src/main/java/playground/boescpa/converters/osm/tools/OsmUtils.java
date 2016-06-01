/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.osm.tools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Provides different useful functions for the OSM-converter-framework.
 *
 * @author boescpa
 */
public class OsmUtils {

	public static Scenario getEmptyPTScenario() {
		final Scenario scenario = getEmptyScenario();
		scenario.getConfig().transit().setUseTransit(true);
		return scenario;
	}

	public static Scenario getEmptyScenario() {
		return ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
}
