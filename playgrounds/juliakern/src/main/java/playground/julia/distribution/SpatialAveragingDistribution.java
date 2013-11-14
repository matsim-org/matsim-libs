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
package playground.julia.distribution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.vsp.emissions.events.EmissionEventsReader;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;



/**
 * @author benjamin, julia
 *
 */
public class SpatialAveragingDistribution {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDistribution.class);
	
	final double scalingFactor = 100;
	private final static String runDirectory1 = "input/sample/";
	private final String netFile1 = runDirectory1 + "sample_network.xml.gz";
	private final String munichShapeFile = runDirectory1 + "cityArea.shp";

	private static String configFile1 = runDirectory1 + "sample_config.xml.gz";
//	private final String emissionFile1 = runDirectory1 + "basecase.sample.emission.events.xml";
//	String plansFile1 = runDirectory1+"basecase.sample.plans.xml";
//	String eventsFile1 = runDirectory1+"basecase.sample.events.xml";
	private final String emissionFile1 = runDirectory1 + "compcase.sample.emission.events.xml";
	String plansFile1 = runDirectory1+"compcase.sample.plans.xml";
	String eventsFile1 = runDirectory1+"compcase.sample.events.xml";
	String outPathStub = "input/sample/";
	
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
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final WarmPollutant warmPollutant2analyze = WarmPollutant.NO2;
	final ColdPollutant coldPollutant2analyze = ColdPollutant.NO2;

	Map<Id, Integer> link2xbin;
	Map<Id, Integer> link2ybin;
	
	Map<Double, ArrayList<EmPerBin>> emissionPerBin ;
	Map<Double, ArrayList<EmPerLink>> emissionPerLink;

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		
		this.featuresInMunich = ShapeFileReader.getAllFeatures(munichShapeFile);
		
		pollutionFactorOutdoor = 1.;//1/(binArea)
		pollutionFactorIndoor = .5;
		timeBinSize = simulationEndTime/noOfTimeBins;
			
		/*
		 * generate two lists to store location of links 
		 * needed frequently
		 */
		
		link2xbin = calculateXbins();
		link2ybin = calculateYbins();
		
		/*
		 * four lists to store information on activities, car trips and emissions
		 * activities: time interval, person id, location - from events
		 * car trips: time interval, person id, link id - from events
		 * emission per bin: time bin, area bin, responsible person id - from emission events
		 * emission per link: time bin, link id, responsible person id - from emission events 
		 * 
		 * TODO later: write emission per bin/cell into xml?
		 * TODO later: write emission per link into xml?
		 * 
		 */
		
		ArrayList<EmActivity> activities = new ArrayList<EmActivity>();
		ArrayList<EmCarTrip> carTrips= new ArrayList<EmCarTrip>();
		
		emissionPerBin = new HashMap<Double, ArrayList<EmPerBin>>();
		emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
		
		generateActivitiesAndTrips(eventsFile1, activities, carTrips);
		generateEmissions(emissionFile1);
		
		/*
		 * two lists to store information on exposure and responsibility 
		 * responsibility: responsible person id, emission value x time - from  (car trips and emission per link), (activities and emission per bin)
		 * exposure: person id, emission value x time - from  (car trips and emission per link), (activities and emission per bin)
		 * 
		 * TODO later: write emission per bin/link into xml... handle as responsibility events
		 * TODO later: write emission per bin/link into xml... handle as exposure events
		 */
		
		ArrayList<ResponsibilityEventImpl> responsibility = new ArrayList<ResponsibilityEventImpl>();
		ArrayList<ExposureEventImpl> exposure = new ArrayList<ExposureEventImpl>();
		
		ResponsibilityUtils reut = new ResponsibilityUtils();
		reut.addExposureAndResponsibilityBinwise(activities, emissionPerBin, exposure, responsibility, timeBinSize, simulationEndTime);
		//reut.addExposureAndResponsibilityLinkwise(carTrips, emissionPerLink, exposure, responsibility, timeBinSize, simulationEndTime);
		
		/*
		 * analysis
		 * exposure analysis: sum, average, welfare, personal time table
		 * responsibility analysis: sum, average, welfare
		 *  TODO: welfare!
		 */
		
		//TODO mit case-namen versehen?
		ExposureUtils exut = new ExposureUtils();
		exut.printExposureInformation(exposure, outPathStub+"exposure.txt");
		exut.printResponsibilityInformation(responsibility, outPathStub+"responsibility.txt");
		exut.printPersonalResponsibilityInformation(responsibility, outPathStub+"personalResponsibility.txt");
		}

	private void generateEmissions(String emissionFile) {
				
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		
		GeneratedEmissionsHandler generatedEmissionsHandler = new GeneratedEmissionsHandler(0.0, timeBinSize, link2xbin, link2ybin, warmPollutant2analyze, coldPollutant2analyze);
		eventsManager.addHandler(generatedEmissionsHandler);
		
		emissionReader.parse(emissionFile1);

		emissionPerBin = generatedEmissionsHandler.getEmissionsPerCell();
		emissionPerLink = generatedEmissionsHandler.getEmissionsPerLink();
		
		logger.info("Done calculating emissions per bin and link.");
		
	}

	private void generateActivitiesAndTrips(String eventsFile,
			ArrayList<EmActivity> activities, ArrayList<EmCarTrip> carTrips) {
		// from eventsfile -> car trips, activities: store
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		IntervalHandler intervalHandler = new IntervalHandler();
		eventsManager.addHandler(intervalHandler);
		
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
				
		intervalHandler.addActivitiesToTimetables(activities, link2xbin, link2ybin, simulationEndTime);
		intervalHandler.addCarTripsToTimetables(carTrips, simulationEndTime);
		
		eventsManager.removeHandler(intervalHandler);
		
	}

	private Map<Id, Integer> calculateYbins() {
		Map<Id, Integer> link2ybin = new HashMap<Id, Integer>();
		for(Id linkId: network.getLinks().keySet()){
			link2ybin.put(linkId, mapYCoordToBin(network.getLinks().get(linkId).getCoord().getY()));
			if(mapYCoordToBin(network.getLinks().get(linkId).getCoord().getY())==null){
			}
		}
		return link2ybin;
	}

	private Map<Id, Integer> calculateXbins() {
		Map<Id, Integer> link2xbin = new HashMap<Id, Integer>();
		for(Id linkId: network.getLinks().keySet()){
			link2xbin.put(linkId, mapXCoordToBin(network.getLinks().get(linkId).getCoord().getX()));
		}
		return link2xbin;
	}

	private Integer mapYCoordToBin(double yCoord) {
		if (yCoord <= yMin || yCoord >= yMax) return null; // yCoord is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingDistribution().run();
	}
}