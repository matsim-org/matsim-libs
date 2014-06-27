/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.gautengPopulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.freight.InAreaPlanKeeper;
import playground.southafrica.utilities.Header;

/**
 * Implementing the intra-area class specifically for Gauteng.
 */
public class RemoveNonGautengCommercial {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RemoveNonGautengCommercial.class.toString(), args);
		
		String inputPlansFile = args[0];
		String inputAttributesFile = args[1];
		String shapefile = args[2];
		String outputPlansFile = args[3];
		String outputAttributesFile = args[4];
		
		run(inputPlansFile, inputAttributesFile, shapefile, outputPlansFile, outputAttributesFile);

		Header.printFooter();
	}
	
	/**
	 * Read in a population of persons and their attributes, clean the scenario
	 * using {@link InAreaPlanKeeper}, and write the resulting population of
	 * persons and their attributes to file.
	 * 
	 * @param inputPlansFile
	 * @param inputAttributesFile
	 * @param shapefile
	 * @param outputPlansFile
	 * @param outputAttributesFile
	 */
	public static void run(String inputPlansFile, String inputAttributesFile,
			String shapefile, String outputPlansFile, String outputAttributesFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(inputPlansFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputAttributesFile);
		
		/* Checking inside envelope is good enough, setting 'strictlyInside to false */
		Scenario cleanScenario = InAreaPlanKeeper.run(sc, shapefile, false);
		
		/* Write the output to files. */
		new PopulationWriter(cleanScenario.getPopulation()).write(outputPlansFile);
		new ObjectAttributesXmlWriter(cleanScenario.getPopulation().getPersonAttributes()).writeFile(outputAttributesFile);
	}
	
}
