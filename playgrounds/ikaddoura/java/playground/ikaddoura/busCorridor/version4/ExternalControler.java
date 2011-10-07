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

/**
 * @author Ihab
 *
 */

public class ExternalControler {
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/network_busline.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_version4/population.xml"; // Startwert
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output_version4/";
	static int numberOfExternalIterations = 10;
	static int lastInternalIteration = 0;
	
	private int numberOfBuses = 7; // Startwert!
	private int extItNr;
	private String directoryExtIt;
	
	private Map<Integer, Double> iteration2providerScore = new HashMap<Integer, Double>();
	private Map<Integer, Integer> iteration2numberOfBuses = new HashMap<Integer, Integer>();
	private Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>();

	public static void main(final String[] args) throws IOException {
		ExternalControler simulation = new ExternalControler();
		simulation.externalIteration();
	}
	
	private void externalIteration() throws IOException {
		
		for (int extIt = 0; extIt <= numberOfExternalIterations ; extIt++){
			this.setExtItNr(extIt);
			this.setDirectoryExtIt(outputExternalIterationDirPath +"/extITERS/extIt."+extIt);
			File directory = new File(this.getDirectoryExtIt());
			directory.mkdirs();
			
			VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.getNumberOfBuses(), networkFile, this.getDirectoryExtIt());
			transitWriter.writeTransit();
			
			InternalControler internalControler = new InternalControler(configFile, this.extItNr, this.getDirectoryExtIt(), lastInternalIteration, populationFile, outputExternalIterationDirPath, this.getNumberOfBuses());
			internalControler.run();

			Provider provider = new Provider(this.getExtItNr(), this.getNumberOfBuses());
			provider.calculateScore(this.getDirectoryExtIt(), lastInternalIteration, networkFile);
			provider.analyzeScores();
			
			Users users = new Users();
			users.analyzeScores(this.getDirectoryExtIt());

			this.iteration2providerScore.put(this.getExtItNr(), provider.getScore());
			this.iteration2numberOfBuses.put(this.getExtItNr(), this.getNumberOfBuses());
			this.iteration2userScore.put(this.getExtItNr(), users.getAvgExecScore());
			
			this.setNumberOfBuses(provider.strategy(this.iteration2numberOfBuses, this.iteration2providerScore)); // für die nächste externe Iteration!
		}

		TextFileWriter stats = new TextFileWriter();
		stats.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2providerScore, this.iteration2userScore);
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
