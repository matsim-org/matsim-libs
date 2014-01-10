/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.julia.distribution;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

public class DistributionConfigTest implements DistributionConfiguration{
	
	private final static String runDirectory1 = "input/sample/";
	private final String netFile1 = runDirectory1 + "test_network.xml";
	private final String munichShapeFile = runDirectory1 + "cityArea.shp";

//	private static String configFile1 = runDirectory1 + "sample_config.xml.gz";
//	private final String emissionFile1 = runDirectory1 + "basecase.sample.emission.events.xml";
//	String plansFile1 = runDirectory1+"basecase.sample.plans.xml";
//	String eventsFile1 = runDirectory1+"basecase.sample.events.xml";
//	String outPathStub = "output/sample/basecase_30timebins_";
	
	private final String emissionFile1 = runDirectory1 + "test.emission.events.xml";
	String plansFile1 = runDirectory1+"test_plans.xml";
	String eventsFile1 = runDirectory1+"test.events.xml";
	String outPathStub = "output/sample/test";

//	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = .0;
	static double xMax = 100.;
	static double yMin = .0;
	static double yMax = 100.;

	final int noOfTimeBins = 30; //30
	final int noOfXbins = 10; //160 
	final int noOfYbins = 10; //120
	final WarmPollutant warmPollutant2analyze = WarmPollutant.NO2;
	final ColdPollutant coldPollutant2analyze = ColdPollutant.NO2;
		
	
	Network network;
	double simulationEndTime;
	Config config;
	private Scenario scenario;
	private boolean storeResponsibilityEvents = true;
	double timeBinSize;
	Logger logger;
	
	/*
	 * load scenario
	 * calculate time bin size
	 */
	public DistributionConfigTest(Logger logger) {
		this.config = ConfigUtils.createConfig();
		//MatsimConfigReader configReader = new MatsimConfigReader(this.config);
		//configReader.readFile(configFile1);
		config.addCoreModules(); // TODO
		//simulationEndTime = config.qsim().getEndTime();
		simulationEndTime = 60*60*24;
		timeBinSize = simulationEndTime/noOfTimeBins;
		this.config.network().setInputFile(netFile1);
		this.config.plans().setInputFile(plansFile1);
		this.scenario = ScenarioUtils.loadScenario(config);
		this.logger = logger;
		logger.info("Simulation end time is: " + simulationEndTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (simulationEndTime / 3600 / noOfTimeBins) + " hour time bins.");
		
		if(noOfXbins<4 || noOfYbins<4){
			logger.warn("Emission distribution works better for larger grids, i.e. more than 10 x and y bins.");
		}
	}

	public Double getSimulationEndTime() {
		return this.simulationEndTime;
	}

	public boolean storeResponsibilityEvents() {
		return storeResponsibilityEvents;
	}

	public Map<Id, ? extends Link> getLinks() {
		return this.scenario.getNetwork().getLinks();
	}



	public String getEventsFile() {
		return eventsFile1;
	}

	public String getEmissionFile() {
		return emissionFile1;
	}

	public String getOutPathStub() {
		return outPathStub;
	}

	public double getXmin() {
		return xMin;
	}
	
	public double getXmax() {
		return xMax;
	}
	
	public double getYmin() {
		return yMin;
	}
	
	public double getYmax() {
		return yMax;
	}

	public int getNoOfTimeBins() {
		return noOfTimeBins;
	}
	
	public int getNumberOfXBins(){
		return noOfXbins;
	}
	
	public int getNumberOfYBins(){
		return noOfYbins;
	}

	public WarmPollutant getWarmPollutant2analyze() {
		return warmPollutant2analyze;
	}

	public ColdPollutant getColdPollutant2analyze() {
		return coldPollutant2analyze;
	}

	public Double getTimeBinSize() {
		return timeBinSize;
	}

	@Override
	public Map<Id, Integer> getLink2xBin() {
		Map<Id, Integer> link2xbin = new HashMap<Id, Integer>();
		for(Id linkId: this.getLinks().keySet()){
			link2xbin.put(linkId, mapXCoordToBin(this.getLinks().get(linkId).getCoord().getX()));
		}
		return link2xbin;
	}

	@Override
	public Map<Id, Integer> getLink2yBin() {
		Map<Id, Integer> link2ybin = new HashMap<Id, Integer>();
		for(Id linkId: this.getLinks().keySet()){
			link2ybin.put(linkId, mapYCoordToBin(this.getLinks().get(linkId).getCoord().getY()));
		}
		return link2ybin;
	}


	private Integer mapYCoordToBin(double yCoord) {
		double yMin = this.getYmin();
		double yMax = this.getYmax();
		if (yCoord <= yMin || yCoord >= yMax) return null; // yCoord is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * this.getNumberOfYBins()); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		double xMin = this.getXmin();
		double xMax = this.getXmax();
		if (xCoord <= xMin  || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * this.getNumberOfXBins()); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	@Override
	public String getMunichShapeFile() {
		return this.munichShapeFile;
	}

}
