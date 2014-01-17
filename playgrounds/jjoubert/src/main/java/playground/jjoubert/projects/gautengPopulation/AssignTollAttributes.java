/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.gauteng.roadpricingscheme.TagPenetration;
import playground.southafrica.utilities.Header;

/**
 * Class to add {@link ObjectAttributes} to persons in a population. The 
 * attributes include whether vehicles have e-Tags for the Gauteng toll, and
 * the vehicle type.
 * 
 * @author jwjoubert
 */
public class AssignTollAttributes {
	private static Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AssignTollAttributes.class.toString(), args);
		String populationFile = args[0];
		String inputAttributes = args[1];
		String outputAttributes = args[2];
		
		AssignTollAttributes.Run(populationFile, inputAttributes, outputAttributes);
		
		Header.printFooter();
	}
	
	/**
	 * Adds Gauteng eToll specific attributes to a population file. Currently
	 * the following attributes are added:
	 * <ul>
	 * 		<li> whether or not a vehicle has an eTag. See {@link TagPenetration};
	 * 		<li> adding a vehicle toll class -- A2, B, or C -- depending on the 
	 * 			 subpopulation.
	 * </ul>
	 * 
	 * @param population
	 * @param inputAttributes
	 * @param outputAttributes
	 */
	public static void Run(String population, String inputAttributes, String outputAttributes){
		/* Read population and population attributes. */
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(population);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputAttributes);
		
		for(Person p : sc.getPopulation().getPersons().values()){
			addTagPenetration(p);
			addVehicleType(p);
		}
		
		/* Write the population attributes. */
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(outputAttributes);
	}

	
	private static void addVehicleType(Person p) {
		Id id = p.getId();
		int subpopulation = getSubpopulationInt(p);
		double random = MatsimRandom.getRandom().nextDouble();
		String tollClass = "Unknown";

		switch (subpopulation) {
		case 0:
			tollClass = "A2";
			break;
		case 1:
			/* We distinguish between intra- and inter-provincial traffic for
			 * Gauteng. All intra-Gauteng vehicles do/should have an attribute
			 * indicating it as such. */
			boolean intraGauteng = false;

			Object o = sc.getPopulation().getPersonAttributes().getAttribute(id.toString(), "intraGauteng");
			if(o == null){
				/* It is not a intra-Gauteng vehicle: do nothing. */
			} else{
				/* There is a intra-Gauteng indicator: parse it. */
				if(o instanceof Boolean){
					intraGauteng = (Boolean) o;
				}
			}
			
			if(intraGauteng){
				/* Assign an arbitrary split for A:B:C of 50:50:0. */ 
				if(random <= 0.5){
					tollClass = "A2";
				} else if(random <= 1.0){
					tollClass = "B";
				} else {
					tollClass = "C";
				}
			} else{
				/* Assign an arbitrary split for A:B:C of 5:45:50. */
				if(random <= 0.05){
					tollClass = "A2";
				} else if(random <= 0.50){
					tollClass = "B";
				} else {
					tollClass = "C";
				}
			}			
			break;
		case 2:
			tollClass = "B";
			break;
		case 3:
			tollClass = "A2";
			break;
		case 4:
			tollClass = "A2";
			break;
		}
		
		sc.getPopulation().getPersonAttributes().putAttribute(id.toString(), "vehicleTollClass", tollClass);
	}


	/**
	 * Adds an attribute 'eTag' with value true/false to the person.
	 * 
	 * @param p
	 */
	private static void addTagPenetration(Person p) {
		Id id = p.getId();
		int subpopulation = getSubpopulationInt(p);
		double random = MatsimRandom.getRandom().nextDouble();
		double penetration = 0.0;
		
		switch (subpopulation) {
		case 0:
			penetration = TagPenetration.CAR;
			break;
		case 1:
			penetration = TagPenetration.COMMERCIAL;
			break;
		case 2:
			penetration = TagPenetration.BUS;
			break;
		case 3:
			penetration = TagPenetration.TAXI;
			break;
		case 4:
			penetration = TagPenetration.EXTERNAL;
			break;
		}
		
		if(random <= penetration){
			sc.getPopulation().getPersonAttributes().putAttribute(id.toString(), "eTag", true);
		} else{
			sc.getPopulation().getPersonAttributes().putAttribute(id.toString(), "eTag", false);			
		}
	}

	
	/**
	 * Converts the subpopulation attribute to an integer.
	 * @param p
	 * @return
	 */
	private static int getSubpopulationInt(Person p) {
		Object o = sc.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), sc.getConfig().plans().getSubpopulationAttributeName());
		if(o == null){
			/* Default subpopulation is assumed to be private individuals 
			 * travelling by car. */
			return 0; 
		} else{
			if(o instanceof String){
				String s = (String) o;
				if(s.equalsIgnoreCase("car")){
					return 0;
				} else if(s.equalsIgnoreCase("commercial")){
					return 1;
				} else if(s.equalsIgnoreCase("bus")){
					return 2;
				} else if(s.equalsIgnoreCase("taxi")){
					return 3;
				} else if(s.equalsIgnoreCase("ext")){
					return 4;
				} else{
					throw new RuntimeException("Unknown/invalid subpopulation type `" + s + "'");
				}
			}
		}
		return 0;
	}

}
