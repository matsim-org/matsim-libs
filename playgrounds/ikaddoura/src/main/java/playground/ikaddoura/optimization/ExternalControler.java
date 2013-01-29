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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
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

	private SortedMap<Integer, ExtItInformation> it2information = new TreeMap<Integer, ExtItInformation>();
	private TextFileWriter textWriter = new TextFileWriter();
	
	private double fare;
	private int capacity;
	private int numberOfBuses;

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
		
		Operator operator = new Operator();
		Users users = new Users();

		int iterationCounter = 0;
		
		this.numberOfBuses = startBusNumber;
		for (int extItParam0 = 0; extItParam0 <= stepsBusNumber ; extItParam0++){
			log.info("************* EXTERNAL ITERATION (0) " + extItParam0 + " BEGINS *************");
			
			this.fare = startFare;
			for (int extItParam1 = 0; extItParam1 <= stepsFare ; extItParam1++){
				log.info("************* EXTERNAL ITERATION (1) " + extItParam1 + " BEGINS *************");
				
				this.capacity = startCapacity;
				for (int extItParam2 = 0; extItParam2 <= stepsCapacity ; extItParam2++){
					log.info("************* EXTERNAL ITERATION (2) " + extItParam2 + " BEGINS *************");
					log.info("number of buses: " + this.numberOfBuses + " // fare: " + this.fare + " // capacity: " + this.capacity);
					
					String directoryIt = outputPath + "/extITERS/" + iterationCounter + "_buses" + this.numberOfBuses + "_fare" + this.fare + "_capacity" + this.capacity;
					File directory = new File(directoryIt);
					directory.mkdirs();
					
					Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
			
					new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
					new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
						
					VehicleScheduleWriter vsw = new VehicleScheduleWriter(this.numberOfBuses, this.capacity, scenario.getNetwork(), directoryIt);
					vsw.writeTransitVehiclesAndSchedule();
					
					InternalControler internalControler = new InternalControler(scenario, directoryIt, this.fare, randomSeed);
					internalControler.run();
					
					deleteUnnecessaryInternalIterations(directoryIt+"/internalIterations/ITERS/", scenario); 
		
					operator.setParametersForExtIteration(this.capacity);
					users.setParametersForExtIteration(scenario);
					
					OperatorUserAnalysis analysis = new OperatorUserAnalysis(scenario, directoryIt, vsw.getHeadway());
					analysis.readEvents();
					
					operator.calculateCosts(analysis);
					users.calculateLogsum();
						
					ExtItInformation info = new ExtItInformation();
					info.setFare(this.fare);
					info.setCapacity(this.capacity);
					info.setNumberOfBuses(this.numberOfBuses);
					info.setHeadway(vsw.getHeadway());
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
					
					if (extItParam2 < stepsCapacity){
						this.capacity = this.capacity + incrCapacity;
					}
					
					log.info("************* EXTERNAL ITERATION (2) " + extItParam2 + " ENDS *************");
				}
				
				if (extItParam1 < stepsFare){
					this.fare = this.fare + incrFare;
				}
				
				log.info("************* EXTERNAL ITERATION (1) " + extItParam1 + " ENDS *************");
			}
			
			if (extItParam0 < stepsBusNumber){
				this.numberOfBuses = this.numberOfBuses + incrBusNumber;
			}
			
			log.info("************* EXTERNAL ITERATION (0) " + extItParam0 + " ENDS *************");
					
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