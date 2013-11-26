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

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

public class DistributionConfig {
	
	final double scalingFactor = 100;
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
	String outPathStub = "output/sample/compcase_30timebins_";
	
	Network network;
	Collection<SimpleFeature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	double pollutionFactorOutdoor, pollutionFactorIndoor;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	final int noOfTimeBins = 30; // one bin for each hour? //TODO 30? sim endtime ist 30...
	double timeBinSize;
	final int noOfXbins = 160; //TODO rethink this
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final WarmPollutant warmPollutant2analyze = WarmPollutant.NO2;
	final ColdPollutant coldPollutant2analyze = ColdPollutant.NO2;
	
	Config config;
	private Scenario scenario;
	private boolean storeResponsibilityEvents = true;
	private int numberOfTimeBins = 30;
	
	/*
	 * load scenario
	 * calculate time bin size
	 */
	public DistributionConfig(Logger logger) {
		this.config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(this.config);
		configReader.readFile(configFile1);
		simulationEndTime = config.qsim().getEndTime();
		timeBinSize = simulationEndTime/noOfTimeBins;
		this.config.network().setInputFile(netFile1);
		this.config.plans().setInputFile(plansFile1);
		this.scenario = ScenarioUtils.loadScenario(config);
		logger.info("Simulation end time is: " + simulationEndTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (simulationEndTime / 3600 / noOfTimeBins) + " hour time bins.");
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

	public String getMunichShapeFile() {
		return munichShapeFile;
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
		return numberOfTimeBins;
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

}
