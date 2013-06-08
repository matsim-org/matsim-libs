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

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.southafrica.utilities.Header;

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
	 * Check the population for unique activity types, and number of occurrences
	 * for each type.
	 * @param populationFilename
	 */
	public static void printActivityStatistics(String populationFilename){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader pr = new MatsimPopulationReader(sc);
		pr.readFile(populationFilename);
		
		LOG.info("Population parsed. Analysing activity types...");
		Counter counter = new Counter(" person # ");
		Map<String, Integer> map = new TreeMap<String, Integer>();
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(id).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(!map.containsKey(act.getType())){
						map.put(act.getType(), new Integer(1));
					} else{
						map.put(act.getType(), map.get(act.getType()) + 1 );
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Print the statistics to the console. */
		LOG.info("--------------------------------------------");
		LOG.info("Activity statistics:");
		LOG.info("--------------------------------------------");
		for(String s : map.keySet()){
			String gap = "";
			for(int i = 0; i < 6-s.length(); i++){
				gap += " ";
			}
			LOG.info(String.format("%s%s: %d", gap, s, map.get(s)));			
		}
		LOG.info("--------------------------------------------");
	}
	
	
	/**
	 * An implementation to quickly use to print statistics.
	 */
	public static void main(String[] args){
		Header.printHeader(PopulationUtils.class.toString(), args);
		
		int option = Integer.parseInt(args[0]);
		switch (option) {
		case 1:
			printHouseholdStatistics(args[1]);
			break;
		case 2:
			printPopulationStatistics(args[1]);
			break;
		case 3:
			printActivityStatistics(args[1]);
			break;
		default:
			LOG.warn("Cannot print any statistics for option `" + option + "'");
			break;
		}
		
		Header.printFooter();
	}
	
	
	
	
}
