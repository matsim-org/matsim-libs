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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.freight.IntraAreaIdentifier;
import playground.southafrica.utilities.Header;

/**
 * Class to check if a commercial vehicle is an 'intra-provincial' vehicle in
 * the Gauteng province.
 * 
 * @author jwjoubert
 */
public class AddGautengIntraAttribute {

	/**
	 * Implement the intra-area checker specifically for Gauteng.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AddGautengIntraAttribute.class.toString(), args);
		
		String populationFile = args[0];
		String attributesFile = args[1];
		String shapefile = args[2];
		String newAttributesFile = args[3];
		
		run(populationFile, attributesFile, shapefile, newAttributesFile);

		Header.printFooter();
	}
	
	public static void run(String populationFile, String inputPopulationAtrributeFile,
			String shapefile, String outputPopulationAttributeFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputPopulationAtrributeFile);
		
		Scenario adaptedScenario = IntraAreaIdentifier.run(sc, shapefile, false, "intraGauteng");
		
		/* Write the adapted attributes to file. */
		new ObjectAttributesXmlWriter(adaptedScenario.getPopulation().getPersonAttributes()).writeFile(outputPopulationAttributeFile);
	}
}
