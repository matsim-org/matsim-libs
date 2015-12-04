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

/**
 * 
 */
package playground.johannes.gsv.demand.tasks;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import playground.johannes.gsv.demand.PopulationTask;

/**
 * @author johannes
 *
 */
public class PlanNetworkConnect implements PopulationTask {

	private final Scenario scenario;
	
	public PlanNetworkConnect(String roadNetFile) {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
		reader.readFile(roadNetFile);
	}
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		XY2Links delegate = new XY2Links(scenario);

		delegate.run(pop);
	}

}
