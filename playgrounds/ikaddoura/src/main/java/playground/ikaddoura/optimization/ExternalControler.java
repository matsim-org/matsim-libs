/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.ikaddoura.optimization.io.OptSettings;
import playground.ikaddoura.optimization.io.OptSettingsReader;
import playground.ikaddoura.optimization.io.PopFilePathsLoader;
import playground.ikaddoura.optimization.io.RndSeedsLoader;
import playground.ikaddoura.optimization.io.TextFileWriter;
import playground.ikaddoura.optimization.operator.Operator;
import playground.ikaddoura.optimization.users.Users;
import playground.ikaddoura.utils.prepare.PopulationWorkOtherGenerator;
import playground.ikaddoura.utils.pt.DeparturesGenerator;
import playground.ikaddoura.utils.pt.ScheduleFromCorridor;
import playground.ikaddoura.utils.pt.VehiclesGenerator;

/**
 * @author Ihab
 *
 */

class ExternalControler {
	
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String settingsFile;
	static String configFile;
	static String outputPath;
	
	static boolean useRandomSeedsFile;
	static boolean usePopulationPathsFile;
	static boolean calculate_inVehicleTimeDelayEffects;
	static boolean calculate_waitingTimeDelayEffects;
	static boolean calculate_capacityDelayEffects;
	static boolean marginalCostPricingPt;
	static boolean calculate_carCongestionEffects;
	static boolean marginalCostPricingCar;
	
	static long randomSeed;
	static String populationFile;
	
	static double incrHeadway;
	static double incrFare;
	static int incrCapacity;
	static int incrDemand;

	static double startHeadway;
	static double startFare;
	static int startCapacity;
	static int startDemand;
	
	static int stepsHeadway;
	static int stepsCapacity;
	static int stepsFare;
	static int stepsDemand;
	
	private Map<Double, Map<Integer, String>> headway2capacity2vehiclesFile = new HashMap<Double, Map<Integer, String>>();
	private Map<Double, String> headway2scheduleFile = new HashMap<Double, String>();
	private Map<Integer, String> demand2populationFile = new HashMap<Integer, String>();
	private SortedMap<Integer, IterationInfo> it2information = new TreeMap<Integer, IterationInfo>();
	private TextFileWriter textWriter = new TextFileWriter();
	private Operator operator = new Operator();
	private Users users = new Users();

	public static void main(final String[] args) throws IOException {
		
		log.info("Setting parameters...");
		if (args.length == 0){
			settingsFile = "/Users/Ihab/Desktop/optimization/input/settingsFile_MCP_inVeh.csv";
			configFile = "/Users/Ihab/Desktop/optimization/input/config.xml";
			outputPath = "/Users/Ihab/Desktop/optimization/output/MCP_inVeh_ohneMCoperator";
			
		} else {
			settingsFile = args[0];
			configFile = args[1];
			outputPath = args[2];
		}
				
		OptSettingsReader settingsReader = new OptSettingsReader(settingsFile);
		OptSettings settings = settingsReader.getOptSettings();
		
		incrHeadway = settings.getIncrHeadway();
		incrFare = settings.getIncrFare();
		incrCapacity = settings.getIncrCapacity();
		incrDemand = settings.getIncrDemand();
		
		startHeadway = settings.getStartHeadway();
		startFare = settings.getStartFare();
		startCapacity = settings.getStartCapacity();
		startDemand = settings.getStartDemand();

		stepsHeadway = settings.getStepsHeadway();
		stepsFare = settings.getStepsFare();
		stepsCapacity = settings.getStepsCapacity();
		stepsDemand = settings.getStepsDemand();
		
		useRandomSeedsFile = settings.isUseRandomSeedsFile();
		usePopulationPathsFile = settings.isUsePopulationPathsFile();
		
		calculate_inVehicleTimeDelayEffects = settings.isCalculating_inVehicleTimeDelayEffects();
		calculate_waitingTimeDelayEffects = settings.isCalculating_waitingTimeDelayEffects();
		calculate_capacityDelayEffects = settings.isCalculate_capacityDelayEffects();
		marginalCostPricingPt = settings.isMarginalCostPricingPt();
		calculate_carCongestionEffects = settings.isCalculate_carCongestionEffects();
		marginalCostPricingCar = settings.isMarginalCostPricingCar();
		
		if (useRandomSeedsFile){
			String randomSeedsFile = settings.getRandomSeedsFile();
			int rndSeedNr = Integer.parseInt(args[3]);
			RndSeedsLoader rndSeedsLoader = new RndSeedsLoader(randomSeedsFile);
			log.info("Looking up randomSeed #" + rndSeedNr + " from file " + randomSeedsFile + "...");
			randomSeed = rndSeedsLoader.getRandomSeed(rndSeedNr);
			log.info("Looking up randomSeed #" + rndSeedNr + " from file " + randomSeedsFile + "... Done. RandomSeed: " + randomSeed);
		} else {
			log.info("Random seed will be set via config.");
		}
		
		if (usePopulationPathsFile){
			String populationPathsFile = settings.getPopulationPathsFile();
			int popFilePathNr = Integer.parseInt(args[4]);
			PopFilePathsLoader popFilePathsLoader = new PopFilePathsLoader(populationPathsFile);
			log.info("Looking up population file path #" + popFilePathNr + " from file " + populationPathsFile + "...");
			populationFile = popFilePathsLoader.getPopulationFile(popFilePathNr);
			log.info("Looking up population file path #" + popFilePathNr + " from file " + populationPathsFile + "... Done. Population file: " + populationFile);
		} else {
			log.info("Population file will be set via config.");
		}
		
		log.info("Setting parameters... Done.");
		
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}

