/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.utils.prepare;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class PopulationWorkOtherGeneratorMain {

	public static void main(String[] args) {
		
		String networkFile = "/Users/Ihab/Desktop/internalization_input/network_withStorageConstraint.xml";
		String dir = "/Users/Ihab/Desktop/";
		int totalDemand = 1000;
		
		File directory = new File(dir);
		directory.mkdirs();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		
		String populationFile = dir + "population" + totalDemand + ".xml";
		PopulationWorkOtherGenerator pG = new PopulationWorkOtherGenerator(scenario);
		pG.writePopulation(totalDemand, populationFile);
	}

}
