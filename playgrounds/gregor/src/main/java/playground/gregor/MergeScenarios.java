/* *********************************************************************** *
 * project: org.matsim.*
 * MergeScenarios.java
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

package playground.gregor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class MergeScenarios {
	
	public static void main(String [] args) {
		String conf = "/Users/laemmel/devel/hhw_hybrid/input/config.xml";
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		Config c2 = ConfigUtils.createConfig();
		c2.network().setInputFile("/Users/laemmel/devel/hhw_hybrid/input/car_network.xml.gz");
		c2.plans().setInputFile("/Users/laemmel/devel/hhw_hybrid/input/car_plans.xml.gz");
		Scenario sc2 = ScenarioUtils.loadScenario(c2);
		
		for (Node n : sc2.getNetwork().getNodes().values()) {
			n.getOutLinks().clear();
			n.getInLinks().clear();
			sc.getNetwork().addNode(n);
		}
		for (Link l : sc2.getNetwork().getLinks().values()) {
			sc.getNetwork().addLink(l);
		};
		for (Person p : sc2.getPopulation().getPersons().values()) {
			sc.getPopulation().addPerson(p);
		}
		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(sc.getPopulation()).write(c.plans().getInputFile());
	}

}
