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

package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

public class PopulationUtils {
	private final static Logger LOG = Logger.getLogger(PopulationUtils.class);
	
	/**
	 * Reads in a {@link Households} file, and prints the number of 
	 * {@link Household}s, and the total number of members indicated for the 
	 * households. 
	 * @param householdsFilename
	 */
	public static void printHouseholdStatistics(String householdsFilename){
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 hr = new HouseholdsReaderV10(households);
		hr.parse(householdsFilename);
		
		/* Get the number of members. */
		int members = 0;
		for(Household h : households.getHouseholds().values()){
			members += h.getMemberIds().size();
		}
		
		/* Print the statistics to the console. */
		LOG.info("--------------------------------------------");
		LOG.info("Household statistics:");
		LOG.info("--------------------------------------------");
		LOG.info("  # households: " + households.getHouseholds().size());
		LOG.info("  # household members: " + members);
		LOG.info("--------------------------------------------");
	}

	
	/**
	 * Reads in a {@link Population} file, and prints the total number of
	 * {@link Person}s in the population.
	 * @param populationFilename
	 */
	public static void printPopulationStatistics(String populationFilename){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader pr = new MatsimPopulationReader(sc);
		pr.readFile(populationFilename);
		
		/* Print the statistics to the console. */
		LOG.info("--------------------------------------------");
		LOG.info("Population statistics:");
		LOG.info("--------------------------------------------");
		LOG.info("  # persons: " + sc.getPopulation().getPersons().size());
		LOG.info("--------------------------------------------");
	}
	
	
	/**
	 * An implementation to quickly use to print statistics.
	 */
	public static void main(String[] args){
		int option = Integer.parseInt(args[0]);
		switch (option) {
		case 1:
			printHouseholdStatistics(args[1]);
			break;
		case 2:
			printPopulationStatistics(args[1]);
		default:
			LOG.warn("Cannot print any statistics for option `" + option + "'");
			break;
		}
	}
	
	
	
	
}
