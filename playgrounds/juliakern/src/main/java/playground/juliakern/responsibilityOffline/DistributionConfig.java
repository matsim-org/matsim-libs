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

package playground.juliakern.responsibilityOffline;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class DistributionConfig implements DistributionConfiguration {
	
	private static final Logger logger = Logger.getLogger(DistributionConfig.class);
	
	private final static String runDirectory1 = "input/sample/";
	private final String netFile1 = runDirectory1 + "sample_network.xml.gz";
	private final String munichShapeFile = runDirectory1 + "cityArea.shp";

	private static String configFile1 = runDirectory1 + "sample_config.xml.gz";
//	private final String emissionFile1 = runDirectory1 + "basecase.sample.emission.events.xml";
//	String plansFile1 = runDirectory1+"basecase.sample.plans.xml";
//	String eventsFile1 = runDirectory1+"basecase.sample.events.xml";
//	String outPathStub = "output/sample/basecase_30timebins_";
	
	private final String emissionFile1 = runDirectory1 + "compcase.sample.emission.events.xml";
	String plansFile1 = runDirectory1+"compcase.sample.plans.xml";
	String eventsFile1 = runDirectory1+"compcase.sample.events.xml";
	String outPathStub = "output/sample/compcase_30timebins_160x120cells_";

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	final int noOfTimeBins = 30; //30
	final int noOfXbins = 160; //160 
	final int noOfYbins = 120; //120
	final WarmPollutant warmPollutant2analyze = WarmPollutant.NO2;
	final ColdPollutant coldPollutant2analyze = ColdPollutant.NO2;
		
	
	Network network;
	double simulationEndTime;
	Config config;
	private Scenario scenario;
	private boolean storeResponsibilityEvents = true;
	double timeBinSize;
	
	
	/*
	 * load scenario
	 * calculate time bin size
	 */
	public DistributionConfig() {
		this.config = ConfigUtils.createConfig();
		ConfigReader configReader = new ConfigReader(this.config);
		configReader.readFile(configFile1);
		simulationEndTime = config.qsim().getEndTime();
		timeBinSize = simulationEndTime/noOfTimeBins;
		this.config.network().setInputFile(netFile1);
		this.config.plans().setInputFile(plansFile1);
		this.scenario = ScenarioUtils.loadScenario(config);
		logger.info("Simulation end time is: " + simulationEndTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (simulationEndTime / 3600 / noOfTimeBins) + " hour time bins.");
		
		if(noOfXbins<4 || noOfYbins<4){
			logger.warn("Emission distribution works better for larger grids, i.e. more than 10 x and y bins.");
		}
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getSimulationEndTime()
	 */
	@Override
	public Double getSimulationEndTime() {
		return this.simulationEndTime;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#storeResponsibilityEvents()
	 */
	@Override
	public boolean storeResponsibilityEvents() {
		return storeResponsibilityEvents;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getLinks()
	 */
	@Override
	public Map<Id<Link>, ? extends Link> getLinks() {
		return this.scenario.getNetwork().getLinks();
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getMunichShapeFile()
	 */
	@Override
	public String getMunichShapeFile() {
		return munichShapeFile;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getEventsFile()
	 */
	@Override
	public String getEventsFile() {
		return eventsFile1;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getEmissionFile()
	 */
	@Override
	public String getEmissionFile() {
		return emissionFile1;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getOutPathStub()
	 */
	@Override
	public String getOutPathStub() {
		return outPathStub;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getXmin()
	 */
	@Override
	public double getXmin() {
		return xMin;
	}
	
	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getXmax()
	 */
	@Override
	public double getXmax() {
		return xMax;
	}
	
	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getYmin()
	 */
	@Override
	public double getYmin() {
		return yMin;
	}
	
	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getYmax()
	 */
	@Override
	public double getYmax() {
		return yMax;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getNoOfTimeBins()
	 */
	@Override
	public int getNoOfTimeBins() {
		return noOfTimeBins;
	}
	
	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getNumberOfXBins()
	 */
	@Override
	public int getNumberOfXBins(){
		return noOfXbins;
	}
	
	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getNumberOfYBins()
	 */
	@Override
	public int getNumberOfYBins(){
		return noOfYbins;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getWarmPollutant2analyze()
	 */
	@Override
	public WarmPollutant getWarmPollutant2analyze() {
		return warmPollutant2analyze;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getColdPollutant2analyze()
	 */
	@Override
	public ColdPollutant getColdPollutant2analyze() {
		return coldPollutant2analyze;
	}

	/* (non-Javadoc)
	 * @see playground.julia.distribution.DistributionConfiguration#getTimeBinSize()
	 */
	@Override
	public Double getTimeBinSize() {
		return timeBinSize;
	}
	
	public Config getConfig(){
		return this.config;
	}

	public String getNetworkFile() {
		return this.netFile1;
	}

	public String getPlansFile() {
		// TODO Auto-generated method stub
		return this.plansFile1;
	}


	@Override
	public Map<Id<Link>, Integer> getLink2xBin() {
		Map<Id<Link>, Integer> link2xbin = new HashMap<>();
		for(Id<Link> linkId: this.getLinks().keySet()){
			link2xbin.put(linkId, mapXCoordToBin(this.getLinks().get(linkId).getCoord().getX()));
		}
		return link2xbin;
	}

	@Override
	public Map<Id<Link>, Integer> getLink2yBin() {
		Map<Id<Link>, Integer> link2ybin = new HashMap<>();
		for(Id<Link> linkId: this.getLinks().keySet()){
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
}
