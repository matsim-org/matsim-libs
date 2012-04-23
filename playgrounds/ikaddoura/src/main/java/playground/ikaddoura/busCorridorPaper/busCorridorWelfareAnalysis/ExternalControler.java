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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author benjamin after Ihab
 *
 */

class ExternalControler {
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input/network80links.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input/output_plans_withTimeChoice_9min.xml";
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output";
	
	static int lastExternalIteration = 20;
	static int lastInternalIteration = 300;
	
	final OptimizationParameter op = OptimizationParameter.FARE;
//	final OptimizationParameter op = OptimizationParameter.CAPACITY;
//	final OptimizationParameter op = OptimizationParameter.NUMBER_OF_BUSES;
	
	double fare;
	int capacity; // standing room + seats (realistic values between 19 and 101!)
	int numberOfBuses;
	
	SortedMap<Integer, Double> iteration2operatorProfit = new TreeMap<Integer, Double>();
	SortedMap<Integer, Double> iteration2operatorCosts = new TreeMap<Integer, Double>();
	SortedMap<Integer, Double> iteration2operatorRevenue = new TreeMap<Integer, Double>();
	SortedMap<Integer, Double> iteration2numberOfBuses = new TreeMap<Integer, Double>();
	SortedMap<Integer, Double> iteration2userScore = new TreeMap<Integer,Double>();
	SortedMap<Integer, Double> iteration2userScoreSum = new TreeMap<Integer,Double>();
	SortedMap<Integer, Double> iteration2totalScore = new TreeMap<Integer,Double>();
	SortedMap<Integer, Integer> iteration2numberOfCarLegs = new TreeMap<Integer, Integer>();
	SortedMap<Integer, Integer> iteration2numberOfPtLegs = new TreeMap<Integer, Integer>();
	SortedMap<Integer, Integer> iteration2numberOfWalkLegs = new TreeMap<Integer, Integer>();
	SortedMap<Integer, Double> iteration2fare = new TreeMap<Integer, Double>();
	SortedMap<Integer, Double> iteration2capacity = new TreeMap<Integer, Double>();
	SortedMap<Integer, Map<Id, Double>> iteration2personId2waitTime = new TreeMap<Integer, Map<Id, Double>>();

	
	private void run() throws IOException {
		PtLegHandler ptLegHandler = new PtLegHandler();
		ChartFileWriter chartWriter = new ChartFileWriter();
		TextFileWriter textWriter = new TextFileWriter();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(networkFile);
		
		setDefaultParameters();
		Operator operator = new Operator();
		Users users = new Users();

		for (int extIt = 0; extIt <= lastExternalIteration ; extIt++){
			log.info("************* EXTERNAL ITERATION " + extIt + " BEGINS *************");
			String directoryExtIt = outputExternalIterationDirPath + "/extITERS/extIt." + extIt;
			File directory = new File(directoryExtIt);
			directory.mkdirs();
			

			// TODO: VehicleScheduleGenerator aufräumen!
			VehicleScheduleWriter vsw = new VehicleScheduleWriter(this.capacity, networkFile, directoryExtIt);
			vsw.writeTransitVehiclesAndSchedule();
			
			// TODO: anschauen!
			InternalControler internalControler = new InternalControler(configFile, extIt, directoryExtIt, lastInternalIteration, populationFile, outputExternalIterationDirPath, this.numberOfBuses, networkFile, fare, ptLegHandler);

			operator.setParametersForExtIteration(this.capacity, this.numberOfBuses);
			users.setParametersForExtIteration(directoryExtIt, sc.getNetwork(), internalControler.getMarginalUtlOfMoney());

			internalControler.run();
			
			OperatorUserAnalysis analysis = new OperatorUserAnalysis(directoryExtIt, lastInternalIteration, networkFile);
			analysis.readEvents(operator, users);
			
			users.calculateScore();
			operator.calculateScore(analysis);

			this.iteration2operatorProfit.put(extIt, operator.getProfit());
			this.iteration2operatorCosts.put(extIt, operator.getCosts());
			this.iteration2operatorRevenue.put(extIt, analysis.getRevenue());
			this.iteration2numberOfBuses.put(extIt, (double) analysis.getNumberOfBusesFromEvents());
			this.iteration2userScoreSum.put(extIt, users.getLogSum());
			this.iteration2totalScore.put(extIt, (users.getLogSum() + operator.getProfit()));
			this.iteration2numberOfCarLegs.put(extIt, analysis.getSumOfCarLegs());
			this.iteration2numberOfPtLegs.put(extIt, analysis.getSumOfPtLegs());
			this.iteration2numberOfWalkLegs.put(extIt, analysis.getSumOfWalkLegs());
			this.iteration2fare.put(extIt, this.fare);
			this.iteration2capacity.put(extIt,(double) this.capacity);
			this.iteration2personId2waitTime.put(extIt, ptLegHandler.getPersonId2WaitingTime());
			
//			textWriter.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2fare, this.iteration2capacity, this.iteration2operatorCosts, this.iteration2operatorRevenue, this.iteration2operatorProfit, this.iteration2userScore, this.iteration2userScoreSum, this.iteration2totalScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);
			textWriter.writeWaitingTimes(outputExternalIterationDirPath, extIt, this.iteration2personId2waitTime.get(extIt));
			
			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2numberOfBuses, "Number of buses per iteration", "NumberOfBuses");
			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2capacity, "Vehicle capacity per iteration", "Capacity");
			chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2fare, "Bus fare per iteration", "Fare");

			chartWriter.writeChart_LegModes(outputExternalIterationDirPath, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
			chartWriter.writeChart_UserScores(outputExternalIterationDirPath, this.iteration2userScore);
			chartWriter.writeChart_UserScoresSum(outputExternalIterationDirPath, this.iteration2userScoreSum);
			chartWriter.writeChart_TotalScore(outputExternalIterationDirPath, this.iteration2totalScore);
			chartWriter.writeChart_OperatorScores(outputExternalIterationDirPath, this.iteration2operatorProfit, this.iteration2operatorCosts, this.iteration2operatorRevenue);
			
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
			this.capacity = 20;
			this.numberOfBuses = 5;
		} else if(op.equals(OptimizationParameter.NUMBER_OF_BUSES)){
			this.fare = 2.;
			this.capacity = 20;
			this.numberOfBuses = 1;
		}
	}

	public static void main(final String[] args) throws IOException {
		ExternalControler externalControler = new ExternalControler();
		externalControler.run();
	}
}