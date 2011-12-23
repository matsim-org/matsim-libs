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
package playground.ikaddoura.busCorridor.version7;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */

public class ExternalControler {
	
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_final/network.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input_final/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_final/population.xml"; // for first iteration only
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output_final_relaxation";
	static int numberOfExternalIterations = 0;
	static int lastInternalIteration = 100; // for ChangeTransitLegMode: ModuleDisableAfterIteration = 28
	
	// settings for first iteration or if values not increased for all iterations
	private int numberOfBuses = 1; // at least one bus!
	private double fare = -2.5; // negative!
	private int capacity = 50; // standing room + seats (realistic values between 19 and 101!)

	private int extItNr;
	private String directoryExtIt;
	
	private Map<Integer, Double> iteration2operatorProfit = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorCosts = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorEarnings = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2numberOfBuses = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>();
	private Map<Integer, Integer> iteration2numberOfCarLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfPtLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfWalkLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Double> iteration2fare = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2capacity = new HashMap<Integer, Double>();

	public static void main(final String[] args) throws IOException {
		ExternalControler simulation = new ExternalControler();
		simulation.externalIteration();
	}
	
	private void externalIteration() throws IOException {
		
		for (int extIt = 0; extIt <= numberOfExternalIterations ; extIt++){
			log.info("************* EXTERNAL ITERATION "+extIt+" BEGINS *************");
			this.setExtItNr(extIt);
			this.setDirectoryExtIt(outputExternalIterationDirPath +"/extITERS/extIt."+extIt);
			File directory = new File(this.getDirectoryExtIt());
			directory.mkdirs();
			
			VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.getNumberOfBuses(), this.getCapacity(), networkFile, this.getDirectoryExtIt());
			transitWriter.writeTransit();
			
			InternalControler internalControler = new InternalControler(configFile, this.extItNr, this.getDirectoryExtIt(), lastInternalIteration, populationFile, outputExternalIterationDirPath, this.getNumberOfBuses(), networkFile, fare);
			internalControler.run();

			Operator operator = new Operator(this.getExtItNr(), this.getNumberOfBuses(), this.getCapacity());
			operator.calculateScore(this.getDirectoryExtIt(), lastInternalIteration, networkFile);
//			operator.analyzeScores();
			
			Users users = new Users();
			users.analyzeScores(this.getDirectoryExtIt(), networkFile);
			users.analyzeLegModes(this.getDirectoryExtIt(), lastInternalIteration);

			this.iteration2operatorProfit.put(this.getExtItNr(), operator.getProfit());
			this.iteration2operatorCosts.put(this.getExtItNr(), operator.getCosts());
			this.iteration2operatorEarnings.put(this.getExtItNr(), operator.getEarnings());
			this.iteration2numberOfBuses.put(this.getExtItNr(), (double) this.getNumberOfBuses());
			this.iteration2userScore.put(this.getExtItNr(), users.getAvgExecScore());
			this.iteration2numberOfCarLegs.put(this.getExtItNr(), users.getNumberOfCarLegs());
			this.iteration2numberOfPtLegs.put(this.getExtItNr(), users.getNumberOfPtLegs());
			this.iteration2numberOfWalkLegs.put(this.getExtItNr(), users.getNumberOfWalkLegs());
			this.iteration2fare.put(this.getExtItNr(), this.getFare());
			this.iteration2capacity.put(this.getExtItNr(),(double) this.getCapacity());
			
			// settings for next external iteration
//			this.setNumberOfBuses(operator.strategy(this.iteration2numberOfBuses, this.iteration2operatorScore));
//			this.setNumberOfBuses(operator.increaseNumberOfBuses(1)); // absolute value
//			this.setFare(operator.increaseFare(this.getFare(), -0.5)); // absolute value
			this.setCapacity(operator.increaseCapacity(2)); // absolute value
			

			log.info("************* EXTERNAL ITERATION "+extIt+" ENDS *************");
		}

		TextFileWriter stats = new TextFileWriter();
		stats.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2fare, this.iteration2capacity, this.iteration2operatorCosts, this.iteration2operatorEarnings, this.iteration2operatorProfit, this.iteration2userScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);

		ChartFileWriter chartWriter = new ChartFileWriter();
		
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2numberOfBuses, "Number of buses per iteration", "NumberOfBuses");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2capacity, "Vehicle capacity per iteration", "Capacity");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2fare, "Bus fare per iteration", "Fare");

		chartWriter.writeChart_LegModes(outputExternalIterationDirPath, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
		chartWriter.writeChart_UserScores(outputExternalIterationDirPath, this.iteration2userScore);
		chartWriter.writeChart_OperatorScores(outputExternalIterationDirPath, this.iteration2operatorProfit, this.iteration2operatorCosts, this.iteration2operatorEarnings);

	}

	/**
	 * @return the numberOfBuses
	 */
	public int getNumberOfBuses() {
		return numberOfBuses;
	}

	/**
	 * @param numberOfBuses the numberOfBuses to set
	 */
	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	/**
	 * @return the extItNr
	 */
	public int getExtItNr() {
		return extItNr;
	}

	/**
	 * @param extItNr the extItNr to set
	 */
	public void setExtItNr(int extItNr) {
		this.extItNr = extItNr;
	}

	/**
	 * @return the directoryExtIt
	 */
	public String getDirectoryExtIt() {
		return directoryExtIt;
	}

	/**
	 * @param directoryExtIt the directoryExtIt to set
	 */
	public void setDirectoryExtIt(String directoryExtIt) {
		this.directoryExtIt = directoryExtIt;
	}

	/**
	 * @return the fare
	 */
	public double getFare() {
		return fare;
	}

	/**
	 * @param fare the fare to set
	 */
	public void setFare(double fare) {
		this.fare = fare;
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	
}
