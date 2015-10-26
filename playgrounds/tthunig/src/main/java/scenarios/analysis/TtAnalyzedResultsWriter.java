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
package scenarios.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Class to write all calculated results of the analyze tool that is given in the constructor.
 * It works for all handlers that extend the abstract analyze tool TtAbstractAnalyzeTool.
 *
 * @author tthunig
 */
public class TtAnalyzedResultsWriter {

	private static final Logger log = Logger.getLogger(TtAnalyzedResultsWriter.class);
	
	private TtAbstractAnalysisTool handler;
	private String outputDir;
	private PrintStream overallItWritingStream;
	private int numberOfRoutes;
	
	public TtAnalyzedResultsWriter(TtAbstractAnalysisTool handler, String outputDir) {
		this.handler = handler;
		this.outputDir = outputDir;
		
		this.numberOfRoutes = handler.getNumberOfRoutes();
		
		// prepare file for the results of all iterations
		prepareWriting();
	}

	private void prepareWriting() {
		try {
			this.overallItWritingStream = new PrintStream(
					new File(this.outputDir + "routesAndTTs.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		String header = "it\ttotal tt[s]";
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			header += "\t#users " + routeNr;
		}
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			header += "\tavg tt[s] " + routeNr;
		}
		header += "\t#stucked";
		
		this.overallItWritingStream.println(header);
	}

	public void addSingleItToResults(int iteration){
		
		log.info("Starting to analyze iteration " + iteration);

		// get results
		double totalTTIt = handler.getTotalTT();
		double[] avgRouteTTsIt = handler.calculateAvgRouteTTs();
		int[] routeUsersIt = handler.getRouteUsers();
		int numberOfStuckedAgents = handler.getNumberOfStuckedAgents();
		
		// write results
		StringBuffer line = new StringBuffer();
		line.append(iteration + "\t" + totalTTIt);
		for (int routeNr = 0; routeNr < numberOfRoutes; routeNr++) {
			line.append("\t" + routeUsersIt[routeNr]);
		}
		for (int routeNr = 0; routeNr < numberOfRoutes; routeNr++) {
			line.append("\t" + avgRouteTTsIt[routeNr]);
		}
		line.append("\t" + numberOfStuckedAgents);
		this.overallItWritingStream.println(line.toString());
	}

	public void writeFinalResults() {
		// close stream
		this.overallItWritingStream.close();
		
		// write last iteration specific analysis
		log.info("Final analysis:");		
		writeLastIterationResults(handler.getTotalTT(), handler.getTotalRouteTTs(), 
				handler.calculateAvgRouteTTs(), handler.getRouteUsers(), 
				handler.getNumberOfStuckedAgents());
		writeOnRoutes(handler.getOnRoutePerSecond());
		writeRouteStarts(handler.getRouteDeparturesPerSecond());
		writeAvgRouteTTs("Departure", handler.calculateAvgRouteTTsByDepartureTime());
	}
	
	/**
	 * Create result file from the last iteration (FinalResults.txt) and write some
	 * results to the console.
	 */
	private void writeLastIterationResults(double totalTT, double[] totalRouteTTs,
			double[] avgRouteTTs, int[] routeUsers, int numberOfStuckedAgents) {

		PrintStream stream;
		String filename = outputDir + "FinalResults.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
	
		log.info("Total travel time: " + totalTT);
		String header = "total tt[s]";
		StringBuffer resultLine = new StringBuffer();
		resultLine.append(totalTT);
		String latexFormat = "" + (int)totalTT;
		
		log.info("Route Users: (route: #users)");
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			log.info("\t" + routeNr + ": " + routeUsers[routeNr]);
			header += "\t#users " + routeNr;
			resultLine.append("\t" + routeUsers[routeNr]);
			latexFormat += " & " + routeUsers[routeNr];
		}
		
		log.info("Average travel times: (route: avg tt)");
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			log.info("\t" + routeNr + ": " + avgRouteTTs[routeNr]);
			header += "\tavg tt[s] " + routeNr;
			resultLine.append("\t" + avgRouteTTs[routeNr]);
			latexFormat += " & " + (Double.isNaN(avgRouteTTs[routeNr]) ? "-" : (int)avgRouteTTs[routeNr]);
		}
		
		log.info("Number of stucked agents: " + numberOfStuckedAgents);
		header += "\t#stucked";
		resultLine.append("\t" + numberOfStuckedAgents);
		
		latexFormat += " \\\\";
		log.info("Latex format: " + latexFormat);
				
		stream.println(header);
		stream.println(resultLine.toString());
		stream.close();
		
		log.info("output written to " + filename);
	}

	private void writeRouteStarts(Map<Double, double[]> routeStartsMap) {
		PrintStream stream;
		String filename = outputDir + "startsPerRoute.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time";
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			header += "\t#starts " + routeNr;
		}
		header += "\t#starts total";
		stream.println(header);
		for (Double time : routeStartsMap.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] routeStarts = routeStartsMap.get(time);
			double totalStarts = 0.0;
			
			line.append(time);
			for (int routeNr = 0; routeNr < numberOfRoutes; routeNr++) {
				line.append("\t" + routeStarts[routeNr]);
				totalStarts += routeStarts[routeNr];
			}
			line.append("\t" + totalStarts);
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}

	private void writeOnRoutes(Map<Double, double[]> onRoutesMap) {
		PrintStream stream;
		String filename = this.outputDir + "onRoutes.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time";
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			header += "\t#users " + routeNr;
		}
		header += "\t#users total";
		stream.println(header);
		for (Double time : onRoutesMap.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] onRoutes = onRoutesMap.get(time);
			double totalOnRoute = 0.0;
			
			line.append(time);
			for (int routeNr = 0; routeNr < numberOfRoutes; routeNr++) {
				line.append("\t" + onRoutes[routeNr]);
				totalOnRoute += onRoutes[routeNr];
			}
			line.append("\t" + totalOnRoute);
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}

	private void writeAvgRouteTTs(String eventType, Map<Double, double[]> avgTTs) {
		PrintStream stream;
		String filename = outputDir + "avgRouteTTsPer" + eventType + ".txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = eventType + "Time";
		for (int routeNr=0; routeNr < numberOfRoutes; routeNr++){
			header += "\t#avg tt " + routeNr;
		}
		stream.println(header);
		for (Double eventTime : avgTTs.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] avgRouteTTs = avgTTs.get(eventTime);
			
			line.append(eventTime);
			for (int routeNr = 0; routeNr < numberOfRoutes; routeNr++) {
				line.append("\t" + avgRouteTTs[routeNr]);
			}
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}
	
}
