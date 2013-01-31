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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
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
	static int incrDemand;

	static int startBusNumber;
	static double startFare;
	static int startCapacity;
	static int startDemand;
	
	static int stepsBusNumber;
	static int stepsCapacity;
	static int stepsFare;
	static int stepsDemand;
	
	static String settingsFile;

	private Map<Integer, Map<Integer, String>> numberOfbuses2capacity2vehiclesFile = new HashMap<Integer, Map<Integer, String>>();
	private Map<Integer, String> numberOfbuses2scheduleFile = new HashMap<Integer, String>();
	private Map<Integer, String> demand2populationFile = new HashMap<Integer, String>();
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
			outputPath = "/Users/Ihab/Desktop/output2";			
		} else {
			settingsFile = args[0];
			configFile = args[1];
			outputPath = args[2];
			
			if (args.length > 3) {
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
		incrDemand = settings.getIncrDemand();
		
		startBusNumber = settings.getStartBusNumber();
		startFare = settings.getStartFare();
		startCapacity = settings.getStartCapacity();
		startDemand = settings.getStartDemand();

		stepsBusNumber = settings.getStepsBusNumber();
		stepsFare = settings.getStepsFare();
		stepsCapacity = settings.getStepsCapacity();
		stepsDemand = settings.getStepsDemand();
		
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
		int demand;
		
		demand = startDemand;
		for (int demandStep = 0; demandStep <= stepsDemand ; demandStep++){
			numberOfBuses = startBusNumber;
			for (int busStep = 0; busStep <= stepsBusNumber ; busStep++){
					
				capacity = startCapacity;
				for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
					
					fare = startFare;
					for (int fareStep = 0; fareStep <= stepsFare ; fareStep++){

						log.info("*********************************************************");
						log.info("demand: " + demand + " // number of buses: " + numberOfBuses + " // fare: " + fare + " // capacity: " + capacity);
						runInternalIteration(iterationCounter, demand, numberOfBuses, capacity, fare);
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
			
			if (demandStep < stepsDemand){
				demand = demand + incrDemand;
			}
		}
	}
	
	private void runInternalIteration(int iterationCounter, int demand, int numberOfBuses, int capacity, double fare) throws IOException {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));

		String directoryIt = outputPath + "/extITERS/" + iterationCounter + "_demand" + demand + "_buses" + numberOfBuses + "_fare" + fare + "_capacity" + capacity;
		File directory = new File(directoryIt);
		directory.mkdirs();
		scenario.getConfig().controler().setOutputDirectory(directoryIt);
		
		String scheduleFile = this.numberOfbuses2scheduleFile.get(numberOfBuses);
		String vehiclesFile = this.numberOfbuses2capacity2vehiclesFile.get(numberOfBuses).get(capacity);
		
		if (this.demand2populationFile.isEmpty()) {
			String populationFile = scenario.getConfig().plans().getInputFile();
			if (populationFile==null){
				throw new RuntimeException("Missing populationFile in config.");
			} else {
				log.info("PopulationFile from config: " + scenario.getConfig().plans().getInputFile());
			}
		} else {
			String populationFile = this.demand2populationFile.get(demand);
			scenario.getConfig().plans().setInputFile(populationFile);
		}
		
		scenario.getConfig().transit().setTransitScheduleFile(scheduleFile);
		scenario.getConfig().transit().setVehiclesFile(vehiclesFile);
		
		if (randomSeed==0) {
			log.info("Random seed is taken from configFile. Random seed: " + scenario.getConfig().global().getRandomSeed());
		} else {
			log.info("Random seed is not taken from configFile. Setting random seed to " + randomSeed);
			scenario.getConfig().global().setRandomSeed(randomSeed);
		}
		
		InternalControler internalControler = new InternalControler(scenario, fare);
		internalControler.run();
		
		deleteUnnecessaryInternalIterations(scenario); 

		operator.setParametersForExtIteration(capacity);
		users.setParametersForExtIteration(scenario);
		
		OperatorUserAnalysis analysis = new OperatorUserAnalysis(scenario, this.umlaufzeit/numberOfBuses);
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
		
		if (startDemand == 0 && incrDemand == 0 && stepsDemand == 0){
			log.info("No populationFile written. Expecting populationFile in config.");
		} else {
			int demand = startDemand;
			for (int demandStep = 0; demandStep <= stepsDemand; demandStep++){
			
				log.info("Writing population...");
				String populationFile = dir + "population" + demand + ".xml";
				PopulationGenerator pG = new PopulationGenerator(scenario);
				pG.writePopulation(demand, populationFile);
		
				this.demand2populationFile.put(demand, populationFile);
				demand = demand + incrDemand;
			}
		}
		
		
		
		int numberOfBuses = startBusNumber;
		for (int busStep = 0; busStep <= stepsBusNumber ; busStep++){
			
			log.info("Writing transitSchedule...");
			String scheduleFile = dir + "transitSchedule_buses" + numberOfBuses + ".xml";
			
			ScheduleWriter sw = new ScheduleWriter(scenario.getNetwork());
			sw.writeSchedule(numberOfBuses, scheduleFile);
			
			int capacity = startCapacity;
			for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
			
				log.info("Writing transitVehicles...");
				String vehiclesFile = dir + "transitVehicles_buses" + numberOfBuses + "_capacity" + capacity + ".xml";
				VehicleWriter vw = new VehicleWriter();
				vw.writeVehicles(numberOfBuses, capacity, vehiclesFile);
				
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

	private void deleteUnnecessaryInternalIterations(Scenario scenario) {
		String itersPath = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/";
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
		if (incrDemand==0 || stepsDemand==0){
			incrDemand = 0;
			stepsDemand = 0;
			log.info("Constant demand");
		}
	}
}