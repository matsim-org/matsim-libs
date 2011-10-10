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
package playground.ikaddoura.busCorridor.version4;

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
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/network_busline.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/population.xml"; // for first iteration
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output_version4";
	static int numberOfExternalIterations = 30;
	static int lastInternalIteration = 20 ; // for ChangeTransitLegMode: ModuleDisableAfterIteration = 17
	
	private int numberOfBuses = 1; // for first iteration!
	private int extItNr;
	private String directoryExtIt;
	
	private Map<Integer, Double> iteration2operatorScore = new HashMap<Integer, Double>();
	private Map<Integer, Integer> iteration2numberOfBuses = new HashMap<Integer, Integer>();
	private Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>();
	private Map<Integer, Integer> iteration2numberOfCarLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfPtLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfWalkLegs = new HashMap<Integer, Integer>();

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
			
			VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.getNumberOfBuses(), networkFile, this.getDirectoryExtIt());
			transitWriter.writeTransit();
			
			InternalControler internalControler = new InternalControler(configFile, this.extItNr, this.getDirectoryExtIt(), lastInternalIteration, populationFile, outputExternalIterationDirPath, this.getNumberOfBuses());
			internalControler.run();

			Operator operator = new Operator(this.getExtItNr(), this.getNumberOfBuses());
			operator.calculateScore(this.getDirectoryExtIt(), lastInternalIteration, networkFile);
			operator.analyzeScores();
			
			Users users = new Users();
			users.analyzeScores(this.getDirectoryExtIt(), networkFile);
			users.analyzeLegModes(this.getDirectoryExtIt(), lastInternalIteration);

			this.iteration2operatorScore.put(this.getExtItNr(), operator.getScore());
			this.iteration2numberOfBuses.put(this.getExtItNr(), this.getNumberOfBuses());
			this.iteration2userScore.put(this.getExtItNr(), users.getAvgExecScore());
			this.iteration2numberOfCarLegs.put(this.getExtItNr(), users.getNumberOfCarLegs());
			this.iteration2numberOfPtLegs.put(this.getExtItNr(), users.getNumberOfPtLegs());
			this.iteration2numberOfWalkLegs.put(this.getExtItNr(), users.getNumberOfWalkLegs());
			
//			this.setNumberOfBuses(operator.strategy(this.iteration2numberOfBuses, this.iteration2operatorScore)); // for next iteration!
			this.setNumberOfBuses(operator.increaseNumberOfBuses()); // für die nächste externe Iteration!

			log.info("************* EXTERNAL ITERATION "+extIt+" ENDS *************");
		}

		TextFileWriter stats = new TextFileWriter();
		stats.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2operatorScore, this.iteration2userScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);
	
		ChartFileWriter chartWriter = new ChartFileWriter();
		chartWriter.writeLineChartFile(outputExternalIterationDirPath , this.iteration2numberOfBuses, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
		chartWriter.writeLineChartScores(outputExternalIterationDirPath, "User", iteration2numberOfBuses, iteration2userScore);
		chartWriter.writeLineChartScores(outputExternalIterationDirPath, "Operator", iteration2numberOfBuses, iteration2operatorScore);
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
	
	
}
