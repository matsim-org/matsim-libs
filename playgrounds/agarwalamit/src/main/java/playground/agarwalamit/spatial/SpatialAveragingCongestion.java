/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.spatial;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.agarwalamit.siouxFalls.congestionAnalyzer.CongestionPerLinkHandler;
import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.benjamin.scenarios.munich.analysis.nectar.SpatialAveragingUtilsExtended;
import playground.vsp.emissions.events.EmissionEventsReader;

/**
 * @author amit after Benjamin
 */
public class SpatialAveragingCongestion {
	private final Logger logger = Logger.getLogger(SpatialAveragingCongestion.class);
	private CongestionPerLinkHandler congestionPerLinkHandler;
	final double scalingFactor = 1.;
	private final static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/output/run";
	private final static String outDir = "../../siouxFallsJava/clusterOutputSelected/run";
	private final static String runBAU = "8/";
//	private final static String runNumber = "1/";
	private final String netFile1 = runDir+runBAU+"/output_network.xml.gz";

	private final String siouxFallsShapeFile = "../../siouxFallsJava/input/networkShape/siouxFallsAreaPolygon.shp";

	private static String configFileBAU =runDir+runBAU+"/output_config.xml.gz"; 
	private final String eventsFileBAU = runDir+runBAU+"/ITERS/it.100/100.events.xml.gz";
//	private final String eventsFile2 = runDir+runNumber+"/ITERS/it.100/100.events.xml.gz";
	private final String emissionFileBAU = runDir+runBAU+"/ITERS/it.100/100.emission.events.xml.gz";
	
	
	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:3459");
	final double xMin =	673506.73;
	final double xMax = 689857.13;
	final double yMin = 4814378.34;
	final double yMax = 4857392.75;
	final int noOfXbins = 160;
	final int noOfYbins = 120;

	final int noOfTimeBins = 1;

	//========
	final double smoothingRadius_m = 200.;
	final boolean line = true;
	//========
	final boolean compareToBAU = false;

	SpatialAveragingUtils sau;
	LocationFilter lf;
	double simulationEndTime;
	Scenario scenario;
	Network network;

	String outPathStub;
	SpatialAveragingUtilsExtended extended ;
	EmissionsPerLinkWarmEventHandler warmHandler;

	public  void run(){
		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, siouxFallsShapeFile, targetCRS);
		this.lf = new LocationFilter();
		extended = new SpatialAveragingUtilsExtended(smoothingRadius_m);
		this.simulationEndTime = getEndTime(configFileBAU);

		scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();
		processCongestions(eventsFileBAU);
		processEmissions(emissionFileBAU);

		Map<Double, Map<Id, Double>> time2LinkDelays = this.congestionPerLinkHandler.getDelayPerLinkAndTimeInterval();
		//to keep the same demand in emission and congestion pricing, more logical is using demand from warm emission analysis module.
		Map<Double, Map<Id, Double>> time2CountsPerLink = this.warmHandler.getTime2linkIdLeaveCount(); 

		Map<Double, double[][]> time2WeightedCongestion = fillWeightedCongestionValues(filterLinks(time2LinkDelays));
		Map<Double, double[][]> time2WeightedDemand = fillWeightedDemandValues(filterLinks(time2CountsPerLink));

		this.congestionPerLinkHandler.reset(0);
		this.warmHandler.reset(0);
		
		Map<Double, double[][]> normalizedTime2WeightedCongestion = normalizeAllArrays(time2WeightedCongestion);
		Map<Double, double[][]> normalizedTime2WeightedDemand = normalizeAllArrays(time2WeightedDemand);

		/* Sum over weighted values for cell does not need to be normalized, since normalization cancels out.
		 * Result is an average value for cell*/
		Map<Double, double[][]> time2SpecificDelays = calculateSpecificDelaysPerBin(time2WeightedCongestion, time2WeightedDemand);

		if(line){
			outPathStub = runDir+runBAU+"/analysis/r/rCongestionLine"+smoothingRadius_m;
		} else {
			outPathStub = runDir+runBAU+"/analysis/r/rCongestionPoint"+smoothingRadius_m;
		}

