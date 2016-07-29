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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.southafrica.population.utilities.activityTypeManipulation.NmbmActivityTypeManipulator;
import playground.southafrica.utilities.Header;

public class PopulationUtils {
	private final static Logger LOG = Logger.getLogger(PopulationUtils.class);
	
	/**
	 * Method to read a population file, and extract all the activity durations
	 * before writing them to file. This is used, for example,as input into the
	 * plotSurveyActivityDurations.R script and calculate the deciles before
	 * manipulating the activity types using {@link NmbmActivityTypeManipulator}.
	 *   
	 * @param populationFile
	 * @param outputFile
	 */
	public static void extractActivityDurations(String populationFile, String outputFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(populationFile);
		
		/*TODO Remove after debugging. */
		double maxS = Double.NEGATIVE_INFINITY;
		
		List<Tuple<String, Double>> durations = new ArrayList<Tuple<String,Double>>();
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			
			/* Ignore plans that have a s ingle home-based activity. */
			if(selectedPlan.getPlanElements().size() == 1){
				/* Ignore it, the person stays home. */
				/* TODO Toe determine the activity duration, i.e. scoring, we
				 * may have to consider giving these a duration of 24:00:00. */
			} else {
				/* Iterate over index, and not the PlanElement, as we need the index */
				for(int i = 0; i < selectedPlan.getPlanElements().size(); i++){
					PlanElement pe = selectedPlan.getPlanElements().get(i);
					if(pe instanceof Activity){
						double duration = 0.0;
						Activity act = (Activity) pe;
						if(i == 0){
							/* It is the first (home) activity. */
							duration = act.getEndTime();
							if(!act.getType().equalsIgnoreCase("h")){
								LOG.warn("Chain starting with activity other than `home': " 
										+ act.getType() + " (" + person.getId() + ")");
								durations.add(new Tuple<String, Double>(act.getType(), duration));
							} else{
								durations.add(new Tuple<String, Double>("h1", duration));
							}
							if(duration < 0){
								LOG.warn("First for " + person.getId().toString() + "!! Negative duration: " + duration);
							}
						} else if(i < selectedPlan.getPlanElements().size() - 1){
							/* It can be any activity. */
							if(act.getType().equalsIgnoreCase("h")){
								durations.add(new Tuple<String, Double>("h3", act.getEndTime() - act.getStartTime()));
							} else {
								if(act.getStartTime() == Double.NEGATIVE_INFINITY ||
										act.getEndTime() == Double.NEGATIVE_INFINITY){
									/* Rather use activity's maximum duration. */
									duration = act.getMaximumDuration();
								} else{
									duration = act.getEndTime() - act.getStartTime();
								}
								durations.add(new Tuple<String, Double>(act.getType(), duration));
							}
							if(duration < 0){
								LOG.warn("Mid for " + person.getId().toString() + "!! Negative duration: " + duration);
							}
						} else {
							/* It is the final (home) activity. */
							duration = Math.max(24*60*60, act.getStartTime()) - act.getStartTime();
							if(duration != Double.POSITIVE_INFINITY){
								if(!act.getType().equalsIgnoreCase("h")){
									LOG.warn("Chain ending with activity other than `home': " 
											+ act.getType() + " (" + person.getId() + ")");
									durations.add(new Tuple<String, Double>(act.getType(), duration));
								} else{
									durations.add(new Tuple<String, Double>("h2", duration));
								}
								if(duration < 0){
									LOG.warn("LAST!! Negative duration: " + duration);
								}
							}
						}
					}
				}
			}
		}
		LOG.warn("Maximum duration (shopping): " + maxS);
		
