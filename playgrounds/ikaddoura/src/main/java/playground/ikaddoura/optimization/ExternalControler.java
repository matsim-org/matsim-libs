/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.optimization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.optimization.analysis.Operator;
import playground.ikaddoura.optimization.analysis.OperatorUserAnalysis;
import playground.ikaddoura.optimization.analysis.Users;
import playground.ikaddoura.optimization.io.OptSettings;
import playground.ikaddoura.optimization.io.OptSettingsReader;
import playground.ikaddoura.optimization.io.RndSeedsLoader;
import playground.ikaddoura.optimization.io.TextFileWriter;

/**
 * @author benjamin and Ihab
 *
 */

class ExternalControler {
	
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String configFile;
	static String outputPath;
	static String rndSeedsFile;
	static int rndSeedNr;
	static long randomSeed;
	
	static int incrBusNumber;
	static double incrFare;
	static int incrCapacity;

	static int startBusNumber;
	static double startFare;
	static int startCapacity;
	
	static int stepsBusNumber;
	static int stepsCapacity;
	static int stepsFare;
	
	static String settingsFile;

	private Map<Integer, Map<Integer, String>> numberOfbuses2capacity2vehiclesFile = new HashMap<Integer, Map<Integer, String>>();
	private Map<Integer, String> numberOfbuses2scheduleFile = new HashMap<Integer, String>();
	private SortedMap<Integer, IterationInfo> it2information = new TreeMap<Integer, IterationInfo>();
	private TextFileWriter textWriter = new TextFileWriter();
	private Operator operator = new Operator();
	private Users users = new Users();

	private double umlaufzeit;

	public static void main(final String[] args) throws IOException {
		
		log.info("Setting parameters...");
		if (args.length == 0){
			settingsFile = "/Users/Ihab/Desktop/input/settingsFile.csv";
			configFile = "/Users/Ihab/Desktop/input/config.xml";
			outputPath = "/Users/Ihab/Desktop/output";			
		} else {
			settingsFile = args[0];
			configFile = args[1];
			outputPath = args[2];
			
			if (!args[3].isEmpty() && !args[4].isEmpty()){
				rndSeedsFile = args[3];		
				rndSeedNr = Integer.parseInt(args[4]);
				RndSeedsLoader rndSeedsLoader = new RndSeedsLoader(rndSeedsFile);
				log.info("Looking up randomSeed #" + rndSeedNr + " from file " + rndSeedsFile + "...");
				randomSeed = rndSeedsLoader.getRandomSeed(rndSeedNr);
				log.info("Looking up randomSeed #" + rndSeedNr + " from file " + rndSeedsFile + "... Done. RandomSeed: " + randomSeed);
			}
		}
				
		OptSettingsReader settingsReader = new OptSettingsReader(settingsFile);
		OptSettings settings = settingsReader.getOptSettings();
		
		incrBusNumber = settings.getIncrBusNumber();
		incrFare = settings.getIncrFare();
		incrCapacity = settings.getIncrCapacity();
		
		startBusNumber = settings.getStartBusNumber();
		startFare = settings.getStartFare();
		startCapacity = settings.getStartCapacity();

		stepsBusNumber = settings.getStepsBusNumber();
		stepsFare = settings.getStepsFare();
		stepsCapacity = settings.getStepsCapacity();
		
		log.info("Setting parameters... Done.");
		
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}

