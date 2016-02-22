/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class FilterWobBsPaxOnly {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimPopulationReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/scenario/input/vw073.120.plans.xml.gz");
		
		for (Person p : scenario.getPopulation().getPersons().values()){
			if (p.getId().toString().startsWith("BS_WB")) scenario2.getPopulation().addPerson(p);
			else if (p.getId().toString().startsWith("WB_BS")) scenario2.getPopulation().addPerson(p);
		}
		new PopulationWriter(scenario2.getPopulation()).write("../../../shared-svn/projects/vw_rufbus/scenario/input/vw073.120.bswbplans.xml.gz");
	}

}