		for(double endOfTimeInterval: time2WeightedCongestion.keySet()){
			this.sau.writeRoutput(normalizedTime2WeightedCongestion.get(endOfTimeInterval), outPathStub + ".sec"+endOfTimeInterval + ".txt");
			this.sau.writeRoutput(normalizedTime2WeightedDemand.get(endOfTimeInterval), outPathStub + ".vkm." + endOfTimeInterval + ".txt");
			this.sau.writeRoutput(time2SpecificDelays.get(endOfTimeInterval), outPathStub+ ".secVkm." + endOfTimeInterval + ".txt");

			//			this.sau.writeGISoutput(time2WeightedEmissions1, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
			//			this.sau.writeGISoutput(time2WeightedDemand1, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
			//			this.sau.writeGISoutput(time2SpecificEmissions, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
		}
	}

	private Map<Double, double[][]> calculateSpecificDelaysPerBin(
			Map<Double, double[][]> time2weightedDelays,
			Map<Double, double[][]> time2weightedDemand) {

		Map<Double, double[][]> time2specificDelays = new HashMap<Double, double[][]>();
		for( Double endOfTimeInterval : time2weightedDelays.keySet()){
			double [][] specificDelays = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					specificDelays[xIndex][yIndex] = time2weightedDelays.get(endOfTimeInterval)[xIndex][yIndex] / time2weightedDemand.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2specificDelays.put(endOfTimeInterval, specificDelays);
		}
		return time2specificDelays;
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

	private Map<Double, Map<Id,  Double>> filterLinks (Map<Double, Map<Id, Double>> time2LinksData) {
		Map<Double, Map<Id, Double>> time2LinksDataFiltered = new HashMap<Double, Map<Id, Double>>();

		for(Double endOfTimeInterval : time2LinksData.keySet()){
			Map<Id,  Double> linksData = time2LinksData.get(endOfTimeInterval);
			Map<Id, Double> linksDataFiltered = new HashMap<Id,  Double>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id linkId = link.getId();
					
					if(linksData.get(linkId) == null){
						linksDataFiltered.put(linkId, 0.);
					} else {
						linksDataFiltered.put(linkId, linksData.get(linkId));
					}
				}
			}
			time2LinksDataFiltered.put(endOfTimeInterval, linksDataFiltered);
		}
		return time2LinksDataFiltered;
	}


	private Map<Double, double[][]> fillWeightedCongestionValues(Map<Double, Map<Id,  Double>> time2DelayAndFiltered)  {
		Map<Double, double[][]> time2weightedDelays = new HashMap<Double, double[][]>();

		for(Double endOfTimeInterval : time2DelayAndFiltered.keySet()){
			double[][]weightedDelays = new double[noOfXbins][noOfYbins];

			for(Id linkId : time2DelayAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();

				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				double value = time2DelayAndFiltered.get(endOfTimeInterval).get(linkId);
				double scaledValue = this.scalingFactor * value;

				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell;
						if(line){
							weightOfLinkForCell = extended.calculateWeightOfLineForCellV2(fromNodeCoord,toNodeCoord,cellCentroid.getX(), cellCentroid.getY());
						} else{
							weightOfLinkForCell	= 	this.sau.calculateWeightOfPointForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						}
						weightedDelays[xIndex][yIndex] += weightOfLinkForCell * scaledValue;	
					}
				}
			}
			time2weightedDelays.put(endOfTimeInterval, weightedDelays);
		}
		return time2weightedDelays;
	}

	private Map<Double, double[][]> fillWeightedDemandValues(Map<Double, Map<Id, Double>> time2CountsPerLinkAndFiltered)  {
		Map<Double, double[][]> time2weightedDemand = new HashMap<Double, double[][]>();

		for(Double endOfTimeInterval : time2CountsPerLinkAndFiltered.keySet()){
			double[][]weightedDemand = new double[noOfXbins][noOfYbins];

			for(Id linkId : time2CountsPerLinkAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;

				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				double count = time2CountsPerLinkAndFiltered.get(endOfTimeInterval).get(linkId);
				double vkm = count * linkLength_km;
				double scaledVkm = this.scalingFactor * vkm;

				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell;
						if(line){
							weightOfLinkForCell = extended.calculateWeightOfLineForCellV2(fromNodeCoord,toNodeCoord, cellCentroid.getX(), cellCentroid.getY());
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

	private void processCongestions(String eventFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		this.congestionPerLinkHandler = new CongestionPerLinkHandler(noOfTimeBins, simulationEndTime, scenario);
		eventsManager.addHandler(this.congestionPerLinkHandler);
		eventsReader.readFile(this.eventsFileBAU);
	}
	
	private void processEmissions(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.warmHandler);
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
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}
	public static void main(String[] args) {
		SpatialAveragingCongestion sac = new SpatialAveragingCongestion();
		sac.run();
	}
}
