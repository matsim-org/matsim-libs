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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.cupum.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.cupum.EmissionsPerLinkWarmEventHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

/**
 * @author benjamin
 *
 */
public class SpatialAveragingForLinkEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkEmissions.class);

	private final static String runNumber1 = "981";
	private final static String runNumber2 = "982";
	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String emissionFile1 = runDirectory1 + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
	private final String emissionFile2 = runDirectory2 + runNumber2 + "." + lastIteration2 + ".emission.events.xml.gz";

	//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
	//	private final String configFile1 = runDirectory1 + "output_config.xml.gz";

	Network network;
	FeatureType featureType;
	Set<Feature> featuresInMunich;

	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;

	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static int noOfTimeBins = 1;
	double simulationEndTime;

	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	static int noOfXbins = 160;
	static int noOfYbins = 120;
	static int minimumNoOfLinksInCell = 0;
	private final double smoothingRadius_m = 500.; 
	static String pollutant = WarmPollutant.NO2.toString();
	static boolean baseCaseOnly = false;
	static boolean calculateRelativeChange = true;

	// OUTPUT
	private String outPathStub;

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		defineListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();
		initFeatures();
		this.featuresInMunich = readShape(munichShapeFile);

		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();

		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1 = sumUpEmissions(time2warmEmissionsTotal1, time2coldEmissionsTotal1);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered1 = setNonCalculatedEmissionsAndFilter(time2EmissionsTotal1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		processEmissions(emissionFile2);
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal2 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal2 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();

		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal2 = sumUpEmissions(time2warmEmissionsTotal2, time2coldEmissionsTotal2);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered2 = setNonCalculatedEmissionsAndFilter(time2EmissionsTotal2);

		Map<Double, Map<Id, Map<String, Double>>> time2EmissionMapToAnalyze;

		if(baseCaseOnly){
			time2EmissionMapToAnalyze = time2EmissionsTotalFiltered1;
			outPathStub = runDirectory1 + runNumber1 + "." + lastIteration1;
		} else {
			if(calculateRelativeChange){
				time2EmissionMapToAnalyze = calcualateRelativeEmissionDifferences(time2EmissionsTotalFiltered1, time2EmissionsTotalFiltered2);
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".relativeDelta";
			} else {
				time2EmissionMapToAnalyze = calcualateAbsoluteEmissionDifferences(time2EmissionsTotalFiltered1, time2EmissionsTotalFiltered2);;
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
			}
		}

		// EmissionWriter eWriter = new EmissionWriter();
		BufferedWriter writer = IOUtils.getBufferedWriter(outPathStub + "." + pollutant + ".smoothed.txt");
		writer.append("xCentroid\tyCentroid\t" + pollutant + "\tTIME\n");

		Collection<Feature> features = new ArrayList<Feature>();

		for(double endOfTimeInterval : time2EmissionMapToAnalyze.keySet()){
			Map<Id, Map<String, Double>> emissionMapToAnalyze = time2EmissionMapToAnalyze.get(endOfTimeInterval);
			// String outFile = outPathStub + (int) endOfTimeInterval + ".txt";
			// eWriter.writeLinkLocation2Emissions(listOfPollutants, deltaEmissionsTotal, network, outFile);

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
							double value = emissionMapToAnalyze.get(linkId).get(pollutant);
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
					if(noOfLinksInCell[xIndex][yIndex] >= minimumNoOfLinksInCell){
						if(isInMunichShape(cellCentroid)){
// 							if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
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
//							}
						}
					}
				}
			}
		}
		writer.close();
		logger.info("Finished writing output to " + outPathStub + "." + pollutant + ".smoothed.txt");

		ShapeFileWriter.writeGeometries(features, outPathStub +  "." + pollutant + ".movie.emissionsPerLinkSmoothed.shp");
		logger.info("Finished writing output to " + outPathStub +  "." + pollutant + ".movie.emissionsPerLinkSmoothed.shp");
	}

	private boolean isInMunichShape(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(Feature feature : this.featuresInMunich){
			if(feature.getDefaultGeometry().contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}

	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2012-04-13 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = date + time;
		return dateTimeString;
	}

	private double calculateWeightOfPersonForCell(double x1, double y1, double x2, double y2) {
		double distance = Math.abs(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
		return Math.exp((-distance * distance) / (smoothingRadius_m * smoothingRadius_m));
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
		if (yCoord <= yMin || yCoord >= yMax) return null; // yHome is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xHome is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Map<Double, Map<Id, Map<String, Double>>> calcualateAbsoluteEmissionDifferences(
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1,
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal2) {

		Map<Double, Map<Id, Map<String, Double>>> time2AbsoluteDelta = new HashMap<Double, Map<Id, Map<String, Double>>>();
		for(Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2EmissionsTotal1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> linkId2Emissions = entry0.getValue();
			Map<Id, Map<String, Double>> absoluteDelta = new HashMap<Id, Map<String, Double>>();

			for(Entry<Id, Map<String, Double>> entry1 : linkId2Emissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> emissionDifferenceMap = new HashMap<String, Double>();
				for(String pollutant : entry1.getValue().keySet()){
					Double emissionsBefore = entry1.getValue().get(pollutant);
					Double emissionsAfter = time2EmissionsTotal2.get(endOfTimeInterval).get(linkId).get(pollutant);
					Double emissionDifference = emissionsAfter - emissionsBefore;
					emissionDifferenceMap.put(pollutant, emissionDifference);
				}
				absoluteDelta.put(linkId, emissionDifferenceMap);
			}
			time2AbsoluteDelta.put(endOfTimeInterval, absoluteDelta);
		}
		return time2AbsoluteDelta;
	}

	private Map<Double, Map<Id, Map<String, Double>>> calcualateRelativeEmissionDifferences(
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1,
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal2) {

		Map<Double, Map<Id, Map<String, Double>>> time2RelativeDelta = new HashMap<Double, Map<Id, Map<String, Double>>>();
		for(Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2EmissionsTotal1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> linkId2emissions = entry0.getValue();
			Map<Id, Map<String, Double>> relativeDelta = new HashMap<Id, Map<String, Double>>();

			for(Entry<Id, Map<String, Double>> entry1 : linkId2emissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> emissionDifferenceMap = new HashMap<String, Double>();
				for(String pollutant : entry1.getValue().keySet()){
					double emissionsBefore = entry1.getValue().get(pollutant);
					double emissionsAfter = time2EmissionsTotal2.get(endOfTimeInterval).get(linkId).get(pollutant);
					if (emissionsBefore != 0.0){ // cannot calculate relative change if "before" value is 0.0 ...
						double emissionDifferenceRatio = (emissionsAfter - emissionsBefore) / emissionsBefore;
						emissionDifferenceMap.put(pollutant, emissionDifferenceRatio);
					} else { // ... therefore setting ratio to 100%
						emissionDifferenceMap.put(pollutant, 1.0);
					}
				}
				relativeDelta.put(linkId, emissionDifferenceMap);
			}
			time2RelativeDelta.put(endOfTimeInterval, relativeDelta);
		}
		return time2RelativeDelta;
	}

	private Map<Double, Map<Id, Map<String, Double>>> setNonCalculatedEmissionsAndFilter(Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, Map<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id, Map<String, Double>> emissionsTotalFiltered = new HashMap<Id, Map<String, Double>>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
						Map<String, Double> emissionType2Value = new HashMap<String, Double>();

						if(emissionsTotal.get(linkId) != null){
							for(String pollutant : listOfPollutants){
								emissionType2Value = emissionsTotal.get(linkId);
								if(emissionType2Value.get(pollutant) != null){
									Double originalValue = emissionsTotal.get(linkId).get(pollutant);
									emissionType2Value.put(pollutant, originalValue);
								} else {
									// setting some emission types that are not available for the link to 0.0
									emissionType2Value.put(pollutant, 0.0);
								}
							}
						} else {
							for(String pollutant : listOfPollutants){
								// setting all emission types for links that had no emissions on it to 0.0 
								emissionType2Value.put(pollutant, 0.0);
							}
						}
						emissionsTotalFiltered.put(linkId, emissionType2Value);
					}
				}					
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id, Map<String, Double>>> sumUpEmissions(
			Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal) {

		Map<Double, Map<Id, Map<String, Double>>> time2totalEmissions = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2warmEmissionsTotal.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> warmEmissions = entry0.getValue();
			Map<Id, Map<String, Double>> totalEmissions = new HashMap<Id, Map<String, Double>>();

			for(Entry<Id, Map<String, Double>> entry1 : warmEmissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> linkSpecificWarmEmissions = entry1.getValue();

				if(time2coldEmissionsTotal.get(endOfTimeInterval) != null){
					Map<Id, Map<String, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);

					if(coldEmissions.get(linkId) != null){
						Map<String, Double> linkSpecificSumOfEmissions = new HashMap<String, Double>();
						Map<String, Double> linkSpecificColdEmissions = coldEmissions.get(linkId);
						Double individualValue;

						for(String pollutant : listOfPollutants){
							if(linkSpecificWarmEmissions.containsKey(pollutant)){
								if(linkSpecificColdEmissions.containsKey(pollutant)){
									individualValue = linkSpecificWarmEmissions.get(pollutant) + linkSpecificColdEmissions.get(pollutant);
								} else{
									individualValue = linkSpecificWarmEmissions.get(pollutant);
								}
							} else{
								individualValue = linkSpecificColdEmissions.get(pollutant);
							}
							linkSpecificSumOfEmissions.put(pollutant, individualValue);
						}
						totalEmissions.put(linkId, linkSpecificSumOfEmissions);
					} else{
						totalEmissions.put(linkId, linkSpecificWarmEmissions);
					}
				} else {
					totalEmissions.put(linkId, linkSpecificWarmEmissions);
				}
				time2totalEmissions.put(endOfTimeInterval, totalEmissions);
			}
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

	private static Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInMunich;
		featuresInMunich = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInMunich;
	}

	@SuppressWarnings("deprecation")
	private void initFeatures() {
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, this.targetCRS);
		AttributeType time = AttributeTypeFactory.newAttributeType(
				"Time", String.class);
		AttributeType deltaEmissions = AttributeTypeFactory.newAttributeType(
				"deltaEmiss", Double.class);

		Exception ex;
		try {
			this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[]
					{point, time, deltaEmissions}, "EmissionPoint");
			return;
		} catch (FactoryRegistryException e0) {
			ex = e0;
		} catch (SchemaException e0) {
			ex = e0;
		}
		throw new RuntimeException(ex);
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
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

	public static void main(String[] args) throws IOException{
		new SpatialAveragingForLinkEmissions().run();
	}

	private static Integer getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
}