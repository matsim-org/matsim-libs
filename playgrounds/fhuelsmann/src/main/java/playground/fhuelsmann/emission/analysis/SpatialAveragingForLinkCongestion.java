/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkCongestion.java
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
package playground.fhuelsmann.emission.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;

import playground.benjamin.events.emissions.ColdPollutant;
import playground.benjamin.events.emissions.EmissionEventsReader;
import playground.benjamin.events.emissions.WarmPollutant;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

public class SpatialAveragingForLinkCongestion {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkCongestion.class);

	private final static String runNumber1 = "981";
	private final static String runDirectory1 = "../../run" + runNumber1 + "/"+ runNumber1+".";
	private final static String configFile = runDirectory1 + "output_config.xml.gz";
	private final static Integer lastIteration = getLastIteration(configFile);
	private final String netFile = runDirectory1 + "output_network.xml.gz";
	private final String eventsFile = "../../run981/ITERS/it.1500/981.1500.events.xml.gz";
	

	private final String outPath = "../../run982/emissions/" +  "-" + runNumber1 + ".";

	Scenario scenario;
	Network network;
	private FeatureType featureType;
	CongestionPerLinkHandler congestionHandler; 
	SortedSet<String> listOfPollutants;

	static int noOfTimeBins = 1;
	double simulationEndTime;

	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	static int noOfXbins = 80;
	static int noOfYbins = 60;
	static int minimumNoOfLinksInCell = 1;

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile);
		defineListOfPollutants();
		loadScenario(netFile);
		this.network = this.scenario.getNetwork();
		initFeatures();

		processCongestion(eventsFile);
		Map<Double, Map<Id, Double>> time2congestionTotal1 = this.congestionHandler.getCongestionPerLinkAndTimeInterval();
		Map<Double, Map<Id, Double>>time2CongestionTotalFiltered1 = setNonCalculatedCongestionAndFilter(time2congestionTotal1);

		this.congestionHandler.reset(0);

	/*	processEmissions(emissionFile2);
		Map<Double, Map<Id, Map<String, Double>>> time2congestionTotal2 = this.congestionHandler.getCongestionPerLinkAndTimeInterval();
		
		Map<Double, Map<Id, Map<String, Double>>> time2CongestionTotalFiltered2 = setNonCalculatedEmissionsAndFilter(time2congestionTotal2);*/

	//	Map<Double, Map<Id, Map<String, Double>>> time2deltaEmissionsTotal = calcualateEmissionDifferences(time2CongestionTotalFiltered1, time2CongestionTotalFiltered2);

		//		EmissionWriter eWriter = new EmissionWriter();
		BufferedWriter writer = IOUtils.getBufferedWriter(outPath + "movie.congestionOneTimePerLinkLocationSmoothed.txt");
		writer.append("xCentroid\tyCentroid\tCongestion\tTIME\n");

		Collection<Feature> features = new ArrayList<Feature>();

		for(double endOfTimeInterval : time2CongestionTotalFiltered1.keySet()){
			Map<Id, Double> congestionTotal = time2CongestionTotalFiltered1.get(endOfTimeInterval);
			//			String outFile = outPath + (int) endOfTimeInterval + ".emissionsTotalPerLinkLocation.txt";
			//			eWriter.writeLinkLocation2Emissions(listOfPollutants, deltaEmissionsTotal, network, outFile);

			int[][] noOfLinksInCell = new int[noOfXbins][noOfYbins];
			double[][] sumOfweightsForCell = new double[noOfXbins][noOfYbins];
			double[][] sumOfweightedValuesForCell = new double[noOfXbins][noOfYbins];

			for(Link link : network.getLinks().values()){
				Id linkId = link.getId();
				Coord linkCoord = link.getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();

				Integer xbin = mapXCoordToBin(xLink);
				Integer ybin = mapYCoordToBin(yLink);
				if ( xbin != null && ybin != null ){

					noOfLinksInCell[xbin][ybin] ++;

					for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
						for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
							Coord cellCentroid = findCellCentroid(xIndex, yIndex);
							double value = congestionTotal.get(linkId);
							// TODO: not distance between data points, but distance between
							// data point and cell centroid is used now; is the former to expensive?
							double weightOfLinkForCell = calculateWeightOfPersonForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
							sumOfweightsForCell[xIndex][yIndex] += weightOfLinkForCell;
							sumOfweightedValuesForCell[xIndex][yIndex] += weightOfLinkForCell * value;
						}
					}
				}
			}
			for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(noOfLinksInCell[xIndex][yIndex] > minimumNoOfLinksInCell){
					//	if(endOfTimeInterval <= 86400.){
							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / sumOfweightsForCell[xIndex][yIndex];
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
							String outString = cellCentroid.getX() + "\t" + cellCentroid.getY() + "\t" + averageValue + "\t" + dateTimeString + "\n";
							writer.append(outString);
						
							Point point = MGC.xy2Point(cellCentroid.getX(), cellCentroid.getY());
							try {
								Feature feature = this.featureType.create(new Object[] {
										point, dateTimeString, averageValue
								});
								features.add(feature);
							} catch (IllegalAttributeException e1) {
								throw new RuntimeException(e1);
							}
						//}
					}
				}
			}
		}
		writer.close();
		logger.info("Finished writing output to " + outPath + "movie.congestionOneTimePerLinkLocationSmoothed.txt");
		
		ShapeFileWriter.writeGeometries(features, outPath + "movie.congestionOneTimePerLinkLocationSmoothed.shp");
	}

	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2012-04-13 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = date + time;
		return dateTimeString;
	}

	private double calculateWeightOfPersonForCell(double x1, double y1, double x2, double y2) {
		double distance = Math.abs(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))); // TODO: need to check if distance > 0 ?!?
		return Math.exp((-distance * distance) / (1000. * 1000.)); // TODO: what is this normalization for?
	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin); // TODO: ???
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin); // TODO: ???
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
		if (yCoord <= yMin || yCoord >= yMax) return null; // yHome is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xHome is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Map<Double, Map<Id, Double>> setNonCalculatedCongestionAndFilter(Map<Double, Map<Id, Double>> time2CongestionTotal) {
		Map<Double, Map<Id,Double>> time2CongestionTotalFiltered = new HashMap<Double, Map<Id, Double>>();

		for(Double endOfTimeInterval : time2CongestionTotal.keySet()){
			Map<Id, Double> congestionTotal = time2CongestionTotal.get(endOfTimeInterval);
			Map<Id, Double> congestionTotalFiltered = new HashMap<Id, Double>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();

						if(congestionTotal.get(linkId) != null){
							double congestion = congestionTotal.get(linkId);
							congestionTotalFiltered.put(linkId, congestion);
						} else {
								// setting all congestion values for links that had no congestion on it to 0.0 
							congestionTotalFiltered.put(linkId, 0.0);
							}
						}
					}
				}					
			time2CongestionTotalFiltered.put(endOfTimeInterval, congestionTotalFiltered);
		}
		return time2CongestionTotalFiltered;
	}

	private void processCongestion(String eventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		this.congestionHandler = new CongestionPerLinkHandler(network, this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.congestionHandler );
		reader.readFile(eventsFile);
	}

	@SuppressWarnings("deprecation")
	private void initFeatures() {
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, null);
		AttributeType time = AttributeTypeFactory.newAttributeType(
				"Time", String.class);
		AttributeType co2Emissions = AttributeTypeFactory.newAttributeType(
				"deltaCO2", String.class);
		
		Exception ex;
		try {
			this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[]
			        {point, time, co2Emissions}, "EmissionPoint");
			return;
		} catch (FactoryRegistryException e0) {
			ex = e0;
		} catch (SchemaException e0) {
			ex = e0;
		}
		throw new RuntimeException(ex);
		
	}

	private void loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}

	private void defineListOfPollutants() {
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private Double getEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.getQSimConfigGroup().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}
	
	private static Integer getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingForLinkCongestion().run();
	}

}
