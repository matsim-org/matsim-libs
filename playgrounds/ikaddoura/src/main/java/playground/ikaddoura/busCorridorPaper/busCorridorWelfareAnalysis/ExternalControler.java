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
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input/network80links.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input/populationBusCorridor80linksCar.xml";
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output/PaperTEST";
	
	static int lastExternalIteration = 0;
	static int lastInternalIteration = 0;
	
//	final OptimizationParameter op = OptimizationParameter.FARE;
//	final OptimizationParameter op = OptimizationParameter.CAPACITY;
	final OptimizationParameter op = OptimizationParameter.NUMBER_OF_BUSES;
	
	double fare;
	int capacity;
	int numberOfBuses;
	
	SortedMap<Integer, ExtItInformation> extIt2information = new TreeMap<Integer, ExtItInformation>();
	
	private void run() throws IOException {
		ChartFileWriter chartWriter = new ChartFileWriter();
		TextFileWriter textWriter = new TextFileWriter();
		
		setDefaultParameters();
		Operator operator = new Operator();
		Users users = new Users();

		for (int extIt = 0; extIt <= lastExternalIteration ; extIt++){
			log.info("************* EXTERNAL ITERATION " + extIt + " BEGINS *************");
			
			String directoryExtIt = outputExternalIterationDirPath + "/extITERS/extIt." + extIt;
			File directory = new File(directoryExtIt);
			directory.mkdirs();

			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
			new MatsimNetworkReader(sc).readFile(networkFile);
			new MatsimPopulationReader(sc).readFile(populationFile);
			
			VehicleScheduleWriter vsw = new VehicleScheduleWriter(this.numberOfBuses, this.capacity, sc.getNetwork(), directoryExtIt);
			vsw.writeTransitVehiclesAndSchedule();
			
			InternalControler internalControler = new InternalControler(sc, directoryExtIt, lastInternalIteration, this.fare);

			operator.setParametersForExtIteration(this.capacity, this.numberOfBuses);
			users.setParametersForExtIteration(directoryExtIt, sc.getNetwork(), internalControler.getMarginalUtlOfMoney());

			internalControler.run();
			
			OperatorUserAnalysis analysis = new OperatorUserAnalysis(directoryExtIt, lastInternalIteration, networkFile);
			analysis.readEvents(operator, users);
			
			users.calculateScore();
			operator.calculateScore(analysis);

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
			
//			textWriter.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2fare, this.iteration2capacity, this.iteration2operatorCosts, this.iteration2operatorRevenue, this.iteration2operatorProfit, this.iteration2userScore, this.iteration2userScoreSum, this.iteration2totalScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);
//			textWriter.writeWaitingTimes(outputExternalIterationDirPath, extIt, this.iteration2personId2waitTime.get(extIt));
//			
//			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2numberOfBuses, "Number of buses per iteration", "NumberOfBuses");
//			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2capacity, "Vehicle capacity per iteration", "Capacity");
//			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2fare, "Bus fare per iteration", "Fare");
//
//			chartWriter.writeChart_LegModes(outputExternalIterationDirPath, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
//			chartWriter.writeChart_UserScores(outputExternalIterationDirPath, this.iteration2userScore);
//			chartWriter.writeChart_UserScoresSum(outputExternalIterationDirPath, this.iteration2userScoreSum);
//			chartWriter.writeChart_TotalScore(outputExternalIterationDirPath, this.iteration2totalScore);
//			chartWriter.writeChart_OperatorScores(outputExternalIterationDirPath, this.iteration2operatorProfit, this.iteration2operatorCosts, this.iteration2operatorRevenue);
			
			// settings for next external iteration
			if (extIt < lastExternalIteration){
				if(op.equals(OptimizationParameter.FARE)) this.fare++;
				if(op.equals(OptimizationParameter.CAPACITY)) this.capacity = this.capacity + 10;
				if(op.equals(OptimizationParameter.NUMBER_OF_BUSES)) this.numberOfBuses++;
			}
			log.info("************* EXTERNAL ITERATION " + extIt + " ENDS *************");
		}
	}

	private void setDefaultParameters() {
		if(op.equals(OptimizationParameter.FARE)){
			this.fare = 0.;
			this.capacity = 50;
			this.numberOfBuses = 5;
		} else if (op.equals(OptimizationParameter.CAPACITY)){
			this.fare = 2.;
			this.capacity = 20; // standing room + seats (realistic values between 19 and 101!)
			this.numberOfBuses = 5;
		} else if(op.equals(OptimizationParameter.NUMBER_OF_BUSES)){
			this.fare = 2.;
			this.capacity = 50;
			this.numberOfBuses = 1;
		}
	}

	public static void main(final String[] args) throws IOException {
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}
}