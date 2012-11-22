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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

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

/**
 * @author benjamin and Ihab
 *
 */

class ExternalControler {
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String configFile;
	static String outputExternalIterationDirPath;
	
	static int lastExternalIterationParam1;
	static int lastExternalIterationParam2;
	
	static OptimizationParameter1 op1;
	static OptimizationParameter2 op2;
	
	static int incrBusNumber;
	static double incrFare;
	static int incrCapacity;

	static int startBusNumber;
	static double startFare;
	static int startCapacity;
	
	static String settingsFile;

	private SortedMap<Integer, ExtItInformation> extIt2information = new TreeMap<Integer, ExtItInformation>();
	private SortedMap<Integer, ExtItInformation> it2information = new TreeMap<Integer, ExtItInformation>();
	private double fare;
	private int capacity;
	private int numberOfBuses;

	public static void main(final String[] args) throws IOException {
		
		settingsFile = args[0];
//		settingsFile = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/opt3/input_opt3/settingsFile.csv";
		
		OptSettingsReader settingsReader = new OptSettingsReader(settingsFile);
		OptSettings settings = settingsReader.getOptSettings();
		
		log.info("Setting run parameters...");

		configFile = settings.getConfigFile();
		outputExternalIterationDirPath = settings.getOutputPath();
		lastExternalIterationParam1 = settings.getLastExtIt1();
		lastExternalIterationParam2 = settings.getLastExtIt2();
	
		String op1String = settings.getOptimizationParameter1();

		if(op1String.equals(OptimizationParameter1.FARE.toString())){
			op1 = OptimizationParameter1.FARE;
		} else if(op1String.equals(OptimizationParameter1.CAPACITY.toString())){
			op1 = OptimizationParameter1.CAPACITY;
		} else if(op1String.equals(OptimizationParameter1.HEADWAY.toString())){
			op1 = OptimizationParameter1.HEADWAY;
		} else {
			throw new RuntimeException("Optimization parameter " + op1String + " is unknown. Aborting... ");
		}
		
		String op2String = settings.getOptimizationParameter2();

		if(op2String.equals(OptimizationParameter2.FARE.toString())){
			op2 = OptimizationParameter2.FARE;
		} else if(op2String.equals(OptimizationParameter2.HEADWAY.toString())){
			op2 = OptimizationParameter2.HEADWAY;
		} else {
			throw new RuntimeException("Optimization parameter " + op2String + " is unknown. Aborting... ");
		}
		
//		incrBusNumber = settings.getIncrBusNumber();
		incrFare = settings.getIncrFare();
		incrCapacity = settings.getIncrCapacity();
		
		startBusNumber = settings.getStartBusNumber();
		startFare = settings.getStartFare();
		startCapacity = settings.getStartCapacity();

		log.info("Setting run parameters... Done.");
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}

