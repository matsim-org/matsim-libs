/* *********************************************************************** *
 * project: org.matsim.*
 * ModeReporter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.population.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Simple analysis to report the number of (unique) individuals who use each of
 * the observed modes in a given population with plans. Also, the class reports
 * the number of trips using each observed mode.
 * 
 * @author jwjoubert
 */
public class ModeReporter {
	final private static Logger LOG = Logger.getLogger(ModeReporter.class);
	final private Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ModeReporter.class.toString(), args);
		
		String population = args[0];
		ModeReporter mr = new ModeReporter(population);
		mr.reportModeShare();
		
		Header.printFooter();
	}
	
	/**
	 * Instantiates the mode reporter by reading in a MATSim population, 
	 * which is assumed to have {@link Plan}s.
	 * @param populationFile
	 */
	public ModeReporter(String populationFile) {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(populationFile);
	}
	
	public void reportModeShare(){
		LOG.info("Scanning population for mode usage...");
		Map<String, Integer> mapTrips = new TreeMap<>();
		Map<String, Integer> mapPersons = new TreeMap<>();
		
		int travellers = 0;
		
		Counter counter = new Counter("  person # ");
		for(Id<Person> pid : sc.getPopulation().getPersons().keySet()){
			List<String> modesUsed = new ArrayList<String>();
			
			Plan plan = sc.getPopulation().getPersons().get(pid).getSelectedPlan();
			List<Leg> legs = PopulationUtils.getLegs(plan);
			travellers += legs.size() > 0 ? 1 : 0;
			for(Leg leg : legs){
				String mode = leg.getMode();
				
				/* Check if this person has already used this mode. */
				if(!modesUsed.contains(mode)){
					modesUsed.add(mode);
				}
				
				/* Update the trip counts. */
				if(!mapTrips.containsKey(mode)){
					mapTrips.put(mode, 1);
				} else{
					int oldValue = mapTrips.get(mode);
					mapTrips.put(mode, oldValue+1);
				}
			}

			/* Update the persons map. */
			for(String mode : modesUsed){
				if(!mapPersons.containsKey(mode)){
					mapPersons.put(mode, 1);
				} else{
					int oldValue = mapPersons.get(mode);
					mapPersons.put(mode, oldValue+1);
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Report the person statistics. */
		int populationSize = sc.getPopulation().getPersons().size();
		LOG.info("     Total population size: " + populationSize);
		LOG.info("Total number of travellers: " + travellers);
		LOG.info("Number of persons using each observed mode:");
		for(String mode: mapPersons.keySet()){
			double share = ((double)mapPersons.get(mode)) / ((double)travellers);
			LOG.info(String.format("%8s: %d (%.2f%%)", mode, mapPersons.get(mode), share*100));
		}
		
		/* Report the trip statistics. */
		int totalNumberOfTrips = 0;
		for(String mode: mapTrips.keySet()){
			totalNumberOfTrips += mapTrips.get(mode);
		}
		LOG.info("");
		LOG.info("Total number of trips: " + totalNumberOfTrips);
		LOG.info("Number of trips for each observed mode:");
		for(String mode : mapTrips.keySet()){
			double share = ((double)mapTrips.get(mode)) / ((double)totalNumberOfTrips);
			LOG.info(String.format("%12s: %d (%.2f%%)", mode, mapTrips.get(mode), share*100));
		}
	}

}
