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

package playground.michalm.audiAV;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.random.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class AudiAVSmallPlansCreator {
	public static void main(String[] args) {
		String dir = "../../../shared-svn/projects/audi_av/scenario/";
		String planFile = dir + "plans.xml.gz";
		String fractPlanFile = dir + "plans_small.xml.gz";

		final double fraction = 0.001;// about 2.5k+ trips

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(planFile);
		Population population = scenario.getPopulation();

		Population fractPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		UniformRandom uniform = RandomUtils.getGlobalUniform();

		for (Person p : population.getPersons().values()) {
			if (uniform.trueOrFalse(fraction)) {
				fractPop.addPerson(p);
			}
		}

		new PopulationWriter(fractPop).write(fractPlanFile);
	}
}
