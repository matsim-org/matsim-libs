/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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
package playground.southafrica.population.freight.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to generate a population of commercial vehicles where the plans are
 * bootstrap sampled from observed activity chains for a given set of vehicles
 * on a given day (type).
 * 
 * @author jwjoubert
 */
public class BootstrapPopulationGenerator {
	final private static Logger LOG = Logger.getLogger(BootstrapPopulationGenerator.class);
	
	private Map<Integer, Integer> chainMap;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(BootstrapPopulationGenerator.class.toString(), args);
		
		String xmlFolder = args[0];
		String vehicleIds = args[1];
		int dayType = Integer.parseInt(args[2]);
		String abnormalDaysFile = args[3];
		int numberToGenerate = Integer.parseInt(args[4]);
		String outputFolder = args[5];
		
		/* Read the list of vehicle files */
		List<File> vehicles = null;
		try {
			vehicles = DigicoreUtils.readDigicoreVehicleIds(vehicleIds, xmlFolder);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get the necessary vehicle files.");
		}
		
		/* Read the list of abnormal days. */
		List<Integer> abnormalDays = DigicoreUtils.readDayOfYear(abnormalDaysFile);
		
		BootstrapPopulationGenerator bpg = new BootstrapPopulationGenerator();
		
		/* Check the number of activity chains eligible for sampling. 
		 * IMPORTANT: Here I hard code the MATSim Random value to use. This is
		 * for repeatability purposes.*/
		MatsimRandom.reset(20150317l);
		int totalChains = bpg.checkSampleEligibility(vehicles, abnormalDays, dayType);
		bpg.buildBootstrapMap(totalChains, numberToGenerate, MatsimRandom.getLocalInstance());
		Scenario sc = bpg.buildPopulation(vehicles, abnormalDays, dayType);
		
		/* Write the population to file. */
		String populationFile = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "population.xml";
		new PopulationWriter(sc.getPopulation()).write(populationFile);
		
