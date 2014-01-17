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

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

/**
 * Class to join two populations, each containing optional person attributes.
 * 
 * @author jwjoubert
 */
public class JoinSubpopulations {

	/**
	 * Execute a local instance of joining two subpopulations, along with their
	 * (optional) attributes.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(JoinSubpopulations.class.toString(), args);
		
		String first = args[0];
		String firstAttribute = args[1];
		String second = args[2];
		String secondAttribute = args[3];
		String output = args[4];
		String outputAttribute = args[5];
		
		JoinSubpopulations.Run(first, firstAttribute, second, secondAttribute, output, outputAttribute);
		
		Header.printFooter();
	}
	
	
	/**
	 * Joining two subpopulations. If any of the second population's
	 * {@link Person}s has an {@link Id} that corresponds with a {@link Person} 
	 * in the first subpopulation, an exception will be thrown. The only 
	 * attributes read will be:
	 * <ul>
	 * 		<li>subpopulation;
	 * 		<li>intraGauteng.
	 * </ul>
	 *  
	 * @param first the absolute path of the first input population file; 
	 * @param firstAttribute the (optional) absolute path of the first input 
	 * 		  population's attribute file;
	 * @param second the absolute path of the second input population file; 
	 * @param secondAttribute the (optional) absolute path of the first input 
	 * 		  population's attribute file;
	 * @param output the absolute path of the output population file; and
	 * @param outputAttribute the absolute path of the output population's 
	 * 		  attribute file.
	 */
	public static void Run(String first, String firstAttribute, String second, String secondAttribute, String output, String outputAttribute){
		/* Read the first subpopulation. */
		Scenario sc1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc1).parse(first);
		/* Read the (optional) attributes of the first subpopulation. */
		File f1 = new File(firstAttribute);
		if(f1.exists() && f1.canRead()){
			new ObjectAttributesXmlReader(sc1.getPopulation().getPersonAttributes()).parse(firstAttribute);
		}
		
		/* Read the second subpopulation. */
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc2).parse(second);
		/* Read the (optional) attributes of the first subpopulation. */
		File f2 = new File(secondAttribute);
		if(f2.exists() && f2.canRead()){
			new ObjectAttributesXmlReader(sc2.getPopulation().getPersonAttributes()).parse(secondAttribute);
		}
		
		/* Add all the persons from the second to the first subpopulation. */
		for(Person p : sc2.getPopulation().getPersons().values()){
			sc1.getPopulation().addPerson(p);
			
			/* Add subpopulation attribute. */
			String subpopAttribute = sc1.getConfig().plans().getSubpopulationAttributeName();
			Object subpopulation = sc2.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), subpopAttribute);
			if(subpopulation != null && subpopulation instanceof String){
				sc1.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), subpopAttribute, (String)subpopulation);
			}
			
			/* Add inter-Gauteng attribute (may not exist). */
			Object intraGauteng = sc2.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), "intraGauteng");
			if(intraGauteng != null && intraGauteng instanceof Boolean){
				sc1.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "intraGauteng", intraGauteng);
			}
		}
		
		/* Write the resulting population to file. */
		new PopulationWriter(sc1.getPopulation(), sc1.getNetwork()).write(output);
		new ObjectAttributesXmlWriter(sc1.getPopulation().getPersonAttributes()).writeFile(outputAttribute);
	}
	

}
