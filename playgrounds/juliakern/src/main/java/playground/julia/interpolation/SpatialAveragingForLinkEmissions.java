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
package playground.julia.interpolation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

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
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

//import playground.julia.interpolation.EmissionUtils;
import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
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
	private final static String runNumber2 = "983";
	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	
	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + runNumber2 + "." + lastIteration2 + ".emission.events.xml.gz";

	Network network;
	Set<Feature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub;

	Map<Double, Map<Id, Double>> time2CountsPerLink1;
	Map<Double, Map<Id, Double>> time2CountsPerLink2;

	//coordinates
	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	// define all relevant parameters
	final int noOfTimeBins = 15; //was 60 corresponds to 30 min
	final int noOfXbins = 160; //was 160
	final int noOfYbins = 120; //was 120
	final int minimumNoOfLinksInCell = 0;
	final double smoothingRadius_m = 500.; 
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean baseCaseOnly = true;
	final boolean calculateRelativeChange = false;


	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();
		FeatureType featureType = initFeatures();
		this.featuresInMunich = readShape(munichShapeFile);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionMapToAnalyze;

		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		time2CountsPerLink1 = this.warmHandler.getTime2linkIdLeaveCount();

		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal1 = sumUpEmissionsPerTimeInterval(time2warmEmissionsTotal1, time2coldEmissionsTotal1);
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled1 = setNonCalculatedEmissions(time2EmissionsTotal1);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1 = filterLinks(time2EmissionsTotalFilled1);
		time2CountsPerLink1 = setNonCalculatedCountsAndFilter(time2CountsPerLink1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		if(baseCaseOnly){
			time2EmissionMapToAnalyze = time2EmissionsTotalFilledAndFiltered1;
			outPathStub = runDirectory1 + runNumber1 + "." + lastIteration1;
		} else {
			processEmissions(emissionFile2);
			Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal2 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal2 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
			time2CountsPerLink2 = this.warmHandler.getTime2linkIdLeaveCount();
			
			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal2 = sumUpEmissionsPerTimeInterval(time2warmEmissionsTotal2, time2coldEmissionsTotal2);
			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled2 = setNonCalculatedEmissions(time2EmissionsTotal1);
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered2 = filterLinks(time2EmissionsTotalFilled2);
			time2CountsPerLink2 = setNonCalculatedCountsAndFilter(time2CountsPerLink2);

			if(calculateRelativeChange){
				time2EmissionMapToAnalyze = calcualateRelativeEmissionDifferences(time2EmissionsTotalFilledAndFiltered1, time2EmissionsTotalFilledAndFiltered2);
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".relativeDelta";
			} else {
				time2EmissionMapToAnalyze = calcualateAbsoluteEmissionDifferences(time2EmissionsTotalFilledAndFiltered1, time2EmissionsTotalFilledAndFiltered2);;
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
			}
		}

// 		EmissionWriter eWriter = new EmissionWriter();
//		BufferedWriter writer = IOUtils.getBufferedWriter(outPathStub + "." + pollutant + ".smoothed.txt");
//		writer.append("xCentroid\tyCentroid\t" + pollutant + "\tTIME\n");

		Collection<Feature> features = new ArrayList<Feature>();

		double[][] sumOfweightedValuesForCell = new double[noOfXbins][noOfYbins];
		
		for(double endOfTimeInterval : time2EmissionMapToAnalyze.keySet()){
			Map<Id, Map<String, Double>> emissionMapToAnalyze = time2EmissionMapToAnalyze.get(endOfTimeInterval);
			// String outFile = outPathStub + (int) endOfTimeInterval + ".txt";
			// eWriter.writeLinkLocation2Emissions(listOfPollutants, deltaEmissionsTotal, network, outFile);

			int[][] noOfLinksInCell = new int[noOfXbins][noOfYbins];
			double[][] sumOfweightsForCell = new double[noOfXbins][noOfYbins];
			sumOfweightedValuesForCell = new double[noOfXbins][noOfYbins];

			//calculate weighted values and weights for every link, every bin
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
							double value = emissionMapToAnalyze.get(linkId).get(pollutant2analyze);
							//exponential function (distance)
							double weightOfLinkForCell = calculateWeightOfPersonForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
							sumOfweightsForCell[xIndex][yIndex] += weightOfLinkForCell;
							sumOfweightedValuesForCell[xIndex][yIndex] += weightOfLinkForCell * value;
						}
					}
				}
			}
			
			//scaling
			for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(noOfLinksInCell[xIndex][yIndex] >= minimumNoOfLinksInCell){
						if(isInMunichShape(cellCentroid)){
							if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...

								// double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / sumOfweightsForCell[xIndex][yIndex]; // average of emissions per cell

								// double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m); // sum of emissions per cell normalized to emissions per m²

								double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) * 1000. * 1000.; // sum of emissions per cell normalized to emissions per km²

								String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
								// String outString = cellCentroid.getX() + "\t" + cellCentroid.getY() + "\t" + averageValue + "\t" + dateTimeString + "\n";
								// writer.append(outString);

								Point point = MGC.xy2Point(cellCentroid.getX(), cellCentroid.getY());
								try {
									Feature feature = featureType.create(new Object[] {
											point, dateTimeString, averageValue
									});
									features.add(feature);
								} catch (IllegalAttributeException e1) {
									throw new RuntimeException(e1);
								}
							}
						}
					}
				}
			}	

			String outputPathForR = new String(outPathStub + ".Routput"+pollutant2analyze.toString()+"."+endOfTimeInterval+".txt");
			writeRoutput(sumOfweightedValuesForCell, outputPathForR);
			
		}

		//TODO momentan fuer jedes Zeitintervall, passende Ifabfrage o ae