		/* Write the output. */
		String filename = outputFile;
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("Type,Duration");
			bw.newLine();
			for(Tuple<String, Double> tuple : durations){
				bw.write(tuple.getFirst());
				/* In minutes. */
				bw.write(String.format(",%.2f\n", tuple.getSecond() / 60)); 
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedWriter "
					+ filename);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ filename);
			}
		}
	}


	/**
	 * Reads in a {@link Households} file, and prints the number of 
	 * {@link Household}s, and the total number of members indicated for the 
	 * households. 
	 * @param householdsFilename
	 */
	public static void printHouseholdStatistics(String householdsFilename){
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 hr = new HouseholdsReaderV10(households);
		hr.readFile(householdsFilename);
		
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
		PopulationReader pr = new PopulationReader(sc);
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
		PopulationReader pr = new PopulationReader(sc);
		pr.readFile(populationFilename);
		
		LOG.info("Population parsed. Analysing activity types...");
		Counter counter = new Counter(" person # ");
		Map<String, Integer> map = new TreeMap<String, Integer>();
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(id).getSelectedPlan();
			if(plan != null){
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
	
	
	public static void printNumberOfEmployedPersons(String population){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new PopulationReader(sc);
		pr.readFile(population);
		
		LOG.info("Population parsed. Analysing employment types...");
		
		Counter counter = new Counter(" employed persons # ");
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			Person person = sc.getPopulation().getPersons().get(id);
			if(PersonUtils.isEmployed(person)){
				counter.incCounter();
			}
		}
		counter.printCounter();
		
		LOG.info("--------------------------------------------");
	}
	
	
	/**
	 * Identifies the agent type by the Id-prefix, and reports the number of 
	 * each type.
	 * 
	 * @param population
	 */
	public static void printNumberOfAgentTypes(String population){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new PopulationReader(sc);
		pr.readFile(population);
		
		Map<String, Integer> map = new TreeMap<String, Integer>();
		
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			String agentType = id.toString().split("_")[0];
			if(map.containsKey(agentType)){
				map.put(agentType, map.get(agentType)+1 );
			} else{
				map.put(agentType, 1);
			}
		}
		
		/* Report the statistics */
		LOG.info("--------------------------------------------");
		for(String s : map.keySet()){
			LOG.info("   " + s + ": " + String.valueOf(map.get(s)));
		}
		LOG.info("--------------------------------------------");
	}
	
	
	/**
	 * Identify all the activity chain types, as well as the number of 
	 * occurrences of each type.
	 * @param population
	 */
	public static void idActivityChainTypes(String population, String filename){
		LOG.info("Checking for activity chain types...");

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new PopulationReader(sc);
		pr.readFile(population);
		
		Map<String, Integer> map = new TreeMap<>();
		for(Id<Person> pid : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(pid).getSelectedPlan();
			String chain = "";
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					chain += ((Activity)pe).getType();
				} else{
					chain += "-";
				}
			}
			if(map.containsKey(chain)){
				int oldValue = map.get(chain);
				map.put(chain, oldValue + 1);
			} else{
				map.put(chain, 1);
			}
		}
		
		/* Write to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("chain,occurrences");
			bw.newLine();
			for(String s : map.keySet()){
				bw.write(String.format("%s,%d\n", s, map.get(s)));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done");
	}
	
	/**
	 * Report the number of activity chains, i.e. plans, and trips that have
	 * mode `ride`.
	 * 
	 * @param population
	 */
	public static void printNumberOfRideStatistics(String population){
		LOG.info("Checking for mode 'ride'...");
		
		int rideChains = 0;
		int chains = 0;
		int rideTrips = 0;
		int trips = 0;

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new PopulationReader(sc);
		pr.readFile(population);
		
		for(Id<Person> pid : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(pid).getSelectedPlan();
			
			boolean chainHasRide = false;
			
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					if(leg.getMode().equalsIgnoreCase("ride")){
						rideTrips++;
						chainHasRide = true;
					}
					trips++;
				}
			}
			chains++;
			if(chainHasRide){
				rideChains++;
			}
		}
		LOG.info("Done");
		LOG.info("----------------------------------------------");
		LOG.info("    Total number of trips: " + trips);
		LOG.info("   Trips with mode 'ride': " + rideTrips);
		LOG.info("   Total number of chains: " + chains);
		LOG.info("  Chains with mode 'ride': " + rideChains);
		LOG.info("----------------------------------------------");
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
		case 4:
			printNumberOfEmployedPersons(args[1]);
			break;
		case 5:
			printNumberOfAgentTypes(args[1]);
			break;
		case 6:
			idActivityChainTypes(args[1], args[2]);
			break;
		case 7:
			printNumberOfRideStatistics(args[1]);
			break;
		default:
			LOG.warn("Cannot print any statistics for option `" + option + "'");
		}
		
		Header.printFooter();
	}
	
	
}
