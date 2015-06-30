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
package playground.agarwalamit.analysis.spatial.old;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.old.SpatialAveragingUtilsExtended;


/**
 * @author benjamin
 *
 */
public class SpatialAveragingDemandEmissionsBK {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDemandEmissionsBK.class);

	final double scalingFactor = 100.;
	private final static String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	private final static String runBAU = runDir+"/baseCaseCtd";
	//	private final static String outDir = runDir+runBAU+"NoCheck4VisBoundary";
	private final static String runNumber = runDir+"/ci";
	private final String netFile1 = runBAU+"/output_network.xml.gz";//"../../siouxFallsJava/output/run4/output_network.xml.gz" ;//runDirectory1 + runNumber1 + ".output_network.xml.gz";

	private final String munichShapeFile = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	private static String configFile1 =runBAU+"/output_config.xml";; //"../../siouxFallsJava/output/run4/output_config.xml";//runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final String emissionFileBAU = runBAU+"/ITERS/it.1500/1500.emission.events.xml.gz";
	private final String emissionFile2 = runNumber+"/ITERS/it.1500/1500.emission.events.xml.gz";

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");//EPSG:3459 for siouxFalls
	final double xMin = 4452550.25;
	final double xMax = 4479483.33;
	final double yMin = 5324955.00;
	final double yMax = 5345696.81;
	final int noOfXbins = 160;
	final int noOfYbins = 120;

	final int noOfTimeBins = 1;

	//========
	final double smoothingRadius_m = 500.;
	final boolean line = true;
	//========
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBAU = true;

	SpatialAveragingUtils sau;
	LocationFilter lf;
	double simulationEndTime;
	SortedSet<String> listOfPollutants;
	Network network;

	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	String outPathStub;
	SpatialAveragingUtilsExtended extended ;
	private void run() {
		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS);
		this.lf = new LocationFilter();
		extended = new SpatialAveragingUtilsExtended(smoothingRadius_m);
		this.simulationEndTime = getEndTime(configFile1);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		

		processEmissions(emissionFileBAU);
		Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval(); 
		Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLink1 = this.warmHandler.getTime2linkIdLeaveCount();

		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal1 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal1, time2ColdEmissionsTotal1);
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled1 = setNonCalculatedEmissions(time2EmissionsTotal1);
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1 = filterEmissionLinks(time2EmissionsTotalFilled1);
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered1 = setNonCalculatedCountsAndFilter(time2CountsPerLink1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		Map<Double, double[][]> time2WeightedEmissions1 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered1);
		Map<Double, double[][]> time2NormalizedWeightedEmissions1 = normalizeAllArrays(time2WeightedEmissions1);

		Map<Double, double[][]> time2WeightedDemand1 = fillWeightedDemandValues(time2CountsPerLinkFilledAndFiltered1);
		Map<Double, double[][]> time2NormalizedWeightedDemand1 = normalizeAllArrays(time2WeightedDemand1);

		/* Sum over weighted values for cell does not need to be normalized, since normalization canceles out.
		 * Result is an average value for cell*/
		Map<Double, double[][]> time2SpecificEmissions1 = calculateSpecificEmissionsPerBin(time2WeightedEmissions1, time2WeightedDemand1);
		//		Map<Double, double[][]> time2SpecificEmissions1 = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions1, time2NormalizedWeightedDemand1);

		if(line){
			outPathStub = runDir+"/analysis/spatialPlots/rEmissionLine"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
		} else {
			outPathStub = runDir+"/analysis/spatialPlots/rEmissionPoint"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
		}

		for(double endOfTimeInterval: time2WeightedEmissions1.keySet()){
			this.sau.writeRoutput(time2NormalizedWeightedEmissions1.get(endOfTimeInterval), outPathStub +  pollutant2analyze.toString() + ".g." + endOfTimeInterval + ".txt");
			this.sau.writeRoutput(time2NormalizedWeightedDemand1.get(endOfTimeInterval), outPathStub + ".vkm." + endOfTimeInterval + ".txt");
			this.sau.writeRoutput(time2SpecificEmissions1.get(endOfTimeInterval), outPathStub+ pollutant2analyze + ".gPerVkm." + endOfTimeInterval + ".txt");

			//			this.sau.writeGISoutput(time2WeightedEmissions1, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
			//			this.sau.writeGISoutput(time2WeightedDemand1, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
			//			this.sau.writeGISoutput(time2SpecificEmissions, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
		}

		if(compareToBAU){
			processEmissions(emissionFile2);
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal2 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal2 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id<Link>, Double>> time2CountsPerLink2 = this.warmHandler.getTime2linkIdLeaveCount();

			Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal2 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal2, time2ColdEmissionsTotal2);
			Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled2 = setNonCalculatedEmissions(time2EmissionsTotal2);
			Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered2 = filterEmissionLinks(time2EmissionsTotalFilled2);
			Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered2 = setNonCalculatedCountsAndFilter(time2CountsPerLink2);

			Map<Double, double[][]> time2WeightedEmissions2 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered2);
			Map<Double, double[][]> time2NormalizedWeightedEmissions2 = normalizeAllArrays(time2WeightedEmissions2);

			Map<Double, double[][]> time2WeightedDemand2 = fillWeightedDemandValues(time2CountsPerLinkFilledAndFiltered2);
			Map<Double, double[][]> time2NormalizedWeightedDemand2 = normalizeAllArrays(time2WeightedDemand2);

			/* Sum over weighted values for cell does not need to be normalized, since normalization canceles out.
			 * Result is an average value for cell*/
			Map<Double, double[][]> time2SpecificEmissions2 = calculateSpecificEmissionsPerBin(time2WeightedEmissions2, time2WeightedDemand2);
			////			Map<Double, double[][]> time2SpecificEmissions2 = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions2, time2NormalizedWeightedDemand2);
			//			
			/* Sum over weighted values for cell is normalized to "per sqkm" (dependent on calcluateWeightOfLinkForCell)
			 * Values NEED to be additive (e.g. vkm, g, counts, or AVERAGE g/vkm!)
			 * Make sure coordinate system is metric */
			Map<Double, double[][]> time2AbsoluteEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedEmissions1, time2NormalizedWeightedEmissions2);
			Map<Double, double[][]> time2AbsoluteDemandDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedDemand1, time2NormalizedWeightedDemand2);
			Map<Double, double[][]> time2SpecificEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2SpecificEmissions1, time2SpecificEmissions2);

			if(line){
				outPathStub = runDir+"/analysis/spatialPlots/rEmissionWRTBAULine"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
			} else {
				outPathStub = runDir+"/analysis/spatialPlots/rEmissionWRTBAUPoint"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
			}
			//			outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";

			for(double endOfTimeInterval : time2AbsoluteDemandDifferences.keySet()){
				this.sau.writeRoutput(time2AbsoluteEmissionDifferences.get(endOfTimeInterval), outPathStub +  pollutant2analyze + ".g." + endOfTimeInterval + ".txt");
				this.sau.writeRoutput(time2AbsoluteDemandDifferences.get(endOfTimeInterval),	outPathStub + ".vkm." + endOfTimeInterval + ".txt");
				this.sau.writeRoutput(time2SpecificEmissionDifferences.get(endOfTimeInterval), outPathStub  + pollutant2analyze.toString() + ".gPerVkm." + endOfTimeInterval + ".txt");
				//
				////				this.sau.writeGISoutput(time2AbsoluteEmissionDifferences, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
				////				this.sau.writeGISoutput(time2AbsoluteDemandDifferences, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
				////				this.sau.writeGISoutput(time2SpecificEmissionDifferences, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
			}	
		}
	}

	private Map<Double, double[][]> fillWeightedEmissionValues(Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered)  {
		Map<Double, double[][]> time2weightedEmissions = new HashMap<Double, double[][]>();
		double sumOfAllWeights =0;
		for(Double endOfTimeInterval : time2EmissionsTotalFilledAndFiltered.keySet()){
			double[][]weightedEmissions = new double[noOfXbins][noOfYbins];

			for(Id<Link> linkId : time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();

				double value = time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).get(linkId).get(this.pollutant2analyze);
				double scaledValue = this.scalingFactor * value;

				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				// TODO: maybe calculate the following once and look it up here?

				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell;

						if(line){
							weightOfLinkForCell = extended.calculateWeightOfLineForCellV2(fromNodeCoord,toNodeCoord ,cellCentroid.getX(), cellCentroid.getY());
						} else{
							weightOfLinkForCell	= 	this.sau.calculateWeightOfPointForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						}
						weightedEmissions[xIndex][yIndex] += weightOfLinkForCell * scaledValue;	
					}
				}
			}
			time2weightedEmissions.put(endOfTimeInterval, weightedEmissions);
		}
		return time2weightedEmissions;
	}

	private Map<Double, double[][]> fillWeightedDemandValues(Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered)  {
		Map<Double, double[][]> time2weightedDemand = new HashMap<Double, double[][]>();

		for(Double endOfTimeInterval : time2CountsPerLinkFilledAndFiltered.keySet()){
			double[][]weightedDemand = new double[noOfXbins][noOfYbins];

			for(Id<Link> linkId : time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;

				double count = time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).get(linkId);
				double vkm = count * linkLength_km;
				double scaledVkm = this.scalingFactor * vkm;
				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell;
						if(line){
							weightOfLinkForCell = extended.calculateWeightOfLineForCellV2(fromNodeCoord,toNodeCoord ,cellCentroid.getX(), cellCentroid.getY());
						} else{
							weightOfLinkForCell	= 	this.sau.calculateWeightOfPointForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						}
						weightedDemand[xIndex][yIndex] += weightOfLinkForCell * scaledVkm;					
					}
				}
			}
			time2weightedDemand.put(endOfTimeInterval, weightedDemand);
		}
		return time2weightedDemand;
	}

	private Map<Double, double[][]> calculateSpecificEmissionsPerBin(
			Map<Double, double[][]> time2weightedEmissions,
			Map<Double, double[][]> time2weightedDemand) {

		Map<Double, double[][]> time2specificEmissions = new HashMap<Double, double[][]>();
		for( Double endOfTimeInterval : time2weightedEmissions.keySet()){
			double [][] specificEmissions = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					specificEmissions[xIndex][yIndex] = time2weightedEmissions.get(endOfTimeInterval)[xIndex][yIndex] / time2weightedDemand.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2specificEmissions.put(endOfTimeInterval, specificEmissions);
		}
		return time2specificEmissions;
	}

	private Map<Double, double[][]> calculateAbsoluteDifferencesPerBin(
			Map<Double, double[][]> time2weightedValues1,
			Map<Double, double[][]> time2weightedValues2){

		Map<Double, double[][]> time2absoluteDifferences = new HashMap<Double, double[][]>();
		for(Double endOfTimeInterval : time2weightedValues1.keySet()){
			double [][] absoluteDifferences = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					absoluteDifferences[xIndex][yIndex] = time2weightedValues2.get(endOfTimeInterval)[xIndex][yIndex] - time2weightedValues1.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2absoluteDifferences.put(endOfTimeInterval, absoluteDifferences);
		}		
		return time2absoluteDifferences;
	}

	private Map<Double, double[][]> normalizeAllArrays(
			Map<Double, double[][]> time2Array) {

		Map<Double, double[][]> time2NormalizedArray = new HashMap<Double, double[][]>();
		for(Double endOfTimeInterval : time2Array.keySet()){
			double[][] normalizedArray = this.sau.normalizeArray(time2Array.get(endOfTimeInterval));
			time2NormalizedArray.put(endOfTimeInterval, normalizedArray);
		}

		return time2NormalizedArray;
	}

	private Map<Double, Map<Id<Link>, Double>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id<Link>, Double>> time2CountsPerLink) {
		Map<Double, Map<Id<Link>, Double>> time2CountsTotalFiltered = new HashMap<Double, Map<Id<Link>,Double>>();

		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id<Link>, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id<Link>, Double> linkId2CountFiltered = new HashMap<Id<Link>, Double>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id<Link> linkId = link.getId();

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

	private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id<Link>, SortedMap<String, Double>>>();

		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id<Link>, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	private Map<Double, Map<Id<Link>, Map<String, Double>>> filterEmissionLinks(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<Double, Map<Id<Link>, Map<String, Double>>>();

		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id<Link>, SortedMap<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id<Link>, Map<String, Double>> emissionsTotalFiltered = new HashMap<Id<Link>, Map<String, Double>>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id<Link> linkId = link.getId();
					emissionsTotalFiltered.put(linkId, emissionsTotal.get(linkId));
				}
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {

		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2totalEmissions = new HashMap<Double, Map<Id<Link>, SortedMap<String, Double>>>();

		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);

			Map<Id<Link>, SortedMap<String, Double>> totalEmissions = new HashMap<Id<Link>, SortedMap<String, Double>>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id<Link> id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = this.emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id<Link>, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
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
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public static void main(String[] args) throws IOException, MathException{
		new SpatialAveragingDemandEmissionsBK().run();
	}
}