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
package playground.benjamin.utils.spatialAvg;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.utils.spatialAvg.SpatialAveragingWriter;


/**
 * @author benjamin, julia
 *
 */
public class SpatialAveragingDemandEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDemandEmissions.class);

//	private String baseCase = "manteuffel";
//	/*
//	 * When 'base' is used no comparison to a policy case is calculated.
//	 */
//	private String compareCase = "p4";
//	
//	final int noOfTimeBins = 1;
//	
//	final String pollutant2analyze = WarmPollutant.NOX.toString();
////	final String pollutant2analyze = WarmPollutant.NO2.toString();
//	private boolean writeRoutput = true;
//	private boolean writeGisOutput = false;
	
	/* If both of the following booleans are false, the fallback solution
	 * is to use the "pointMethod", i.e. mapping emissions to the center
	 * of each link.
	 */
	final boolean useLineMethod = true;
	private boolean useCellMethod = false;

	private SpatialAveragingWriter saWriter;
	private double simulationEndTime;
	private Network network;
	private String outPathStub;
	private EmissionsPerLinkAndTimeIntervalEventHandler emissionHandler;
	private LinkWeightUtil linkWeightUtil;
	private SpatialGrid[] timeInterval2GridBaseCase;

	private SpatialAveragingInputData inputData;
	
	public void run() throws IOException{
//		inputData = new SpatialAveragingInputData(baseCase, compareCase);
		if(useLineMethod){
			linkWeightUtil = new LinkLineWeightUtil(inputData);
		}else{
			linkWeightUtil = new LinkPointWeightUtil(inputData);
		}
		this.saWriter = new SpatialAveragingWriter(inputData);
		this.simulationEndTime = inputData.getEndTime();
		this.network = loadNetwork(inputData.getNetworkFile());		
		runBaseCase();
		if(!(inputData.isUseBaseCaseOnly())){
			runCompareCase(inputData.getEmissionFileForCompareCase());
		}
		logger.info("Done with spatial averaging.");
	}
	
	@SuppressWarnings("unchecked")
	private void runBaseCase() throws IOException{
		outPathStub = inputData.getAnalysisOutPathBaseCase();
		parseEmissionFile(inputData.getEmissionFileForBaseCase());
		Map<Integer, Map<Id<Link>, EmissionsAndVehicleKm>>timeInterval2Link2Pollutant = emissionHandler.getTimeIntervals2EmissionsPerLink();
		timeInterval2GridBaseCase= new SpatialGrid[inputData.getNoOfTimeBins()];
		
		for(int timeInterval:timeInterval2Link2Pollutant.keySet()){
			logger.info("Calculating grid values for time interval " + (timeInterval+1) + " of " + inputData.getNoOfTimeBins() + " time intervals.");
			// TODO: change to inputData only?
			SpatialGrid sGrid = new SpatialGrid(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins());
			if(useCellMethod){
				linkWeightUtil = new CellWeightUtil((Collection<Link>) network.getLinks().values(), sGrid);
			}
			for(Id<Link> linkId: timeInterval2Link2Pollutant.get(timeInterval).keySet()){
				sGrid.addLinkValue(network.getLinks().get(linkId), timeInterval2Link2Pollutant.get(timeInterval).get(linkId), linkWeightUtil);
			}
			Double factor = linkWeightUtil.getNormalizationFactor()*inputData.getScalingFactor();
			sGrid.multiplyAllCells(factor);
			
			// store base case results for comparison
			timeInterval2GridBaseCase[timeInterval] = sGrid;
			Double endOfTimeInterval = simulationEndTime/inputData.getNoOfTimeBins()*(timeInterval+1);
	
			// print tables
			logger.info("Writing output for the current time interval");
			writeOutput(sGrid, endOfTimeInterval);
			}
		}
	
	private void runCompareCase(String emissionFile) throws IOException{
		logger.info("Starting with compare case.");
		outPathStub = inputData.getAnalysisOutPathCompareCase();
		parseEmissionFile(inputData.getEmissionFileForCompareCase());
		Map<Integer, Map<Id<Link>, EmissionsAndVehicleKm>>timeInterval2Link2Pollutant = emissionHandler.getTimeIntervals2EmissionsPerLink();
		
		for(int timeInterval:timeInterval2Link2Pollutant.keySet()){
			logger.info("Calculating differences to base case for time interval " + (timeInterval+1) + " of " + inputData.getNoOfTimeBins() + " time intervals.");
			SpatialGrid sGrid = new SpatialGrid(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins());
			
			for(Id<Link> linkId: timeInterval2Link2Pollutant.get(timeInterval).keySet()){
				sGrid.addLinkValue(network.getLinks().get(linkId), timeInterval2Link2Pollutant.get(timeInterval).get(linkId), linkWeightUtil);
			}
			sGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor()*inputData.getScalingFactor());
			// calc differences
			SpatialGrid differencesGrid = new SpatialGrid(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins());
			differencesGrid = sGrid.getDifferences(timeInterval2GridBaseCase[timeInterval]);
			Double endOfTimeInterval = simulationEndTime/inputData.getNoOfTimeBins()*(timeInterval+1);
			// print tables
			logger.info("Writing output for the current time interval");
			writeOutput(differencesGrid, endOfTimeInterval);
		}			
	}

	private void parseEmissionFile(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.emissionHandler = new EmissionsPerLinkAndTimeIntervalEventHandler(network.getLinks(), this.simulationEndTime, inputData.getNoOfTimeBins(), inputData.getPollutant2analyze());
		eventsManager.addHandler(emissionHandler);
		emissionReader.readFile(emissionFile);
	}

	private void writeOutput(SpatialGrid sGrid, Double endOfTimeInterval)
			throws IOException {
		if(inputData.isWriteRoutput()){
			logger.info("Starting to write R output.");
			this.saWriter.writeRoutput(sGrid.getWeightedValuesOfGrid(), outPathStub + ".Routput." + inputData.getPollutant2analyze().toString() + ".g." + endOfTimeInterval + ".txt");
//			this.saWriter.writeRoutput(sGrid.getWeightedValuesOfGrid(), outPathStub + ".Routput." + inputData.getPollutant2analyze().toString() + ".g." + "21600-32400.0" + ".txt");
//			this.saWriter.writeRoutput(sGrid.getWeightedValuesOfGrid(), outPathStub + ".Routput." + inputData.getPollutant2analyze().toString() + ".g." + "57600-68400.0" + ".txt");
			this.saWriter.writeRoutput(sGrid.getWeightsOfGrid(), outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
			this.saWriter.writeRoutput(sGrid.getAverageValuesOfGrid(), outPathStub+ ".Routput." + inputData.getPollutant2analyze() + ".gPerVkm." + endOfTimeInterval + ".txt");
			}
		if(inputData.isWriteGisOutput()){
			this.saWriter.writeGISoutput(sGrid.getWeightedValuesOfGrid(), outPathStub +  ".GISoutput." + inputData.getPollutant2analyze().toString() + ".g.movie.shp", endOfTimeInterval);
			this.saWriter.writeGISoutput(sGrid.getWeightsOfGrid(), outPathStub + ".GISoutput.Demand.vkm.movie.shp", endOfTimeInterval);
			this.saWriter.writeGISoutput(sGrid.getAverageValuesOfGrid(), outPathStub +  ".GISoutput." + inputData.getPollutant2analyze().toString() + ".gPerVkm.movie.shp",endOfTimeInterval);
			}
		logger.info("Done writing R output to " + outPathStub);
	}
	
	private Network loadNetwork(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario.getNetwork();
	}

	public void setInputData(SpatialAveragingInputData inputData) {
		this.inputData = inputData;
	}
}