	private void run() throws IOException {
		
		checkConsitency();
		createInputFiles();
		
		int iterationCounter = 0;
		
		double fare;
		int capacity;
		int numberOfBuses;
		
		numberOfBuses = startBusNumber;
		for (int busStep = 0; busStep <= stepsBusNumber ; busStep++){
				
			capacity = startCapacity;
			for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
				
				fare = startFare;
				for (int fareStep = 0; fareStep <= stepsFare ; fareStep++){

					log.info("*********************************************************");
					log.info("number of buses: " + numberOfBuses + " // fare: " + fare + " // capacity: " + capacity);
					runInternalIteration(iterationCounter, numberOfBuses, capacity, fare);
					iterationCounter++;
					
					if (fareStep < stepsFare){
						fare = fare + incrFare;
					}	
				}
				
				if (capacityStep < stepsCapacity){
					capacity = capacity + incrCapacity;
				}
			}
			
			if (busStep < stepsBusNumber){
				numberOfBuses = numberOfBuses + incrBusNumber;
			}					
		}
	}
	
	private void runInternalIteration(int iterationCounter, int numberOfBuses, int capacity, double fare) throws IOException {
		
		String directoryIt = outputPath + "/extITERS/" + iterationCounter + "_buses" + numberOfBuses + "_fare" + fare + "_capacity" + capacity;
		File directory = new File(directoryIt);
		directory.mkdirs();
		
		String scheduleFile = this.numberOfbuses2scheduleFile.get(numberOfBuses);
		String vehiclesFile = this.numberOfbuses2capacity2vehiclesFile.get(numberOfBuses).get(capacity);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
		new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		
		InternalControler internalControler = new InternalControler(scenario, directoryIt, fare, randomSeed, scheduleFile, vehiclesFile);
		internalControler.run();
		
		deleteUnnecessaryInternalIterations(directoryIt+"/internalIterations/ITERS/", scenario); 

		operator.setParametersForExtIteration(capacity);
		users.setParametersForExtIteration(scenario);
		
		OperatorUserAnalysis analysis = new OperatorUserAnalysis(scenario, directoryIt, this.umlaufzeit/numberOfBuses);
		analysis.readEvents();
		
		operator.calculateCosts(analysis);
		users.calculateLogsum();
			
		IterationInfo info = new IterationInfo();
		info.setFare(fare);
		info.setCapacity(capacity);
		info.setNumberOfBuses(numberOfBuses);
		info.setHeadway(this.umlaufzeit/numberOfBuses);
		info.setOperatorCosts(operator.getCosts());
		info.setOperatorRevenue(analysis.getRevenue());
		info.setUsersLogSum(users.getLogSum());
		info.setNumberOfCarLegs(analysis.getSumOfCarLegs());
		info.setNumberOfPtLegs(analysis.getSumOfPtLegs());
		info.setNumberOfWalkLegs(analysis.getSumOfWalkLegs());
		info.setNoValidPlanScore(users.getNoValidPlanScore());
		
		info.setWaitingTimes(analysis.getWaitHandler().getWaitingTimes());
		info.setWaitingTimesNotMissed(analysis.getWaitHandler().getWaitingTimesNotMissed());
		info.setWaitingTimesMissed(analysis.getWaitHandler().getWaitingTimesMissed());
		info.setPersonId2waitingTimes(analysis.getWaitHandler().getPersonId2waitingTimes());
		info.setNumberOfMissedVehicles(analysis.getWaitHandler().getNumberOfMissedVehicles());
						
		info.setAvgT0MinusTActPerPerson(analysis.getCongestionHandler().getAvgTActMinusT0PerPerson());
		info.setT0MinusTActSum(analysis.getCongestionHandler().getTActMinusT0Sum());
		info.setAvgT0MinusTActDivT0PerTrip(analysis.getCongestionHandler().getAvgT0minusTActDivT0PerCarTrip());
		
		this.it2information.put(iterationCounter, info);
		
		iterationCounter++;
		this.textWriter.writeExtItData(outputPath, this.it2information);
		this.textWriter.writeMatrices(outputPath, this.it2information);
	}

	private void createInputFiles() throws IOException {

		String dir = outputPath + "/generatedInputFiles/";
		File directory = new File(dir);
		directory.mkdirs();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
		new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		
		int numberOfBuses = startBusNumber;
		for (int busStep = 0; busStep <= stepsBusNumber ; busStep++){
			
			log.info("Writing transitSchedule...");
			String scheduleFile = dir + "transitSchedule_buses" + numberOfBuses + ".xml";
			
			ScheduleWriter sw = new ScheduleWriter(scenario.getNetwork());
			sw.createSchedule(numberOfBuses, scheduleFile);
			
			int capacity = startCapacity;
			for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
			
				log.info("Writing transitVehicles...");
				String vehiclesFile = dir + "transitVehicles_buses" + numberOfBuses + "_capacity" + capacity + ".xml";
				VehicleWriter vw = new VehicleWriter();
				vw.writeVehicles(numberOfBuses, capacityStep, vehiclesFile);
				
				if (this.numberOfbuses2capacity2vehiclesFile.containsKey(numberOfBuses)){
					this.numberOfbuses2capacity2vehiclesFile.get(numberOfBuses).put(capacity, vehiclesFile);
					
				} else {
					Map<Integer, String> capacity2vehiclesFile = new HashMap<Integer, String>();
					capacity2vehiclesFile.put(capacity, vehiclesFile);
					this.numberOfbuses2capacity2vehiclesFile.put(numberOfBuses, capacity2vehiclesFile);
				}
				capacity = capacity + incrCapacity;
			}
			
			this.numberOfbuses2scheduleFile.put(numberOfBuses, scheduleFile);			
			numberOfBuses = numberOfBuses + incrBusNumber;
			
			this.umlaufzeit = sw.getUmlaufzeit(); // TODO: get umlaufzeit from somewhere else!
		}						
	}

	private void deleteUnnecessaryInternalIterations(String itersPath, Scenario scenario) {
		
		int firstIt = scenario.getConfig().controler().getFirstIteration();
		int lastIt = scenario.getConfig().controler().getLastIteration();
		log.info("Deleting unnecessary internal iteration output files...");
		
		for (int i = firstIt; i < lastIt ; i++){
			File path = new File(itersPath + "it." + i);
			deleteTree(path);
		}
		log.info("Deleting unnecessary internal iteration output files... Done.");
	}

	private void deleteTree( File path ) {
		
	  for ( File file : path.listFiles() )
	  {
	    if ( file.isDirectory() )
	      deleteTree( file );
	    file.delete();
	  }
	  path.delete();
	}

	private void checkConsitency() {
		
		if (incrBusNumber==0 || stepsBusNumber==0){
			incrBusNumber = 0;
			stepsBusNumber = 0;
			log.info("Constant headway");
		}
		if (incrFare==0 || stepsFare==0){
			incrFare = 0;
			stepsFare = 0;
			log.info("Constant fare");
		}
		if (incrCapacity==0 || stepsCapacity==0){
			incrCapacity = 0;
			stepsCapacity = 0;
			log.info("Constant capacity");
		}
	}
}