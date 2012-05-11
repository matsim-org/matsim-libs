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
 * @author benjamin after Ihab
 *
 */

class ExternalControler {
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String configFile;
	static String outputExternalIterationDirPath;
	static int lastExternalIteration;
	static OptimizationParameter op;
	
	SortedMap<Integer, ExtItInformation> extIt2information = new TreeMap<Integer, ExtItInformation>();
	double fare;
	int capacity;
	int numberOfBuses;
	
	public static void main(final String[] args) throws IOException {
//		configFile = "../../shared-svn/studies/ihab/busCorridor/input/config_welfareBusCorridor_noTimeChoice.xml";
//		outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output/paper_fare_withoutTimeChoice";
//		lastExternalIteration = 1;
//		
////		op = OptimizationParameter.FARE;
////		op = OptimizationParameter.CAPACITY;
//		op = OptimizationParameter.NUMBER_OF_BUSES;
		
		configFile = args[0];
		outputExternalIterationDirPath = args[1];
		lastExternalIteration = Integer.parseInt(args[2]);
		String opString = args[3];
		
		if(opString.equals(OptimizationParameter.FARE.toString())){
			op = OptimizationParameter.FARE;
		} else if(opString.equals(OptimizationParameter.CAPACITY.toString())){
			op = OptimizationParameter.CAPACITY;
		} else if(opString.equals(OptimizationParameter.NUMBER_OF_BUSES.toString())){
			op = OptimizationParameter.NUMBER_OF_BUSES;
		} else {
			throw new RuntimeException("Optimization parameter " + opString + " is unknown. Aborting... ");
		}
		
		log.info("Analyzing optimization parameter " + op);
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}

	private void run() throws IOException {
		ChartFileWriter chartWriter = new ChartFileWriter(outputExternalIterationDirPath);
		TextFileWriter textWriter = new TextFileWriter(outputExternalIterationDirPath);
		
		setDefaultParameters();
		Operator operator = new Operator();
		Users users = new Users();

		for (int extIt = 0; extIt <= lastExternalIteration ; extIt++){
			log.info("************* EXTERNAL ITERATION " + extIt + " BEGINS *************");
			
			String directoryExtIt = outputExternalIterationDirPath + "/extITERS/extIt." + extIt;
			File directory = new File(directoryExtIt);
			directory.mkdirs();

			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
			new MatsimNetworkReader(sc).readFile(sc.getConfig().network().getInputFile());
			new MatsimPopulationReader(sc).readFile(sc.getConfig().plans().getInputFile());
			
			log.info(sc.getNetwork().getLinks().toString());
			
			VehicleScheduleWriter vsw = new VehicleScheduleWriter(this.numberOfBuses, this.capacity, sc.getNetwork(), directoryExtIt);
			vsw.writeTransitVehiclesAndSchedule();
			
			InternalControler internalControler = new InternalControler(sc, directoryExtIt, this.fare);
			internalControler.run();

			operator.setParametersForExtIteration(this.capacity, this.numberOfBuses);
			users.setParametersForExtIteration(sc);
			
			OperatorUserAnalysis analysis = new OperatorUserAnalysis(sc.getNetwork(), directoryExtIt, sc.getConfig().controler().getLastIteration());
			analysis.readEvents();
			
			operator.calculateCosts(analysis);
			users.calculateLogsum();

			ExtItInformation info = new ExtItInformation();
			info.setFare(this.fare);
			info.setCapacity(this.capacity);
			info.setNumberOfBuses(this.numberOfBuses);
			info.setOperatorCosts(operator.getCosts());
			info.setOperatorRevenue(analysis.getRevenue());
			info.setUsersLogSum(users.getLogSum());
			info.setNumberOfCarLegs(analysis.getSumOfCarLegs());
			info.setNumberOfPtLegs(analysis.getSumOfPtLegs());
			info.setNumberOfWalkLegs(analysis.getSumOfWalkLegs());
			info.setSumOfWaitingTimes(internalControler.getSumOfWaitingTimes());
			
			extIt2information.put(extIt, info);
			
			textWriter.write(this.extIt2information);
			chartWriter.write(this.extIt2information);
			
			// settings for next external iteration
			// TODO: adjust the steps
			if (extIt < lastExternalIteration){
				if(op.equals(OptimizationParameter.FARE)) this.fare = this.fare - 1.0;
				if(op.equals(OptimizationParameter.CAPACITY)) this.capacity = this.capacity + 4;
				if(op.equals(OptimizationParameter.NUMBER_OF_BUSES)) this.numberOfBuses = this.numberOfBuses + 1;
			}
			log.info("************* EXTERNAL ITERATION " + extIt + " ENDS *************");
		}
	}

	private void setDefaultParameters() {
		if(op.equals(OptimizationParameter.FARE)){
			this.fare = -0.;
			this.capacity = 50;
			this.numberOfBuses = 5;
		} else if (op.equals(OptimizationParameter.CAPACITY)){
			this.fare = -2.;
			this.capacity = 20; // standing room + seats (realistic values between 19 and 101!)
			this.numberOfBuses = 5;
		} else if(op.equals(OptimizationParameter.NUMBER_OF_BUSES)){
			this.fare = -2.;
			this.capacity = 50;
			this.numberOfBuses = 1;
		}
	}
}