/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.vsp.andreas.utils.ana;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitActsRemover;


public class RemoveTransitActsFromPop{

	private final static Logger log = Logger.getLogger(RemoveTransitActsFromPop.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = args[0];
		String popFile = args[1];

		RemoveTransitActsFromPop.removeActs(networkFile, popFile);
	}

	private static void removeActs(String networkFile, String popFile) {
		String popOutFile = popFile + "_removedTransitActs.xml.gz";
		TransitActsRemover remover = new TransitActsRemover();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		new PopulationReader(sc).readFile(popFile);
		
		for (Person person : sc.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				remover.run(plan);
			}
		}

		PopulationWriter popWriter = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		popWriter.write(popOutFile);
	}
}
