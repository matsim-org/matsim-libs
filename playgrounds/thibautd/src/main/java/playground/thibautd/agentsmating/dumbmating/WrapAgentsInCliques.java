/* *********************************************************************** *
 * project: org.matsim.*
 * WrapAgentsInCliques.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.dumbmating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.householdsfromcensus.CliquesWriter;

/**
 * Creates one clique per agent (to use the joint replanning algo on
 * individual plans).
 *
 * @author thibautd
 */
public class WrapAgentsInCliques {
	private static final Logger log =
		Logger.getLogger(WrapAgentsInCliques.class);

	public static void main(String[] args) {
		String configFile = args[0];
		String outputFile = args[1];

		log.debug("loading config");
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Set<Id> personIds = scenario.getPopulation().getPersons().keySet();

		log.info("constructing cliques");
		Map<Id, List<Id>> cliques = new HashMap<Id, List<Id>>(personIds.size());
		List<Id> cliqueIds;

		for (Id id : personIds) {
			cliqueIds = new ArrayList<Id>(1);
			cliqueIds.add(id);
			cliques.put(id, cliqueIds);
		}

		try {
			(new CliquesWriter(cliques)).writeFile(outputFile);
		} catch (Exception e) {}

		log.info("cliques written to "+outputFile);
	}
}

