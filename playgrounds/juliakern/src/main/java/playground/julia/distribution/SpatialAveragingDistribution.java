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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;

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
	private final String emissionFile1 = runDirectory1 + "basecase.sample.emission.events.xml";
	String plansFile1 = runDirectory1+"basecase.sample.plans.xml";
	String eventsFile1 = runDirectory1+"basecase.sample.events.xml";
	
	Network network;
	Collection<SimpleFeature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub = "output/sample";
	double pollutionFactorOutdoor, pollutionFactorIndoor;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	final int noOfTimeBins = 1; // one bin for each hour? //TODO 30? sim endtime ist 30...
	double timeBinSize;
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final String pollutant2analyze = WarmPollutant.NO2.toString();

	private boolean printPersonalInformation = false;

	private boolean storeEvents = false;

	Map<Id, Integer> link2xbin;
	Map<Id, Integer> link2ybin;

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
		
		Map<Double, ArrayList<EmPerBin>> emissionPerBin = new HashMap<Double, ArrayList<EmPerBin>>();
		Map<Double, ArrayList<EmPerLink>> emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
		
		generateActivitiesAndTrips(eventsFile1, activities, carTrips);
		generateEmissions(emissionFile1, emissionPerBin, emissionPerLink);
		
		/*
		 * two lists to store information on exposure and responsibility 
		 * responsibility: responsible person id, emission value x time - from  (car trips and emission per link), (activities and emission per bin)
		 * exposure: person id, emission value x time - from  (car trips and emission per link), (activities and emission per bin)
		 * 
		 * TODO later: write emission per bin/link into xml... handle as responsibility events
		 * TODO later: write emission per bin/link into xml... handle as exposure events
		 */
		
		ArrayList<ResponsibilityEvent> responsibility = new ArrayList<ResponsibilityEvent>();
		ArrayList<ExposureEvent> exposure = new ArrayList<ExposureEvent>();
		
		ResponsibilityUtils reut = new ResponsibilityUtils();
		reut.addExposureAndResponsibilityBinwise(activities, emissionPerBin, exposure, responsibility, timeBinSize, simulationEndTime);
		reut.addExposureAndResponsibilityLinkwise(carTrips, emissionPerLink, exposure, responsibility, timeBinSize, simulationEndTime);
		
		/*
		 * analysis
		 * exposure analysis: sum, average, welfare, personal time table
		 * responsibility analysis: sum, average, welfare
		 *  TODO
		 */
		
		//TODO mit case-namen versehen?
		ExposureUtils exut = new ExposureUtils();
		if(printPersonalInformation){
			exut.printPersonalExposureTimeTables(exposure, outPathStub+"personalExposure.txt");
			exut.printPersonalResponibility(responsibility, outPathStub + "personalExposure.txt");
		}
		exut.printExposureInformation(exposure, outPathStub+"exposure.txt");
		exut.printResponsibilityInformation(responsibility, outPathStub+"responsibility.txt");
		
		if(storeEvents){
			//TODO write events file for respons. and exposure
		}
		

		}
		


	private void generateEmissions(String emissionFile,
			Map<Double, ArrayList<EmPerBin>> emissionPerBin,
			Map<Double, ArrayList<EmPerLink>> emissionPerLink) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		GeneratedEmissionsHandler generatedEmissionsHandler = new GeneratedEmissionsHandler(0.0, timeBinSize, link2xbin, link2ybin, emissionFile);
		eventsManager.addHandler(generatedEmissionsHandler);
		
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(emissionFile);
		
		emissionPerBin = generatedEmissionsHandler.getEmissionsPerCell();
		emissionPerLink = generatedEmissionsHandler.getEmissionsPerLink();
		
		eventsManager.removeHandler(generatedEmissionsHandler);
		
	}

	private void generateActivitiesAndTrips(String eventsFile,
			ArrayList<EmActivity> activities, ArrayList<EmCarTrip> carTrips) {
		// from eventsfile -> car trips, activities: store
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		IntervalHandler intervalHandler = new IntervalHandler();
		eventsManager.addHandler(intervalHandler);
		
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
				
		intervalHandler.addActivitiesToTimetables(activities, link2xbin, link2ybin);
		intervalHandler.addCarTripsToTimetables(carTrips);
		
		eventsManager.removeHandler(intervalHandler);
		
	}

	private Map<Id, Integer> calculateYbins() {
		Map<Id, Integer> link2ybin = new HashMap<Id, Integer>();
		for(Id linkId: network.getLinks().keySet()){
			link2ybin.put(linkId, mapYCoordToBin(network.getLinks().get(linkId).getCoord().getY()));
		}
		return link2ybin;
	}

	private Map<Id, Integer> calculateXbins() {
		Map<Id, Integer> link2xbin = new HashMap<Id, Integer>();
		for(Id linkId: network.getLinks().keySet()){
			link2xbin.put(linkId, mapYCoordToBin(network.getLinks().get(linkId).getCoord().getX()));
		}
		return link2xbin;
	}


	private Double calculateAvgDifference(Map<Id, Double> person2emission,
			Map<Id, Double> person2emission2) {
		
		Double sum = 0.0;
		for(Id personId: person2emission.keySet()){
			sum += person2emission2.get(personId)-person2emission.get(personId);
		}
		return sum/person2emission.size();
	}

	private Map<Id, Double> calculatePersonalConcentration(	Map<Double, double[][]> time2WeightedEmissions,
			Population pop) {
		
		Map<Id, Double> person2emission = new HashMap<Id, Double>();
		// for each person
		// calculate time spend in each bin
		// calculate corresponding concentration - in total //TODO erweitern
		// acitivty types home shopping leisure home pickup work education
		// leg mod bike walk car pt
		for(Id personId: pop.getPersons().keySet()){
			person2emission.put(personId, 0.0);
			Plan plan = pop.getPersons().get(personId).getSelectedPlan();
			
			for(PlanElement pe: plan.getPlanElements()){
				if(pe instanceof Activity){
					Double poll = getExposureFromActivity(time2WeightedEmissions, pe, ((Activity) pe).getStartTime()); //TODO andere zeit benutzen?
					Double oldvalue = person2emission.get(personId);
					person2emission.put(personId, oldvalue+poll);
				}
				if(pe instanceof Leg){
					Leg pel = (Leg)pe;
					Double poll = getExposureFromLeg(time2WeightedEmissions, pel);
					Double oldvalue = person2emission.get(personId);
					person2emission.put(personId, oldvalue+poll);
					
				}
			}
			
			
		}
		return person2emission;
	}

	private Double getExposureFromLeg(
			Map<Double, double[][]> time2WeightedEmissions, Leg currentLeg) {
		Double travelTime = currentLeg.getTravelTime();
		Double currentTimeBin = Math.ceil(currentLeg.getDepartureTime()/timeBinSize)*timeBinSize;
		if(currentTimeBin<timeBinSize)currentTimeBin=timeBinSize;
		Id startLinkId = currentLeg.getRoute().getStartLinkId();
		Double xCoord = network.getLinks().get(startLinkId).getCoord().getX();
		Double yCoord = network.getLinks().get(startLinkId).getCoord().getY();
		int xBin = mapXCoordToBin(xCoord);
		int yBin = mapYCoordToBin(yCoord);
		Double poll;
		try {
			poll = travelTime * time2WeightedEmissions.get(currentTimeBin)[xBin][yBin]*pollutionFactorOutdoor;
		} catch (NullPointerException e) {
			poll =0.0;
		}
		return poll;
	}

	private Double getExposureFromActivity(
			Map<Double, double[][]> time2WeightedEmissions, PlanElement pe, Double endOfTimeInterval) {

		Activity currentActivity = (Activity)pe;

		if(endOfTimeInterval<timeBinSize)endOfTimeInterval=timeBinSize;
		if(endOfTimeInterval>simulationEndTime)endOfTimeInterval=simulationEndTime;
		
		int xBin= mapXCoordToBin(currentActivity.getCoord().getX());
		int yBin= mapYCoordToBin(currentActivity.getCoord().getY());
		
		// polution: time [sec] * binvalue * factor
		Double poll;
		try {
			poll = time2WeightedEmissions.get(endOfTimeInterval)[xBin][yBin]*pollutionFactorIndoor;
		} catch (NullPointerException e) {
			poll =0.0;
		}
		return poll;
	}
	
	private void writeRoutputForPersons(Map<Id, Double> basecase, Map<Id, Double> comparecase, String outputPathForR) {
		try {
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			
			
			// header: Person base case compare case difference
			valueString = "Person \t base case \t compare case \t absolute difference \t relative difference in percent";
			buffW.write(valueString);
			buffW.newLine();
			
			valueString = new String();
			
			for(Id personId: basecase.keySet()){
				
				Double baseValue = basecase.get(personId);
				Double compValue = comparecase.get(personId);
				
				// id
				valueString = personId.toString() + "\t";
				// base value
				valueString += baseValue + "\t";
				// compare value
				valueString += compValue + "\t";
				// absolute difference
				valueString += (compValue-baseValue)+"\t";
				// relative difference
				valueString += ((compValue-baseValue)/baseValue);
								
				buffW.write(valueString);
				buffW.newLine();
				valueString = new String();
			}
			
			buffW.close();	
		} catch (IOException e) {
			throw new RuntimeException("Failed writing output for R.");
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
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