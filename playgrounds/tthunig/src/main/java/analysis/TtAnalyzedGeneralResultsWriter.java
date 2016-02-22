/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * Class to write all calculated results of the analyze tool that is given in the constructor.
 * 
 * @author tthunig
 *
 */
class TtAnalyzedGeneralResultsWriter {

	private static final Logger log = Logger.getLogger(TtAnalyzedGeneralResultsWriter.class);
	
	private TtGeneralAnalysis handler;
	private String outputDirBase;
	private PrintStream overallItWritingStream;
	private int lastIteration;
	
	private enum RelCumFreqType {
		DepPerTime, ArrPerTime, TripsPerDuration, TripsPerDist, TripsPerAvgSpeed
	}
	
	public TtAnalyzedGeneralResultsWriter(TtGeneralAnalysis handler, String outputDirBase, int lastIteration) {
		this.handler = handler;
		this.outputDirBase = outputDirBase;
		this.lastIteration = lastIteration;
		
		// prepare file for the results of all iterations
		prepareOverallItWriting();
	}

	private void prepareOverallItWriting() {
		// create output dir for overall iteration analysis
		String lastItDir = this.outputDirBase + "ITERS/it." + this.lastIteration + "/";
		new File(lastItDir).mkdir();
		String lastItOutputDir = lastItDir + "analysis/";
		new File(lastItOutputDir).mkdir();

		// create writing stream
		try {
			this.overallItWritingStream = new PrintStream(new File(lastItOutputDir + "overallIterationAnalysis.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// write header
		String header = "it\ttotal tt[s]\ttotal delay[s]\ttotal dist[m]\tavg trip speed[m/s]";
		this.overallItWritingStream.println(header);
	}

	public void writeIterationResults(int iteration) {
		log.info("Starting to write analysis of iteration " + iteration + "...");
		addLineToOverallItResults(iteration);
		writeItOnlyResults(iteration);
	}

	private void addLineToOverallItResults(int iteration) {
		// get results
		double totalTTIt = handler.getTotalTt();
		double totalDealyIt = handler.getTotalDelay();
		double totalDistIt = handler.getTotalDistance();
		double avgTripSpeedIt = handler.getAverageTripSpeed();
		
		// write results
		StringBuffer line = new StringBuffer();
		line.append(iteration + "\t" + totalTTIt + "\t" + totalDealyIt + "\t" 
				+ totalDistIt + "\t" + avgTripSpeedIt);
		this.overallItWritingStream.println(line.toString());
	}

	private void writeItOnlyResults(int iteration) {
		// create output dir for this iteration analysis
		String outputDir = this.outputDirBase + "ITERS/it." + iteration + "/analysis/";
		new File(outputDir).mkdir();

		// write iteration specific analysis
		log.info("Results of iteration " + iteration + ":");
		writeRelCumFreqData(outputDir, RelCumFreqType.DepPerTime, handler.getRelativeCumulativeFrequencyOfDeparturesPerTimeInterval());
		writeRelCumFreqData(outputDir, RelCumFreqType.ArrPerTime, handler.getRelativeCumulativeFrequencyOfArrivalsPerTimeInterval());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerDuration, handler.getRelativeCumulativeFrequencyOfTripsPerDuration());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerDist, handler.getRelativeCumulativeFrequencyOfTripsPerDistance());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerAvgSpeed, handler.getRelativeCumulativeFrequencyOfTripsPerSpeed());
		// TODO spatial analysis
	}

	private void writeRelCumFreqData(String outputDir, RelCumFreqType type, Map<Double, Double> relCumFreqMap) {
		PrintStream stream;
		String filename = outputDir + type + ".txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "";
		switch (type){
		case DepPerTime:
			header = "time of day\trelative cumulative frequency of departures";
			break;
		case ArrPerTime:
			header = "time of day\trelative cumulative frequency of arrivals";
			break;
		case TripsPerDuration:
			header = "trip duration\trelative cumulative frequency of trips";
			break;
		case TripsPerDist:
			header = "trip distance\trelative cumulative frequency of trips";
			break;
		case TripsPerAvgSpeed:
			header = "avg trip speed\trelative cumulative frequency of trips";
			break;
		}
		stream.println(header);
		
		for (Entry<Double, Double> entry : relCumFreqMap.entrySet()) {
			StringBuffer line = new StringBuffer();
			line.append(entry.getKey() + "\t" + entry.getValue());
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}

	public void closeAllStreams() {
		this.overallItWritingStream.close();
	}
	
}
