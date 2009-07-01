/* *********************************************************************** *
 * project: org.matsim.*
 * ShrinkPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.ivtsurveys;

import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;

/**
 * @author illenberger
 *
 */
public class ShrinkPopulation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		loader.loadPopulation();
		Population population = data.getPopulation();
		double sample = Double.parseDouble(args[2]);
		PopulationWriter writer = new PopulationWriter(population, args[1], "v4", sample);
		writer.write();

	}

}
