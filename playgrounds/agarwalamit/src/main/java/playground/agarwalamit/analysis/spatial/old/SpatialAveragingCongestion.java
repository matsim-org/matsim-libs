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
package playground.agarwalamit.analysis.spatial.old;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.agarwalamit.analysis.congestion.CongestionPerLinkHandler;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.old.SpatialAveragingUtilsExtended;

/**
 * @author amit after Benjamin
 */
public class SpatialAveragingCongestion {
	private final Logger logger = Logger.getLogger(SpatialAveragingCongestion.class);
	private CongestionPerLinkHandler congestionPerLinkHandler;
	final double scalingFactor = 1.;
	private final static String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	private final static String runBAU = runDir+"/baseCaseCtd";
	private final static String runNumber = runDir+"/ei";
	private final String netFile1 = runBAU+"/output_network.xml.gz";

	private final String munichShapeFile = SpatialDataInputs.shapeFile;

	private static String configFileBAU =runBAU+"/output_config.xml"; 
	private final String eventsFileBAU = runBAU+"/ITERS/it.1500/1500.events.xml.gz";
	private final String eventsFile2 = runNumber+"/ITERS/it.1500/1500.events.xml.gz";
	private final String emissionFileBAU = runBAU+"/ITERS/it.1500/1500.emission.events.xml.gz";
	private final String emissionFile2 = runNumber+"/ITERS/it.1500/1500.emission.events.xml.gz";

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
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
	final boolean compareToBAU = true;

	SpatialAveragingUtils sau;
	LocationFilter lf;
	double simulationEndTime;
	Scenario scenario;
	Network network;

	String outPathStub;
	SpatialAveragingUtilsExtended extended ;
	EmissionsPerLinkWarmEventHandler warmHandler;

	public  void run(){
		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS);
		this.lf = new LocationFilter();
		extended = new SpatialAveragingUtilsExtended(smoothingRadius_m);
		this.simulationEndTime = getEndTime(configFileBAU);

		scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();
		processCongestions(eventsFileBAU);
		processEmissions(emissionFileBAU);