	private void run() throws IOException {
		
		checkSettings();
		createInputFiles();
		
		int iterationCounter = 0;
		
		double fare;
		int capacity;
		double headway;
		int demand;
		
		demand = startDemand;
		for (int demandStep = 0; demandStep <= stepsDemand ; demandStep++){
			
			headway = startHeadway;
			for (int busStep = 0; busStep <= stepsHeadway ; busStep++){
					
				capacity = startCapacity;
				for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
					
					fare = startFare;
					for (int fareStep = 0; fareStep <= stepsFare ; fareStep++){

						log.info("###################################################");
						log.info("### EXTERNAL ITERATION " + iterationCounter + " BEGINS");
					
						runInternalIteration(iterationCounter, demand, headway, capacity, fare);
						
						log.info("### EXTERNAL ITERATION " + iterationCounter + " ENDS");
						log.info("###################################################");
						
						iterationCounter++;
						
						if (fareStep < stepsFare){
							fare = fare + incrFare;
						}	
					}
					
					if (capacityStep < stepsCapacity){
						capacity = capacity + incrCapacity;
					}
				}
				
				if (busStep < stepsHeadway){
					headway = headway + incrHeadway;
				}					
			}
			
			if (demandStep < stepsDemand){
				demand = demand + incrDemand;
			}
		}
	}
	
	private void runInternalIteration(int iterationCounter, int demand, double headway, int capacity, double fare) throws IOException {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));

		String directoryIt = outputPath + "/extITERS/" + iterationCounter;

		File directory = new File(directoryIt);
		directory.mkdirs();
		scenario.getConfig().controler().setOutputDirectory(directoryIt);
		
		String scheduleFile = this.headway2scheduleFile.get(headway);
		String vehiclesFile = this.headway2capacity2vehiclesFile.get(headway).get(capacity);
		
		if (usePopulationPathsFile) {
			scenario.getConfig().plans().setInputFile(populationFile);
		} else {
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
		}
		
		new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		demand = scenario.getPopulation().getPersons().size();
		
		scenario.getConfig().transit().setTransitScheduleFile(scheduleFile);
		scenario.getConfig().transit().setVehiclesFile(vehiclesFile);
		
		if (randomSeed==0) {
			log.info("Random seed is taken from configFile. Random seed: " + scenario.getConfig().global().getRandomSeed());
		} else {
			log.info("Random seed is not taken from configFile. Setting random seed to " + randomSeed);
			scenario.getConfig().global().setRandomSeed(randomSeed);
		}
		
		InternalControler internalControler = new InternalControler(
				scenario,
				fare,
				calculate_inVehicleTimeDelayEffects, 
				calculate_waitingTimeDelayEffects,
				calculate_capacityDelayEffects,
				marginalCostPricingPt,
				calculate_carCongestionEffects,
				marginalCostPricingCar);
		
		internalControler.run();
		deleteUnnecessaryInternalIterations(scenario); 

		operator.setParametersForExtIteration(capacity);
		users.setParametersForExtIteration(scenario);
		
		OperatorUserAnalysis analysis = new OperatorUserAnalysis(scenario, headway);
		analysis.readEvents();
		
		operator.calculateCosts(analysis);
		users.calculateLogsum();
			
		IterationInfo info = new IterationInfo();
		info.setFare(fare);
		info.setCapacity(capacity);
		info.setNumberOfBuses(analysis.getOperatorCostHandler().getVehicleIDs().size());
		info.setHeadway(headway);
		info.setTotalDemand(demand);
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
		info.setNumberOfBoardingDeniedEvents(analysis.getWaitHandler().getBoardingDeniedEvents());
		
		info.setMaxDepartureDelay(analysis.getWaitHandler().getMaxDepartDelay());
		info.setMaxArrivalDelay(analysis.getWaitHandler().getMaxArriveDelay());
		
		info.setAvgT0MinusTActPerPerson(analysis.getCongestionHandler().getAvgTActMinusT0PerPerson());
		info.setT0MinusTActSum(analysis.getCongestionHandler().getTActMinusT0Sum());
		info.setAvgT0MinusTActDivT0PerTrip(analysis.getCongestionHandler().getAvgT0minusTActDivT0PerCarTrip());
		
		info.setAverageFarePerAgent(analysis.getAverageFarePerAgent());
		
		this.it2information.put(iterationCounter, info);
		
		this.textWriter.writeExtItData(outputPath, this.it2information);
		this.textWriter.writeMatrices(outputPath, this.it2information);
		this.textWriter.writeFareData(directoryIt, analysis.getFareData());
		this.textWriter.wrtieFarePerTime(directoryIt, analysis.getAvgFarePerDepartureTimePeriod());
		this.textWriter.writeTripFarePerId(directoryIt, analysis.getFirstTripFares(), analysis.getSecondTripFares());
		
		iterationCounter++;
	}

	private void createInputFiles() throws IOException {

		String dir = outputPath + "/generatedInputFiles/";
		File directory = new File(dir);
		directory.mkdirs();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
		
		if (startDemand == 0 && incrDemand == 0 && stepsDemand == 0){
			if (usePopulationPathsFile){
				log.info("Population paths file enabled. Expecting population file paths.");
			} else {
				log.info("Population paths file disabled. Expecting population file in config.");
			}
		} else {
			int demand = startDemand;
			for (int demandStep = 0; demandStep <= stepsDemand; demandStep++){
			
				log.info("Writing population...");
				String populationFile = dir + "population" + demand + ".xml";
				PopulationWorkOtherGenerator pG = new PopulationWorkOtherGenerator(scenario);
				pG.writePopulation(demand, populationFile);
		
				this.demand2populationFile.put(demand, populationFile);
				demand = demand + incrDemand;
			}
		}
		
		double headway = startHeadway;
		for (int headwayStep = 0; headwayStep <= stepsHeadway ; headwayStep++){
			
			// public transport settings
			String transitRouteMode = "bus";
			boolean isBlocking = false;
			boolean awaitDeparture = true;
			double scheduledTravelTime = 60.;
			double scheduledStopTime = 0.;
			double startService = 4. * 3600.;
			double endService = 24. * 3600.;
			double slackTime = 1200.;
			Id<Vehicle> ptVehId = Id.create("bus", Vehicle.class);
			double alightingTime = 0.75;
			double boardingTime = 1.0;
			DoorOperationMode doorOperationMode = DoorOperationMode.serial;
			double ptVehiclePCU = 1.0;
			double maxVelocity = 500./59.;		
			
			log.info("Writing transitSchedule...");
			TransitSchedule schedule;
			String scheduleFile = dir + "transitSchedule_headway" + headway + ".xml";
			
			ScheduleFromCorridor sfn = new ScheduleFromCorridor(scenario.getNetwork());
			sfn.createTransitSchedule(transitRouteMode, isBlocking, awaitDeparture, scheduledTravelTime, scheduledStopTime);
			schedule = sfn.getTransitSchedule();
			
			List<Id<TransitLine>> lineIDs = new ArrayList<Id<TransitLine>>();
			lineIDs.addAll(schedule.getTransitLines().keySet());
			 
			DeparturesGenerator dg = new DeparturesGenerator();
			dg.addDepartures(schedule, lineIDs, headway, startService, endService, slackTime);
			
			TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
			scheduleWriter.write(scheduleFile);
			
			int capacity = startCapacity;
			for (int capacityStep = 0; capacityStep <= stepsCapacity ; capacityStep++){
			
				log.info("Writing transitVehicles...");
				Vehicles vehicles;
				String vehiclesFile = dir + "transitVehicles_headway" + headway + "_capacity" + capacity + ".xml";
				
				double length = (0.1184 * capacity + 5.2152);	// Data from Australian Transport Council
				int busSeats = (int) (capacity * 1.);
				int standingRoom = (int) (capacity * 0.); // for future functionality (e.g. disutility for standing in bus)
				
				VehiclesGenerator vg = new VehiclesGenerator();				
				vg.createVehicles(
						schedule, 
						lineIDs, 
						busSeats,
						standingRoom, 
						length,
						ptVehId,
						alightingTime,
						boardingTime,
						doorOperationMode,
						ptVehiclePCU,
						maxVelocity);
				vehicles = vg.getVehicles();
				VehicleWriterV1 vehicleWriter = new VehicleWriterV1(vehicles);
				vehicleWriter.writeFile(vehiclesFile);
				
				if (this.headway2capacity2vehiclesFile.containsKey(headway)){
					this.headway2capacity2vehiclesFile.get(headway).put(capacity, vehiclesFile);
					
				} else {
					Map<Integer, String> capacity2vehiclesFile = new HashMap<Integer, String>();
					capacity2vehiclesFile.put(capacity, vehiclesFile);
					this.headway2capacity2vehiclesFile.put(headway, capacity2vehiclesFile);
				}
				capacity = capacity + incrCapacity;
			}
			
			this.headway2scheduleFile.put(headway, scheduleFile);			
			headway = headway + incrHeadway;
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

	private void checkSettings() {
		
		if (calculate_inVehicleTimeDelayEffects){
			log.info("Calculating in-vehicle time delay effects enabled.");
		} else {
			log.info("Calculating in-vehicle time delay effects disabled.");
		}
		
		if (calculate_waitingTimeDelayEffects){
			log.info("Calculating waiting time delay effects enabled.");
		} else {
			log.info("Calculating waiting time delay effects disabled.");
		}
		
		if (marginalCostPricingPt){
			log.info("Marginal cost pricing for public transport enabled.");
		} else {
			log.info("Marginal cost pricing for public transport disabled.");
		}
		
		if (calculate_carCongestionEffects){
			log.info("Calculating car congestion effects enabled.");
		} else {
			log.info("Calculating car congestion effects disabled.");
		}
		
		if (marginalCostPricingCar){
			log.info("Marginal cost pricing for car enabled.");
		} else {
			log.info("Marginal cost pricing for car disabled.");
		}
		
		if (usePopulationPathsFile){
			incrDemand = 0;
			stepsDemand = 0;
			startDemand = 0;
			log.info("Demand variation disabled.");
		}
		
		if (incrHeadway==0 || stepsHeadway==0){
			incrHeadway = 0;
			stepsHeadway = 0;
		}
		if (incrFare==0 || stepsFare==0){
			incrFare = 0;
			stepsFare = 0;
		}
		if (incrCapacity==0 || stepsCapacity==0){
			incrCapacity = 0;
			stepsCapacity = 0;
		}
		if (incrDemand==0 || stepsDemand==0){
			incrDemand = 0;
			stepsDemand = 0;
		}
	}
}