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
	//private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + "sample_network.xml.gz";
	private final String munichShapeFile = runDirectory1 + "cityArea.shp";

	private static String configFile1 = runDirectory1 + "sample_config.xml.gz";
	//private final static Integer lastIteration1 = getLastIteration(configFile1);
	//private static String configFile2 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	//private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String emissionFile1 = runDirectory1 + "basecase.sample.emission.events.xml";
	private final String emissionFile2 = runDirectory1 + "compcase.sample.emission.events.xml";
	String plansFile1 = runDirectory1+"basecase.sample.plans.xml";
	String plansFile2 = runDirectory1+"compcase.sample.plans.xml";
	String eventsFile1 = runDirectory1+"basecase.sample.events.xml";
	String eventsFile2 = runDirectory1+"compcase.sample.events.xml";
	
	Network network;
	Collection<SimpleFeature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub;
	double pollutionFactorOutdoor, pollutionFactorIndoor;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	final int noOfTimeBins = 1; // one bin for each hour //TODO 30? sim endtime ist 30...
	double timeBinSize;
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBaseCase = true;

	private boolean createPersonalExposurePlans = true;

	private boolean simpleExposureOnLegs = false;

	private String car = "car";
	
//	Map<Double, Map<Id, Map<String, Double>>> time2EmissionMapToAnalyze_g = new HashMap<Double, Map<Id,Map<String,Double>>>();
//	Map<Double, Map<Id, Double>> time2DemandMapToAnalyze_vkm = new HashMap<Double, Map<Id,Double>>();


	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		
		this.featuresInMunich = ShapeFileReader.getAllFeatures(munichShapeFile);
		
		pollutionFactorOutdoor = 1.;//1/(binArea)
		pollutionFactorIndoor = .5;
		timeBinSize = simulationEndTime/noOfTimeBins;
		
		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();

		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal1 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal1, time2ColdEmissionsTotal1);
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled1 = setNonCalculatedEmissions(time2EmissionsTotal1);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1 = filterEmissionLinks(time2EmissionsTotalFilled1);
		
		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		logger.info("Done creating time2emission maps.");
		
		Map<Double, double[][]> time2WeightedEmissions1 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered1);

		logger.info("Done calculating weighted emissions");
		
		Population pop = scenario.getPopulation();	
		Map<Id, Double> person2emission = calculatePersonalConcentration(time2WeightedEmissions1,pop);
		ExposureUtils exut = new ExposureUtils();
		
		
		if(createPersonalExposurePlans){
			List<PersonalExposure> popExposure = new LinkedList<PersonalExposure>();
			for(Id personId: pop.getPersons().keySet()){
				PersonalExposure perEx = new PersonalExposure(personId);
				popExposure.add(perEx);
				
				for(PlanElement pe: pop.getPersons().get(personId).getSelectedPlan().getPlanElements()){
					if(pe instanceof Activity){
						handleActivityPlanElement(time2WeightedEmissions1, perEx,pe); // add entry for each activity to personal timetables
					}
					if(pe instanceof Leg){
						Leg pel = (Leg)pe;
						String actType = pel.getMode();
						Double poll;
						if(actType!=car){ // use simple exposure model
							poll = getExposureFromLeg(time2WeightedEmissions1, pel);
							perEx.addExposureIntervall(pel.getDepartureTime(), pel.getDepartureTime()+pel.getTravelTime(), poll, actType);
						}else{ // =car
							if(simpleExposureOnLegs){
								poll= getExposureFromLeg(time2WeightedEmissions1, pel);
								perEx.addExposureIntervall(pel.getDepartureTime(), pel.getDepartureTime()+pel.getTravelTime(), poll, actType);
							}else{ // exposure for each link 
								// generated from events - nothing to do here
							}
						}	
					}	
				}				
			}

			
			if(!simpleExposureOnLegs){
				// add exposure for 'car'
				
				EventsManager eventsManager = EventsUtils.createEventsManager();
				
				CarIntervalHandler carIntervalHandler = new CarIntervalHandler();
				eventsManager.addHandler(carIntervalHandler);
				
				MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
				matsimEventsReader.readFile(eventsFile1);
				
				carIntervalHandler.addIntervalsToTimetables(popExposure, time2EmissionsTotalFilledAndFiltered1, timeBinSize, pollutant2analyze, pollutionFactorOutdoor);
				
			}
			

			logger.info("Calculated time tables of personal exposure.");
			
			String outPathForTimeTables = runDirectory1 + "numberofIntervals"+ noOfTimeBins + "outputTimeTables.txt";
			if(!simpleExposureOnLegs)outPathForTimeTables = runDirectory1 + "numberofIntervals"+ noOfTimeBins + "detailedLegExposure" + "outputTimeTables.txt";
			exut.printTimeTables(popExposure, outPathForTimeTables);
			
			logger.info("Done calculating personal time dependent exposure. Results written to " + outPathForTimeTables);
		}
		
		if(compareToBaseCase){
			processEmissions(emissionFile2);
			Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal2 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal2 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();

			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal2 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal2, time2ColdEmissionsTotal2);
			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled2 = setNonCalculatedEmissions(time2EmissionsTotal2);
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered2 = filterEmissionLinks(time2EmissionsTotalFilled2);

			Map<Double, double[][]> time2WeightedEmissions2 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered2);
			
			Map<Id, Double> person2emission2 = calculatePersonalConcentration(time2WeightedEmissions2, pop);

			String outputPathForR= runDirectory1+"Routput_perPerson.txt";
			writeRoutputForPersons(person2emission, person2emission2, outputPathForR);
			
			logger.info("Average difference " + calculateAvgDifference(person2emission, person2emission2));
			
			if(createPersonalExposurePlans){
				
				Scenario scenario2 = loadScenario2(netFile1);
				this.network = scenario2.getNetwork();
				Population pop2 = scenario2.getPopulation();
				//---------

				List<PersonalExposure> popExposure2 = new LinkedList<PersonalExposure>();
				for(Id personId: pop2.getPersons().keySet()){
					PersonalExposure perEx = new PersonalExposure(personId);
					popExposure2.add(perEx);
					
					for(PlanElement pe: pop2.getPersons().get(personId).getSelectedPlan().getPlanElements()){
						if(pe instanceof Activity){
							handleActivityPlanElement(time2WeightedEmissions1, perEx,pe); // add entry for each activity to personal timetables
						}
						if(pe instanceof Leg){
							Leg pel = (Leg)pe;
							String actType = pel.getMode();
							Double poll;
							if(actType!=car){ // use simple exposure model
								poll = getExposureFromLeg(time2WeightedEmissions1, pel);
								perEx.addExposureIntervall(pel.getDepartureTime(), pel.getDepartureTime()+pel.getTravelTime(), poll, actType);
							}else{ // =car
								if(simpleExposureOnLegs){
									poll= getExposureFromLeg(time2WeightedEmissions1, pel);
									perEx.addExposureIntervall(pel.getDepartureTime(), pel.getDepartureTime()+pel.getTravelTime(), poll, actType);
								}else{ // exposure for each link 
									// generated from events - nothing to do here
								}
							}	
						}	
					}				
				}

				
				if(!simpleExposureOnLegs){
					// add exposure for 'car'
					
					EventsManager eventsManager = EventsUtils.createEventsManager();
					
					CarIntervalHandler carIntervalHandler = new CarIntervalHandler();
					eventsManager.addHandler(carIntervalHandler);
					
					MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
					matsimEventsReader.readFile(eventsFile1);
					
					carIntervalHandler.addIntervalsToTimetables(popExposure2, time2EmissionsTotalFilledAndFiltered1, timeBinSize, pollutant2analyze, pollutionFactorOutdoor);
					
				}
				

				logger.info("Calculated time tables of personal exposure.");
				
				String outPathForTimeTables = runDirectory1 + "numberofIntervals"+ noOfTimeBins + "outputTimeTables_compareCase.txt";
				if(!simpleExposureOnLegs)outPathForTimeTables = runDirectory1 + "numberofIntervals"+ noOfTimeBins + "detailedLegExposure" + "outputTimeTables_compareCase.txt";
				exut.printTimeTables(popExposure2, outPathForTimeTables);
				
				logger.info("Done calculating personal time dependent exposure. Results written to " + outPathForTimeTables);
			
				//------------
				
			}
		}
		
		
	}

	private void handleActivityPlanElement(
			Map<Double, double[][]> time2WeightedEmissions1,
			PersonalExposure perEx, PlanElement pe) {
		
			Activity pea = (Activity)pe;
			String actType = pea.getType();
			// number of time bins matching activity time
			int firstTimeBin = (int) Math.ceil(pea.getStartTime()/timeBinSize);
			if(firstTimeBin==0)firstTimeBin=1;
			int lastTimeBin;
			if(pea.getEndTime()>0.0){
				lastTimeBin = (int) Math.ceil(pea.getEndTime()/timeBinSize);
			}else{
				lastTimeBin = (int) Math.ceil(simulationEndTime/timeBinSize);
			}
			
			if (firstTimeBin<lastTimeBin) {
				//first bin
				Double firstStartTime = pea.getStartTime();
				Double firstEndTime = firstTimeBin * timeBinSize;
				Double firstpoll = getExposureFromActivity(time2WeightedEmissions1, pea, firstEndTime);
				perEx.addExposureIntervall(firstStartTime, firstEndTime, firstpoll, actType);
				
				// inner time bins
				for (int i = firstTimeBin + 1; i < lastTimeBin; i++) {
					Double currStartTime = i * timeBinSize;
					Double currEndTime = (i + 1) * timeBinSize;
					Double poll = getExposureFromActivity(time2WeightedEmissions1, pe, currEndTime);
					perEx.addExposureIntervall(currStartTime,currEndTime, poll, actType);
				}
				
				// last bin
				Double lastStartTime = (lastTimeBin-1) * timeBinSize;
				Double lastEndTime = pea.getEndTime();
				if(lastEndTime<0.0)lastEndTime=simulationEndTime;
				Double lastpoll = getExposureFromActivity(time2WeightedEmissions1, pea, lastTimeBin*timeBinSize);
				perEx.addExposureIntervall(lastStartTime, lastEndTime, lastpoll, actType);
				
			}else{ // activity entirely in one interval
				
				Double startTime = pea.getStartTime();
				Double endTime = pea.getEndTime();
				if(endTime<0.0)endTime=simulationEndTime;
				Double poll = getExposureFromActivity(time2WeightedEmissions1, pea, lastTimeBin*timeBinSize);
				perEx.addExposureIntervall(startTime, endTime, poll, actType);
			}
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
	
	private Map<Double, double[][]> fillWeightedEmissionValues(Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered) {
		Map<Double, double[][]> time2weightedEmissions = new HashMap<Double, double[][]>();
		
		for(Double endOfTimeInterval : time2EmissionsTotalFilledAndFiltered.keySet()){
			double[][]weightedEmissions = new double[noOfXbins][noOfYbins];
			
			for(Id linkId : time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				
				double value = time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).get(linkId).get(this.pollutant2analyze);
				double scaledValue = this.scalingFactor * value;
				
				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell = calculateWeightOfLinkForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						weightedEmissions[xIndex][yIndex] += weightOfLinkForCell * scaledValue;					
					}
				}
			}
			time2weightedEmissions.put(endOfTimeInterval, weightedEmissions);
		}
		return time2weightedEmissions;
	}
	
	

	private Map<Double, double[][]> scale(Map<Double, double[][]> time2weightedValues){
		Map<Double, double[][]> time2scaledValues = new HashMap<Double, double[][]>();
		
		for(Double endOfTimeInterval : time2weightedValues.keySet()){
			double [][] unscaledValues = time2weightedValues.get(endOfTimeInterval);
			double [][] scaledValues = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					scaledValues[xIndex][yIndex] = this.scalingFactor * unscaledValues[xIndex][yIndex];
				}
			}
			time2scaledValues.put(endOfTimeInterval, scaledValues);
		}
		return time2scaledValues;
	}
	
	private boolean isInMunich(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInMunich){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}

	private boolean isInResearchArea(Coord linkCoord) {
		Double xLink = linkCoord.getX();
		Double yLink = linkCoord.getY();
		
		if(xLink > xMin && xLink < xMax){
			if(yLink > yMin && yLink < yMax){
				return true;
			}
		}
		return false;
	}

	private double calculateWeightOfLinkForCell(double x1, double y1, double x2, double y2) {
		double distanceSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
		return Math.exp((-distanceSquared) / (smoothinRadiusSquared_m));
	}
	

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin);
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin);
		Assert.equals(mapXCoordToBin(xBinCenter), xIndex);
		return xBinCenter ;
	}

	private Coord findCellCentroid(int xIndex, int yIndex) {
		double xCentroid = findBinCenterX(xIndex);
		double yCentroid = findBinCenterY(yIndex);
		Coord cellCentroid = new CoordImpl(xCentroid, yCentroid);
		return cellCentroid;
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

	private Map<Double, Map<Id, Double>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id, Double>> time2CountsPerLink) {
		Map<Double, Map<Id, Double>> time2CountsTotalFiltered = new HashMap<Double, Map<Id,Double>>();
	
		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id, Double> linkId2CountFiltered = new HashMap<Id, Double>();
		
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(isInResearchArea(linkCoord)){
					Id linkId = link.getId();
	
					if(linkId2Count.get(linkId) == null){
						linkId2CountFiltered.put(linkId, 0.);
					} else {
						linkId2CountFiltered.put(linkId, linkId2Count.get(linkId));
					}
				}
			}
			time2CountsTotalFiltered.put(endOfTimeInterval, linkId2CountFiltered);
		}
		return time2CountsTotalFiltered;
	}

	private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
		
		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	private Map<Double, Map<Id, Map<String, Double>>> filterEmissionLinks(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<Double, Map<Id, Map<String, Double>>>();
		
		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id, Map<String, Double>> emissionsTotalFiltered = new HashMap<Id, Map<String, Double>>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(isInResearchArea(linkCoord)){
					Id linkId = link.getId();
					emissionsTotalFiltered.put(linkId, emissionsTotal.get(linkId));
				}
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {
	
		Map<Double, Map<Id, SortedMap<String, Double>>> time2totalEmissions = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
	
		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			
			Map<Id, SortedMap<String, Double>> totalEmissions = new HashMap<Id, SortedMap<String, Double>>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = this.emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = this.emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}

	private void processEmissions(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		this.coldHandler = new EmissionsPerLinkColdEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		emissionReader.parse(emissionFile);
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	private Scenario loadScenario2(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile2);
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