		Header.printFooter();
	}
	
	public BootstrapPopulationGenerator() {

	}
	
	
	public int checkSampleEligibility(List<File> vehicles, List<Integer> abnormalDays, int dayType){
		int result = 0;
		
		Counter counter = new Counter("  vehicle # ");
		for(File f : vehicles){
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Check each activity chain. */
			for(DigicoreChain chain : dv.getChains()){
				if(useActivityChain(chain, dayType, abnormalDays)){
					result++;
				}
			}
			
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Total number of activity chains eligible for sampling: " + result);
		return result;
	}
	
	
	/**
	 * Procedure to effectively bootstrap from the activity chain indices. The
	 * procedure is as follows:
	 * <ol>
	 * 		<li> sample (with replacement) an activity chain index;
	 * 		<li> if the index has already been sampled; increment the number of
	 * 			 observations;
	 * 		<li> repeat (1)-(2) until the required number of chains is sampled. 
	 * </ol>
	 * 
	 * @param numberOfChains
	 * @param numberToSample
	 * @param random
	 */
	public void buildBootstrapMap(int numberOfChains, int numberToSample, Random random){
		LOG.info("Building a bootstrap map of " + numberOfChains + " integers.");
		chainMap = new TreeMap<Integer, Integer>();
		for(int i = 0; i < numberToSample; i++){
			Integer value = Integer.valueOf(random.nextInt(numberOfChains));
			if(!chainMap.containsKey(value)){
				chainMap.put(Integer.valueOf(value), 1);
			} else{
				int oldValue = chainMap.get(value);
				chainMap.put(value, oldValue+1);
			}
		}
		printBootstrapMap();
		LOG.info("Done building bootstrap map.");
	}
	
	
	/**
	 * Just a small method to show how many times each activity chain index
	 * was sampled. Thie method is only needed during the development phase.
	 */
	private void printBootstrapMap(){
		LOG.info("Bootstrap map:");
		for(Integer i : chainMap.keySet()){
			LOG.info(String.format("  %6d: %d", i, chainMap.get(i)));
		}
	}
	
	
	/**
	 * Procedure to check if a given activity chain is, or should be considered
	 * or not. For now (JWJ, March 2015) it just checks if the start day of the
	 * activity chain is the same day type specified as argument.
	 * 
	 * @param chain
	 * @return
	 */
	public boolean useActivityChain(DigicoreChain chain, int dayType, List<Integer> abnormalDays){
		boolean result = false;
		if(dayType == chain.getChainStartDay(abnormalDays)){
			result = true;
		}
		return result;
	}
	
	public Scenario buildPopulation(List<File> vehicles, List<Integer> abnormalDays, int dayType){
		LOG.info("Building the scenario population...");
		
		/* First check to ensure the bootstrap map has already been populated. */
		if(chainMap == null){
			throw new RuntimeException("There is no bootstrap map!!");
		}
		
		/* Build the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sc.getPopulation().getFactory();
		int index = 0;
		Counter counter = new Counter("  vehicle # ");
		for(File f : vehicles){
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Check each activity chain. */
			for(DigicoreChain chain : dv.getChains()){
				if(useActivityChain(chain, dayType, abnormalDays)){
					/* Yes, this is an eligible activity chain. Now check if 
					 * it has actually been sampled. */
					if(chainMap.containsKey(Integer.valueOf(index))){
						int repeats = chainMap.get(Integer.valueOf(index));
						
						/* Repeat this activity chain 'n' times. */
						Plan plan = convertDigicoreChainToPlan(pf, chain);
						for(int i = 0; i < repeats; i++){
							long id = sc.getPopulation().getPersons().size();
							Person person = pf.createPerson(Id.createPersonId(id));
							person.addPlan(plan);
							sc.getPopulation().addPerson(person);
						}
					}
					index++;
				}
			}
			
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Completed scenario: " + sc.getPopulation().getPersons().size() + " persons");
		return sc;
	}
	
	
	public Plan convertDigicoreChainToPlan(PopulationFactory pf, DigicoreChain chain){
		Plan plan = new PlanImpl();
		
		/* Create the first major activity. */
		DigicoreActivity firstMajor = chain.getFirstMajorActivity();
		Activity a1 = pf.createActivityFromCoord(firstMajor.getType(), firstMajor.getCoord());
		/* Determine number of days (24-hours) to deduct from time stamp. */
		int daysToDeduct = 0;
		double endTime = firstMajor.getEndTime();
		while(endTime > 24*60*60){
			endTime -= 24*60*60;
			daysToDeduct++;
		}
		a1.setEndTime(endTime);
		/* Add the facilityId if available. */
		if(firstMajor.getFacilityId() != null){
			((ActivityImpl)a1).setFacilityId(firstMajor.getFacilityId());
		}

		plan.addActivity(a1);

		/* Add all the minor activities. */
		for(int i = 0; i < chain.getMinorActivities().size(); i++){
			/* First add the leg. */
			plan.addLeg(pf.createLeg("commercial"));
			
			DigicoreActivity activity = chain.getMinorActivities().get(i);
			Activity minor = pf.createActivityFromCoord(activity.getType(), activity.getCoord());
			minor.setMaximumDuration(activity.getDuration());
			/* Add the facilityId if available. */
			if(activity.getFacilityId() != null){
				((ActivityImpl)minor).setFacilityId(activity.getFacilityId());
			}

			plan.addActivity(minor);
		}
		
		/* Add the final leg and major activity. */
		plan.addLeg(pf.createLeg("commercial"));
	
		DigicoreActivity lastMajor = chain.getLastMajorActivity();
		Activity a2 = pf.createActivityFromCoord(lastMajor.getType(), lastMajor.getCoord());
		a2.setStartTime(lastMajor.getStartTime()-(daysToDeduct*24*60*60));
		/* Add the facilityId if available. */
		if(lastMajor.getFacilityId() != null){
			((ActivityImpl)a2).setFacilityId(lastMajor.getFacilityId());
		}

		plan.addActivity(a2);
		
		return plan;
	}

	
	
	
	
	
	
	
	
}
