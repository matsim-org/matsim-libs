/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.spatialAvg;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingWriter;


/**
 * @author benjamin, julia
 *
 */
public class SpatialAveragingDemandEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDemandEmissions.class);

	private String baseCase = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String compareCase = "exposurePricing"; // zone30, pricing, exposurePricing, 983
	
	final int noOfXbins = 160;
	final int noOfYbins = 120; 
	
	final int noOfTimeBins = 60;
	
	final double smoothingRadius_m = 500.;
	
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBaseCase = true;
	final boolean useLineMethod = true;
	private boolean writeRoutput = true;
	private boolean writeGisOutput = false;
	
	private SpatialAveragingWriter saWriter;
	private double simulationEndTime;
	private Network network;
	private String outPathStub;
	private EmissionsPerLinkAndTimeIntervalEventHandler emissionHandler;
	private LinkWeightUtil linkWeightUtil;
	private SpatialGrid[] timeInterval2GridBaseCase;

	private SpatialAveragingInputData inputData;
	
	/*
	 * process first emission file: calculate weighted emissions per cell
	 * write R or GIS output files (weighted emissions, weighted demand, specific emissions)
	 * 
	 * if comparision to base case is selected: 
	 * process second emission file: calculate weighted emissions per cell
	 * calculate differences to base case
	 * write R or GIS output files
	 * 
	 */
	
	private void run() throws IOException{
		
		inputData = new SpatialAveragingInputData(baseCase, compareCase);
		
		if(useLineMethod){
			linkWeightUtil = new LinkLineWeightUtil(smoothingRadius_m);
		}else{
			linkWeightUtil = new LinkPointWeightUtil(inputData, noOfXbins, noOfYbins, smoothingRadius_m);
		}
		this.saWriter = new SpatialAveragingWriter(inputData, noOfXbins, noOfYbins, smoothingRadius_m);
		
		this.simulationEndTime = inputData.getEndTime();
		this.network = loadNetwork(inputData.getNetworkFile());		
		
		runBaseCase();
		
		if(compareToBaseCase){
			runCompareCase(inputData.getEmissionFileForCompareCase());
		}
	}
	
	private void runBaseCase() throws IOException{

		outPathStub = inputData.getAnalysisOutPathForBaseCase();
		
		parseEmissionFile(inputData.getEmissionFileForBaseCase());
		
		Map <Integer, Map<Id, Double>>timeInterval2Link2Pollutant = emissionHandler.getTimeIntervals2EmissionsPerLink();
		
		timeInterval2GridBaseCase= new SpatialGrid[noOfTimeBins];
		
		for(int timeInterval:timeInterval2Link2Pollutant.keySet()){
			SpatialGrid sGrid = new SpatialGrid(inputData, noOfXbins, noOfYbins);
			
			for(Id linkId: timeInterval2Link2Pollutant.get(timeInterval).keySet()){
				sGrid.addLinkValue(network.getLinks().get(linkId), timeInterval2Link2Pollutant.get(timeInterval).get(linkId), linkWeightUtil);
			}
			sGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
			// store base case results for comparison
			timeInterval2GridBaseCase[timeInterval] = sGrid;
			Double endOfTimeInterval = (timeInterval+1)/noOfTimeBins*simulationEndTime;
			// print tables
			writeOutput(sGrid, endOfTimeInterval);
			}
		}
	
	private void runCompareCase(String emissionFile) throws IOException{
	
		outPathStub = inputData.getAnalysisOutPathForCompareCase();
		
		parseEmissionFile(inputData.getEmissionFileForCompareCase());
		
		Map <Integer, Map<Id, Double>>timeInterval2Link2Pollutant = emissionHandler.getTimeIntervals2EmissionsPerLink();
		
		for(int timeInterval:timeInterval2Link2Pollutant.keySet()){
			SpatialGrid sGrid = new SpatialGrid(inputData, noOfXbins, noOfYbins);
			
			for(Id linkId: timeInterval2Link2Pollutant.get(timeInterval).keySet()){
				sGrid.addLinkValue(network.getLinks().get(linkId), timeInterval2Link2Pollutant.get(timeInterval).get(linkId), linkWeightUtil);
			}
			sGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
			// calc differences
			SpatialGrid differencesGrid = new SpatialGrid(inputData, noOfXbins, noOfYbins);
			differencesGrid = sGrid.getDifferences(timeInterval2GridBaseCase[timeInterval]);
			Double endOfTimeInterval = (timeInterval+1)/noOfTimeBins*simulationEndTime;
			// print tables
			writeOutput(differencesGrid, endOfTimeInterval);
		}			
	}

	private void parseEmissionFile(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.emissionHandler = new EmissionsPerLinkAndTimeIntervalEventHandler(this.simulationEndTime, noOfTimeBins, pollutant2analyze);
		eventsManager.addHandler(emissionHandler);
		emissionReader.parse(emissionFile);
	}

	private void writeOutput(SpatialGrid sGrid, Double endOfTimeInterval)
			throws IOException {
		if(writeRoutput){
			logger.info("Starting to write R output.");
			this.saWriter.writeRoutput(sGrid.getAverageValuesOfGrid(), outPathStub + ".Routput." + pollutant2analyze.toString() + ".g." + endOfTimeInterval + ".txt");
			this.saWriter.writeRoutput(sGrid.getWeightsOfGrid(), outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
			this.saWriter.writeRoutput(sGrid.getAverageValuesOfGrid(), outPathStub+ ".Routput." + pollutant2analyze + ".gPerVkm." + endOfTimeInterval + ".txt");
			}
		if(writeGisOutput){
			this.saWriter.writeGISoutput(sGrid.getWeightedValuesOfGrid(), outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp", endOfTimeInterval);
			this.saWriter.writeGISoutput(sGrid.getWeightsOfGrid(), outPathStub + ".GISoutput.Demand.vkm.movie.shp", endOfTimeInterval);
			this.saWriter.writeGISoutput(sGrid.getAverageValuesOfGrid(), outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp",endOfTimeInterval);
			}
		logger.info("Done writing R output to " + outPathStub);
	}
	
	private Network loadNetwork(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario.getNetwork();
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingDemandEmissions().run();
	}
}