		 Map<Double, Map<Id<Link>, Double>> time2LinkDelays = this.congestionPerLinkHandler.getDelayPerLinkAndTimeInterval();
		//to keep the same demand in emission and congestion pricing, more logical is using demand from warm emission analysis module.
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLink = this.warmHandler.getTime2linkIdLeaveCount(); 

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
			outPathStub = runDir+"/analysis/spatialPlots/rCongestionLine"+smoothingRadius_m;
		} else {
			outPathStub = runDir+"/analysis/spatialPlots/rCongestionPoint"+smoothingRadius_m;
		}

		for(double endOfTimeInterval: time2WeightedCongestion.keySet()){
			this.sau.writeRoutput(normalizedTime2WeightedCongestion.get(endOfTimeInterval), outPathStub + ".sec"+endOfTimeInterval + ".txt");
			this.sau.writeRoutput(normalizedTime2WeightedDemand.get(endOfTimeInterval), outPathStub + ".vkm." + endOfTimeInterval + ".txt");
			this.sau.writeRoutput(time2SpecificDelays.get(endOfTimeInterval), outPathStub+ ".secVkm." + endOfTimeInterval + ".txt");
		}

		if(compareToBAU){
			processCongestions(eventsFile2);
			processEmissions(emissionFile2);
			Map<Double, Map<Id<Link>, Double>> time2LinkDelays2 = this.congestionPerLinkHandler.getDelayPerLinkAndTimeInterval();
			Map<Double, Map<Id<Link>, Double>> time2CountsPerLink2 = this.warmHandler.getTime2linkIdLeaveCount();


			Map<Double, double[][]> time2WeightedCongestion2 = fillWeightedCongestionValues(filterLinks(time2LinkDelays2));
			Map<Double, double[][]> normalizedTime2WeightedCongestion2 = normalizeAllArrays(time2WeightedCongestion2);

			Map<Double, double[][]> time2WeightedDemand2 = fillWeightedDemandValues(filterLinks(time2CountsPerLink2));
			Map<Double, double[][]> normalizedTime2WeightedDemand2 = normalizeAllArrays(time2WeightedDemand2);

			/* Sum over weighted values for cell does not need to be normalized, since normalization canceles out.
			 * Result is an average value for cell*/
			Map<Double, double[][]> time2SpecificDelays2 = calculateSpecificDelaysPerBin(time2WeightedCongestion2, time2WeightedDemand2);
			////			Map<Double, double[][]> time2SpecificEmissions2 = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions2, time2NormalizedWeightedDemand2);
			//			
			/* Sum over weighted values for cell is normalized to "per sqkm" (dependent on calcluateWeightOfLinkForCell)
			 * Values NEED to be additive (e.g. vkm, g, counts, or AVERAGE g/vkm!)
			 * Make sure coordinate system is metric */
			Map<Double, double[][]> time2AbsoluteDelaysDifferences = calculateAbsoluteDifferencesPerBin(normalizedTime2WeightedCongestion, normalizedTime2WeightedCongestion2);
			Map<Double, double[][]> time2AbsoluteDemandDifferences = calculateAbsoluteDifferencesPerBin(normalizedTime2WeightedDemand, normalizedTime2WeightedDemand2);
			Map<Double, double[][]> time2SpecificCongestionDifferences = calculateAbsoluteDifferencesPerBin(time2SpecificDelays, time2SpecificDelays2);

			if(line){
				outPathStub = runDir+"/analysis/spatialPlots/rCongestionWRTBAULine"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
			} else {
				outPathStub = runDir+"/analysis/spatialPlots/rCongestionWRTBAUPoint"+smoothingRadius_m;//runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
			}

			for(double endOfTimeInterval : time2AbsoluteDemandDifferences.keySet()){
				this.sau.writeRoutput(time2AbsoluteDelaysDifferences.get(endOfTimeInterval), outPathStub  + ".sec." + endOfTimeInterval + ".txt");
				this.sau.writeRoutput(time2AbsoluteDemandDifferences.get(endOfTimeInterval),	outPathStub + ".vkm." + endOfTimeInterval + ".txt");
				this.sau.writeRoutput(time2SpecificCongestionDifferences.get(endOfTimeInterval), outPathStub  +  ".secPerVkm." + endOfTimeInterval + ".txt");
			}	
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

	private Map<Double, Map<Id<Link>,  Double>> filterLinks (Map<Double, Map<Id<Link>, Double>> time2LinksData) {
		Map<Double, Map<Id<Link>, Double>> time2LinksDataFiltered = new HashMap<Double, Map<Id<Link>, Double>>();

		for(Double endOfTimeInterval : time2LinksData.keySet()){
			Map<Id<Link>,  Double> linksData = time2LinksData.get(endOfTimeInterval);
			Map<Id<Link>, Double> linksDataFiltered = new HashMap<Id<Link>,  Double>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id<Link> linkId = link.getId();

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


	private Map<Double, double[][]> fillWeightedCongestionValues(Map<Double, Map<Id<Link>, Double>> map)  {
		Map<Double, double[][]> time2weightedDelays = new HashMap<Double, double[][]>();

		for(Double endOfTimeInterval : map.keySet()){
			double[][]weightedDelays = new double[noOfXbins][noOfYbins];

			for(Id<Link> linkId : map.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();

				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				double value = map.get(endOfTimeInterval).get(linkId);
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

	private Map<Double, double[][]> fillWeightedDemandValues(Map<Double, Map<Id<Link>, Double>> map)  {
		Map<Double, double[][]> time2weightedDemand = new HashMap<Double, double[][]>();

		for(Double endOfTimeInterval : map.keySet()){
			double[][]weightedDemand = new double[noOfXbins][noOfYbins];

			for(Id<Link> linkId : map.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;

				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();

				double count = map.get(endOfTimeInterval).get(linkId);
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
	private void processCongestions(String eventFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		this.congestionPerLinkHandler = new CongestionPerLinkHandler(noOfTimeBins, simulationEndTime, scenario);
		eventsManager.addHandler(this.congestionPerLinkHandler);
		eventsReader.readFile(eventFile);
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