//		String outputPathForR = new String(outPathStub + ".Routput.txt");
//		writeRoutput(sumOfweightedValuesForCell, outputPathForR);
//		writer.close();
//		logger.info("Finished writing output to " + outPathStub + "." + pollutant2analyze + ".smoothed.txt");

		ShapeFileWriter.writeGeometries(features, outPathStub +  "." + pollutant2analyze + "perKmSquare.movie.emissionsPerLinkSmoothed.shp");
		logger.info("Finished writing output to " + outPathStub +  "." + pollutant2analyze + ".perKmSquare.movie.emissionsPerLinkSmoothed.shp");
//		ShapeFileWriter.writeGeometries(features, outPathStub +  "." + pollutant2analyze + ".movie.emissionsPerLinkSmoothed.shp");
//		logger.info("Finished writing output to " + outPathStub +  "." + pollutant2analyze + ".movie.emissionsPerLinkSmoothed.shp");


		
	}

	private void writeRoutput(double[][] sumOfweightedValuesForCell,
			String outputPathForR) {
		
		try {

			
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString="\t";
			
			//step size between coordinates
			double xDist=(xMax-xMin)/noOfXbins;
			double yDist=(yMax-yMin)/noOfYbins;
			
			//first line containing coordinates
			for(int i=0; i<sumOfweightedValuesForCell.length;i++){
				valueString+=Double.toString(yMin+i*yDist)+"\t";
			}
			buffW.write(valueString);
			buffW.newLine();
			valueString="";
			
			//array[160][120]
			//outputdatei mit 160 zeilen
			for(int i = 0; i< sumOfweightedValuesForCell[0].length; i++){
				//coordinates as header
				valueString+=Double.toString(xMin+i*xDist)+"\t";
				
				//table contents
				for(int j=0; j<sumOfweightedValuesForCell.length; j++){ 
					try {
						valueString+=Double.toString(sumOfweightedValuesForCell[j][i])+"\t"; 
					} catch (Exception e) {
						//if the array was not initialized at [i][j] use 0.0
						valueString+="0.0"+"\t";
						//alternative, TODO check if R handles this correctly
						//valueString+="NA"+"\t";
					}
				}
				//write line + line break
				buffW.write(valueString);
				buffW.newLine();
				valueString="";
			}
		buffW.close();	
		} catch (IOException e) {
			logger.warn("Failed to write output file for R.");
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
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
		double distance = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
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
					
					
					double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
					
					double emissionsPerVehicleKmBefore; 
					double countBefore = this.time2CountsPerLink1.get(endOfTimeInterval).get(linkId);
					if(countBefore != 0.0){
						emissionsPerVehicleKmBefore = emissionsBefore / (countBefore * linkLength_km);
					} else {
						emissionsPerVehicleKmBefore = 0.0;
					}
					
					double emissionsPerVehicleKmAfter;
					double countAfter = this.time2CountsPerLink2.get(endOfTimeInterval).get(linkId);
					if(countAfter != 0.0){
						emissionsPerVehicleKmAfter = emissionsAfter / (countAfter * linkLength_km);
					} else {
						emissionsPerVehicleKmAfter = 0.0;
					}

					double emissionsPerVehicleKmDifference = emissionsPerVehicleKmAfter - emissionsPerVehicleKmBefore;
//					double emissionsPerVehicleKmDifference = (emissionsPerVehicleKmAfter - emissionsPerVehicleKmBefore) / emissionsPerVehicleKmBefore;
					emissionDifferenceMap.put(pollutant, emissionsPerVehicleKmDifference);
					
					
//					Double emissionDifference = emissionsAfter - emissionsBefore;
//					emissionDifferenceMap.put(pollutant, emissionDifference);
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
					if (emissionsBefore == 0.0){ // cannot calculate relative change if "before" value is 0.0 ...
						emissionsBefore = 1.0;   // ...therefore setting "before" value to a small value.
					} else {
						// do nothing
					}
					double emissionDifferenceRatio = (emissionsAfter - emissionsBefore) / emissionsBefore;
					emissionDifferenceMap.put(pollutant, emissionDifferenceRatio);
				}
				relativeDelta.put(linkId, emissionDifferenceMap);
			}
			time2RelativeDelta.put(endOfTimeInterval, relativeDelta);
		}
		return time2RelativeDelta;
	}

	private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
		
		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	private Map<Double, Map<Id, Map<String, Double>>> filterLinks(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id, Map<String, Double>> emissionsTotalFiltered = new HashMap<Id, Map<String, Double>>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						emissionsTotalFiltered.put(link.getId(), emissionsTotal.get(link.getId()));
					}
				}					
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id, Double>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id, Double>> time2CountsPerLink) {
		Map<Double, Map<Id, Double>> time2CountsTotalFiltered = new HashMap<Double, Map<Id,Double>>();

		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id, Double> linkId2CountFiltered = new HashMap<Id, Double>();
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
						if(linkId2Count.get(linkId) == null){
							linkId2CountFiltered.put(linkId, 0.);
						} else {
							linkId2CountFiltered = linkId2Count;
						}
					}
				}
			}
			time2CountsTotalFiltered.put(endOfTimeInterval, linkId2CountFiltered);
		}
		return time2CountsTotalFiltered;
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
					SortedMap<String, Double> warmEmissionsOfLink = emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
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

	private static Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInMunich;
		featuresInMunich = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInMunich;
	}

	@SuppressWarnings("deprecation")
	private FeatureType initFeatures() {
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, this.targetCRS);
		AttributeType time = AttributeTypeFactory.newAttributeType(
				"Time", String.class);
		AttributeType deltaEmissions = AttributeTypeFactory.newAttributeType(
				"Emissions", Double.class);

		Exception ex;
		try {
			FeatureType type = FeatureTypeFactory.newFeatureType(new AttributeType[]
					{point, time, deltaEmissions}, "EmissionPoint");
			return type;
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
		new SpatialAveragingForLinkEmissions().run();
	}
}