	private void run() throws IOException {
		ChartFileWriter chartWriter = new ChartFileWriter();
		TextFileWriter textWriter = new TextFileWriter();
		
		setDefaultParameters();
		Operator operator = new Operator();
		Users users = new Users();

		int iterationCounter = 0;
		
		for (int extItParam2 = 0; extItParam2 <= lastExternalIterationParam2 ; extItParam2++){
			log.info("************* EXTERNAL ITERATION (2) " + extItParam2 + " BEGINS *************");
			String directoryExtItParam2 = outputExternalIterationDirPath + "/extIt" + extItParam2;
			
			for (int extItParam1 = 0; extItParam1 <= lastExternalIterationParam1 ; extItParam1++){
				log.info("************* EXTERNAL ITERATION (1) " + extItParam1 + " BEGINS *************");

				String directoryExtItParam2Param1 = directoryExtItParam2 + "/extITERS/extIt" + extItParam2 + "." + extItParam1;
				File directory = new File(directoryExtItParam2Param1);
				directory.mkdirs();
				
				Scenario sc = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
				new MatsimNetworkReader(sc).readFile(sc.getConfig().network().getInputFile());
				new MatsimPopulationReader(sc).readFile(sc.getConfig().plans().getInputFile());
					
				VehicleScheduleWriter vsw = new VehicleScheduleWriter(this.numberOfBuses, this.capacity, sc.getNetwork(), directoryExtItParam2Param1);
				vsw.writeTransitVehiclesAndSchedule();
				
				InternalControler internalControler = new InternalControler(sc, directoryExtItParam2Param1, this.fare);
				internalControler.run();
	
				operator.setParametersForExtIteration(this.capacity, this.numberOfBuses);
				users.setParametersForExtIteration(sc);
				
				OperatorUserAnalysis analysis = new OperatorUserAnalysis(sc.getNetwork(), sc.getTransitSchedule(), directoryExtItParam2Param1, sc.getConfig().controler().getLastIteration(), vsw.getHeadway());
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
				info.setId2facilityWaitInfo(analysis.getWaitHandler().getFacilityId2facilityInfos());
				
				info.setAnalysisPeriods(analysis.getPtLoadHandler().getAnalysisPeriods());
				
				info.setAvgT0MinusTActPerPerson(analysis.getCongestionHandler().getAvgTActMinusT0PerPerson());
				info.setT0MinusTActSum(analysis.getCongestionHandler().getTActMinusT0Sum());
				
				this.extIt2information.put(extItParam1, info);
				this.it2information.put(iterationCounter, info);
				
				textWriter.writeExtItData(directoryExtItParam2, this.extIt2information);
				textWriter.writeDataTransitStops(directoryExtItParam2Param1, this.extIt2information, extItParam1);
				textWriter.writeDataEachTransitStop(directoryExtItParam2Param1, this.extIt2information, extItParam1);
				textWriter.writeWaitDataPerPerson(directoryExtItParam2Param1, this.extIt2information, extItParam1);
				textWriter.writeLoadData1(directoryExtItParam2Param1, this.extIt2information, extItParam1);
				textWriter.writeLoadData2(directoryExtItParam2Param1, this.extIt2information, extItParam1);
				chartWriter.write(directoryExtItParam2, this.extIt2information);
								
				// settings for next external iteration (optimization parameter 1)
				if (extItParam1 < lastExternalIterationParam1){
					if(op1.equals(OptimizationParameter1.FARE)) this.fare = this.fare + incrFare;
					if(op1.equals(OptimizationParameter1.CAPACITY)) this.capacity = this.capacity + incrCapacity;
					if(op1.equals(OptimizationParameter1.HEADWAY)) {
						// not using the parameter from the settingsFile
						if (extItParam1 >= 9) {
							this.numberOfBuses = this.numberOfBuses + 2;
						} else {
							this.numberOfBuses = this.numberOfBuses + 1;
						}
					}
				}
				log.info("************* EXTERNAL ITERATION (1) " + extItParam1 + " ENDS *************");
				
				iterationCounter++;
				textWriter.writeExtItData(outputExternalIterationDirPath, this.it2information);
				textWriter.writeMatrices(outputExternalIterationDirPath, this.it2information);
			}
			
			// settings for next external iteration (optimization parameter 2)
			if (extItParam2 < lastExternalIterationParam2){
				if(op2.equals(OptimizationParameter2.FARE)){
					this.fare = this.fare + incrFare;
					this.numberOfBuses = startBusNumber;
				}
				if(op2.equals(OptimizationParameter2.HEADWAY)){
					// not using the parameter from the settingsFile
					if (extItParam2 >= 9) {
						this.numberOfBuses = this.numberOfBuses + 2;
					} else {
						this.numberOfBuses = this.numberOfBuses + 1;
					}
					this.fare = startFare;
				}
			}
			log.info("************* EXTERNAL ITERATION (2) " + extItParam2 + " ENDS *************");
			
			this.extIt2information.clear();
		
		}
	}

//	***************************************************************************************************************************
	
	private void setDefaultParameters() {
		if (op1 != null && op2 == null){
			log.info("Optimization parameter: " + op1 + ". lastExternalIterationParam2 set to 0.");
			
			if (lastExternalIterationParam2 != 0) {
				log.info("Analyzing only one parameter. lastExternalIterationParam2 set to 0.");
				lastExternalIterationParam2 = 0;
			}
			
			log.info("Analyzing optimization parameter " + op1);
			if (op1.equals(OptimizationParameter1.FARE)){
				this.fare = startFare;
				this.capacity = startCapacity;
				this.numberOfBuses = startBusNumber;
			} else if (op1.equals(OptimizationParameter1.CAPACITY)){
				this.fare = startFare;
				this.capacity = startCapacity; // standing room + seats (realistic values between 19 and 101)
				this.numberOfBuses = startBusNumber;
			} else if (op1.equals(OptimizationParameter1.HEADWAY)){
				this.fare = startFare;
				this.capacity = startCapacity;
				this.numberOfBuses = startBusNumber;
			} else {
				throw new RuntimeException("Undefined default parameters for optimization parameter " + op1 + ". Aborting...");
			}
		}
		
		else if (op1 != null && op2 != null) {
			log.info("Analyzing optimization parameters " + op1 + " and " + op2);
			if (op1.equals(OptimizationParameter1.HEADWAY) && op2.equals(OptimizationParameter2.FARE)){
				this.fare = startFare;
				this.capacity = startCapacity;
				this.numberOfBuses = startBusNumber;
			} else if (op1.equals(OptimizationParameter1.FARE) && op2.equals(OptimizationParameter2.HEADWAY)){
				this.fare = startFare;
				this.capacity = startCapacity;
				this.numberOfBuses = startBusNumber;
			} else {
				throw new RuntimeException("Undefined default parameters for combined optimization parameters op1 = " + op1 + " and op2 = " + op2 + ". Aborting...");
			}
			
		} else {
			throw new RuntimeException("Undefined default parameters for combined optimization parameters op1 = " + op1 + " and op2 = " + op2 + ". Aborting...");
		}
	}
}