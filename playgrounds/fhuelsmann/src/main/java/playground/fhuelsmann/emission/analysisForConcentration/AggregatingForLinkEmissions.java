/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.fhuelsmann.emission.analysisForConcentration;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;

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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author benjamin,friederike
 *
 */
public class AggregatingForLinkEmissions {
	
	private static final Logger logger = Logger.getLogger(AggregatingForLinkEmissions.class);

	private final static String runDirectory = "../../detEval/kuhmo/output/output_baseCase_ctd/";
	private final String netFile =runDirectory + "output_network.xml.gz";

	private static String configFile = runDirectory + "output_config.xml.gz";
	private final String emissionFile = runDirectory + "ITERS/it.1500/test3.emission.events.xml";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	Network network;
	Collection<SimpleFeature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub;

	Map<Double, Map<Id<Link>, Double>> time2CountsPerLink;
	Map<Double, Map<Id<Link>, Double>> time2HdvCountsPerLink;
	Map<Id<Link>, Map<String, Double>> linkId2AggregateEmissions = new HashMap<>();

	//final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	// define all relevant parameters
	final int noOfTimeBins = 30;
//	final int noOfXbins = 160;
//	final int noOfYbins = 120;
//	final int minimumNoOfLinksInCell = 0;
//	final double smoothingRadius_m = 500.; 
	final String pollutant2analyze = WarmPollutant.NO2.toString();
//	final boolean baseCaseOnly = true;
//	final boolean calculateRelativeChange = false;


	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile);
		this.network = scenario.getNetwork();
		this.featuresInMunich = readShape(munichShapeFile);
		
		Map<Double,Map<Id<Link>,Map<String, Double>>> time2EmissionMapToAnalyze;
		
		processEmissions(emissionFile);
		Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		time2CountsPerLink = this.warmHandler.getTime2linkIdLeaveCount();
		time2HdvCountsPerLink=this.warmHandler.getTime2linkIdLeaveHDVCount();

		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal = sumUpEmissionsPerTimeInterval(time2warmEmissionsTotal, time2coldEmissionsTotal);
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);
	//	Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered = filterLinks(time2EmissionsTotalFilled);
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2splitlinkId2emissions = calculateEmissionsinMgPerMeterSecond(time2EmissionsTotalFilled);
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2SplitlinkId2AggregateEmissions = aggregateEmissionsSameLinkLocation(time2splitlinkId2emissions);
		
		//map of timePeriod, linkId, array of allVehiclecounts, hdvCounts, freeSpeed=freeSpeedhdv
		Map<Double, Map<Id<Link>, Double[]>>time2CountsTotalFiltered = setNonCalculatedCountsAndFilter(time2CountsPerLink,time2HdvCountsPerLink);
		Map<Double, Map<Id<Link>, Double[]>> time2splitlinkId2counts = calculateCounts(time2CountsTotalFiltered);
		Map<Double, Map<Id<Link>, Double[]>> countsPerSplitlinkIdAggregate = aggregateCountsSameLinkLocation(time2splitlinkId2counts);
		
		time2EmissionMapToAnalyze=time2SplitlinkId2AggregateEmissions;

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		EmissionWriter emissionWriter = new EmissionWriter();
		emissionWriter.writeHour2Link2Emissions(
				listOfPollutants,
				time2EmissionMapToAnalyze,
				network,
				runDirectory +"airPollution/emissionsAggregateNewPerLinkAndHour.txt");
		
		CountsWriter countsWriter = new CountsWriter();
		countsWriter.writeHour2Link2Counts(
				countsPerSplitlinkIdAggregate,
				network,
				runDirectory +"airPollution/countsAggregateNewPerLinkAndHour.txt");

	}
	

	 private Map<Double, Map<Id<Link>, Double[]>> aggregateCountsSameLinkLocation(
		Map<Double, Map<Id<Link>, Double[]>> time2splitlinkId2counts) {
	
		 Map<Double, Map<Id<Link>, Double[]>> time2SplitlinkId2AggregateCounts = new HashMap<Double, Map<Id<Link>, Double[]>>();

			for (Entry<Double, Map<Id<Link>, Double[]>>entry1: time2splitlinkId2counts.entrySet()) {
				double endOfTimeInterval = entry1.getKey();
				Map<Id<Link>, Double[]> splitlinkId2Counts = entry1.getValue();
				Map<Id<Link>, Double[]> splitlinkId2AggregateCounts = new HashMap<>();

				for (Entry<Id<Link>, Double[]> entry2 : splitlinkId2Counts.entrySet()) {
					Id<Link> splitlinkId = entry2.getKey();
			
					//	add a R to the link and check if this link+R exists in the map splitlinkId2Emissions
					// if yes, add the counts of the link+R to the counts if the link
					if (!splitlinkId.toString().contains("R")) {
						String link = splitlinkId.toString();
						String linkR = link + "R";
						Id<Link> linkid = Id.create(linkR, Link.class);
						//splitlinkId = linkid;
					
						if (splitlinkId2Counts.get(linkid) != null) {
							Double [] countsFreeSpeedSoFar = splitlinkId2Counts.get(linkid);
							Double countsSoFar =countsFreeSpeedSoFar[1];
							Double hdvCountsSoFar =countsFreeSpeedSoFar[2];
							Double [] splitcountsFreeSpeed =entry2.getValue();
							Double [] newSplitcountsFreeSpeed= new Double [4];
							Double counts =splitcountsFreeSpeed[1];
							Double hdvCounts =splitcountsFreeSpeed[2];
							Double newCounts = countsSoFar + counts;
							Double newHdvCounts = hdvCountsSoFar + hdvCounts;
							newSplitcountsFreeSpeed[0]=splitcountsFreeSpeed[0];
							newSplitcountsFreeSpeed[1]= newCounts;
							newSplitcountsFreeSpeed[2]= newHdvCounts;
							newSplitcountsFreeSpeed[3]= splitcountsFreeSpeed[3];
							
							splitlinkId2AggregateCounts.put(splitlinkId,newSplitcountsFreeSpeed);
							
						} else {
							Double [] splitcountsFreeSpeed =entry2.getValue();
							splitlinkId2AggregateCounts.put(splitlinkId,splitcountsFreeSpeed);
						}
					}
					//	delete the R of a link and check if this link-R exists in the map splitlinkId2Emissions
					// if yes, add the emissions of the link+R to the emissions if the link
					else {
						String link = splitlinkId.toString();
						String linkL = link.replaceAll("R", "");
						Id<Link> linkid = Id.create(linkL, Link.class);
						//splitlinkId = linkid;
						if (splitlinkId2Counts.get(linkid) != null) {
							Double [] countsFreeSpeedSoFar = splitlinkId2Counts.get(linkid);
							Double countsSoFar =countsFreeSpeedSoFar[1];
							Double hdvCountsSoFar =countsFreeSpeedSoFar[2];
							Double [] splitcountsFreeSpeed =entry2.getValue();
							Double [] newSplitcountsFreeSpeed= new Double [4];
							Double counts =splitcountsFreeSpeed[1];
							Double hdvCounts =splitcountsFreeSpeed[2];
							Double newCounts = countsSoFar + counts;
							Double newHdvCounts = hdvCountsSoFar + hdvCounts;
							newSplitcountsFreeSpeed[0]=splitcountsFreeSpeed[0];
							newSplitcountsFreeSpeed[1]= newCounts;
							newSplitcountsFreeSpeed[2]= newHdvCounts;
							newSplitcountsFreeSpeed[3]= splitcountsFreeSpeed[3];
							
							splitlinkId2AggregateCounts.put(splitlinkId,newSplitcountsFreeSpeed);
						} else {
							Double [] splitcountsFreeSpeed =entry2.getValue();
							splitlinkId2AggregateCounts.put(splitlinkId,splitcountsFreeSpeed);
						}
					}
				}
				time2SplitlinkId2AggregateCounts.put(endOfTimeInterval, splitlinkId2AggregateCounts);
				System.out.println("*********************************"+endOfTimeInterval+"****************************");
			}
			return time2SplitlinkId2AggregateCounts;
	 }



	private Map<Double, Map<Id<Link>, Double[]>> calculateCounts(
		Map<Double, Map<Id<Link>, Double[]>> time2CountsTotalFiltered) {
	
		 Map<Double, Map<Id<Link>, Double[]>> time2splitlinkId2counts = new HashMap<>();
			for (Entry<Double, Map<Id<Link>, Double[]>> entry0 : time2CountsTotalFiltered.entrySet()) {
				double endOfTimeInterval = entry0.getKey();
				Map<Id<Link>, Double[]> linkId2counts = entry0.getValue();
				Map<Id<Link>, Double[]> splitlinkId2counts = new HashMap<>();
				Map<Id<Link>, Double[]> newlinkId2counts = new HashMap<>();
				for (Entry<Id<Link>, Double[]> entry1 : linkId2counts.entrySet()) {
					Id<Link> linkId = entry1.getKey();
					if (isInMunichShape(linkId)) {
						
						String linkString = linkId.toString();
						String[] link;
						String delimiter = "-";
						link = linkString.split(delimiter);

						Map<Id<Link>, Double[]>testsplitlinkId2counts = new HashMap<>();
						
						for (int i = 0; i < link.length; i++) {
							Double [] splitcountsFreeSpeed = new Double [4];
								Double[] countsFreeSpeed = entry1.getValue();
								splitcountsFreeSpeed[0]= countsFreeSpeed[0]*3.6;
								splitcountsFreeSpeed[1]= countsFreeSpeed[1]*100;							
								splitcountsFreeSpeed[2]= countsFreeSpeed[2]*100;
								splitcountsFreeSpeed[3]= countsFreeSpeed[3];
								
								if (entry1.getValue()!= countsFreeSpeed){
									throw new RuntimeException("The orginal " + linkId + " doesn't show the same values as the splitlinkIds " +link[i]);
								}
							
							Id<Link> splitlinkId = Id.create(link[i], Link.class);
							splitlinkId2counts.put(splitlinkId, splitcountsFreeSpeed);
							testsplitlinkId2counts.put(splitlinkId, splitcountsFreeSpeed);		
						}
						newlinkId2counts.putAll(splitlinkId2counts);
				
					}
				}
				time2splitlinkId2counts.put(endOfTimeInterval,newlinkId2counts);
			}
			return time2splitlinkId2counts;
	}



	/*begin of emission calculation*/
    private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<>();
		
		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id<Link>, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}


		/*begin writing counts and freespeed to an array*/
	private Map<Double, Map<Id<Link>, Double[]>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id<Link>, Double>> time2CountsPerLink,Map<Double, Map<Id<Link>, Double>>time2HdvCountsPerLink) {
		Map<Double, Map<Id<Link>, Double[]>> time2CountsTotalFiltered = new HashMap<>();
		
		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id<Link>, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id<Link>, Double> linkIdHdvCount = time2HdvCountsPerLink.get(endOfTimeInterval);
			Map<Id<Link>, Double[]> linkId2CountFiltered = new HashMap<>();
			for(Link link : network.getLinks().values()){
						Id<Link> linkId = link.getId();
						if (isInMunichShape(linkId)) {
						Double freeVelocity= link.getFreespeed();
						Double length= link.getLength();
						Double counts = linkId2Count.get(linkId);
						Double hvdCounts = linkIdHdvCount.get(linkId);
						Double [] countsFreeSpeed = new Double [4];
						countsFreeSpeed[0]= freeVelocity;
						countsFreeSpeed[1]= counts;
						countsFreeSpeed[2]= hvdCounts;
						countsFreeSpeed[3]= length;
												
						if(linkId2Count.get(linkId) == null){
							countsFreeSpeed[0]= freeVelocity;
							countsFreeSpeed[1]= 0.;
							countsFreeSpeed[2]= 0.;
							countsFreeSpeed[3]= length;
							linkId2CountFiltered.put(linkId, countsFreeSpeed);
						} else if(linkIdHdvCount.get(linkId)==null && linkId2Count.get(linkId) != null) {
							countsFreeSpeed[2]= 0.;
							
							linkId2CountFiltered.put(linkId, countsFreeSpeed);
						}

						else {
							linkId2CountFiltered.put(linkId, countsFreeSpeed);
						}
					}
				}
			time2CountsTotalFiltered.put(endOfTimeInterval, linkId2CountFiltered);
		}
		return time2CountsTotalFiltered;
	}

	private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {

		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2totalEmissions = new HashMap<>();

		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			
			Map<Id<Link>, SortedMap<String, Double>> totalEmissions = new HashMap<>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id<Link> id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id<Link>, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}
	
	private Map<Double, Map<Id<Link>, Map<String, Double>>> calculateEmissionsinMgPerMeterSecond(
			Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {

		Map<Double, Map<Id<Link>, Map<String, Double>>> time2splitlinkId2emissions = new HashMap<>();
		for (Entry<Double, Map<Id<Link>, SortedMap<String, Double>>> entry0 : time2EmissionsTotal.entrySet()) {
			double endOfTimeInterval = entry0.getKey();
			Map<Id<Link>, SortedMap<String, Double>> linkId2Emissions = entry0.getValue();

			Map<Id<Link>, Map<String, Double>> newlinkId2emissions = new HashMap<>();
			for (Entry<Id<Link>, SortedMap<String, Double>> entry1 : linkId2Emissions.entrySet()) {
				Id<Link> linkId = entry1.getKey();
							
				if (isInMunichShape(linkId)) {
					
					String linkString = linkId.toString();
					String[] link;
					String delimiter = "-";
				
					link = linkString.split(delimiter);
					Map<Id<Link>, Map<String, Double>> splitlinkId2emissions = new HashMap<>();
					Map<Id<Link>, Map<String, Double>> testsplitlinkId2emissions = new HashMap<>();
					for (int i = 0; i < link.length; i++) {

						Map<String, Double> pollutant2emissions = new HashMap<String, Double>();
						Map<String, Double> testpollutant2emissions = new HashMap<String, Double>();
						
						for (String pollutant : entry1.getValue().keySet()) {
							Double emissions = entry1.getValue().get(pollutant);
							testpollutant2emissions.put(pollutant, emissions);						
							double linkLength_m = this.network.getLinks().get(linkId).getLength();
							double emissionsPerM;
							double emissionsPerMs;
							emissionsPerM = emissions / linkLength_m; //calculate emissions per meter
							emissionsPerMs = emissionsPerM / 3.6 * 100; // calculate emissionsPerM per second, from g to mg, for the 100% sample
							pollutant2emissions.put(pollutant, emissionsPerMs);
							
							if (entry1.getValue().get(pollutant)!= (testpollutant2emissions).get(pollutant)){
								throw new RuntimeException("The orginal " + linkId + " doesn't show the same pollutant emissions as the splitlinkIds " +link[i]);
							}
						}
						Id<Link> splitlinkId = Id.create(link[i], Link.class);
						splitlinkId2emissions.put(splitlinkId, pollutant2emissions);
						testsplitlinkId2emissions.put(splitlinkId, testpollutant2emissions);		
					}
					newlinkId2emissions.putAll(splitlinkId2emissions);
				}
			}
			time2splitlinkId2emissions.put(endOfTimeInterval,newlinkId2emissions);
		}
		return time2splitlinkId2emissions;
	}
	
	public Map<Double, Map<Id<Link>, Map<String, Double>>> aggregateEmissionsSameLinkLocation(
			Map<Double, Map<Id<Link>, Map<String, Double>>> time2splitlinkId2emissions) {
		
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2SplitlinkId2AggregateEmissions = new HashMap<>();

		for (Entry<Double, Map<Id<Link>, Map<String, Double>>> entry1: time2splitlinkId2emissions.entrySet()) {
			double endOfTimeInterval = entry1.getKey();
			Map<Id<Link>, Map<String, Double>> splitlinkId2Emissions = entry1.getValue();
			Map<Id<Link>, Map<String, Double>> splitlinkId2AggregateEmissions = new HashMap<>();
			Map<Id<Link>, Map<String, Double>> newsplitlinkId2AggregateEmissions = new HashMap<>();

			for (Entry<Id<Link>, Map<String, Double>> entry2 : splitlinkId2Emissions.entrySet()) {
				Id<Link> splitlinkId = entry2.getKey();

				//	add a R to the link and check if this link+R exists in the map splitlinkId2Emissions
				// if yes, add the emissions of the link+R to the emissions if the link
				if (!splitlinkId.toString().contains("R")) {
					Map<String, Double> emissionsAggregate = new HashMap<String, Double>();
					String link = splitlinkId.toString();
					String linkR = link + "R";
					Id<Link> linkid = Id.create(linkR, Link.class);
					//splitlinkId = linkid;
				
					if (splitlinkId2Emissions.get(linkid) != null) {
						Map<String, Double> emissionsSoFar = splitlinkId2Emissions.get(linkid);
						for (String pollutant : entry2.getValue().keySet()) {
							Double emissions = entry2.getValue().get(pollutant);
							
							Double previousValue = emissionsSoFar.get(pollutant);
							Double newValue = previousValue + emissions;
							emissionsAggregate.put(pollutant, newValue);
						}
						splitlinkId2AggregateEmissions.put(splitlinkId,emissionsAggregate);
						} else {

						for (String pollutant : entry2.getValue().keySet()) {
							Double emissions = entry2.getValue().get(pollutant);
							emissionsAggregate.put(pollutant, emissions);
						}
						splitlinkId2AggregateEmissions.put(splitlinkId,emissionsAggregate);
					}
				}
				//	delete the R of a link and check if this link-R exists in the map splitlinkId2Emissions
				// if yes, add the emissions of the link+R to the emissions if the link
				else {
					Map<String, Double> emissionsAggregate = new HashMap<String, Double>();
					String link = splitlinkId.toString();
					String linkL = link.replaceAll("R", "");
					Id<Link> linkid = Id.create(linkL, Link.class);
					//splitlinkId = linkid;
					if (splitlinkId2Emissions.get(linkid) != null) {
						Map<String, Double> emissionsSoFar = splitlinkId2Emissions.get(linkid);

						for (String pollutant : entry2.getValue().keySet()) {
							Double emissions = entry2.getValue().get(pollutant);
							Double previousValue = emissionsSoFar.get(pollutant);
							Double newValue = previousValue + emissions;
							emissionsAggregate.put(pollutant, newValue);
						}
						splitlinkId2AggregateEmissions.put(splitlinkId,emissionsAggregate);
						
					} else {
						for (String pollutant : entry2.getValue().keySet()) {
							Double emissions = entry2.getValue().get(pollutant);
							emissionsAggregate.put(pollutant, emissions);
						}
						splitlinkId2AggregateEmissions.put(splitlinkId,emissionsAggregate);
					}
				} newsplitlinkId2AggregateEmissions.putAll(splitlinkId2AggregateEmissions);
			}
			if (splitlinkId2Emissions.size() !=splitlinkId2AggregateEmissions.size()){
				throw new RuntimeException("The map splitlinkId2Emissions with size "+ splitlinkId2Emissions.size() +" has a different size than "+
			"splitlinkId2AggregateEmissions with size " +splitlinkId2AggregateEmissions.size());
			}
			time2SplitlinkId2AggregateEmissions.put(endOfTimeInterval, splitlinkId2AggregateEmissions);
		}
		return time2SplitlinkId2AggregateEmissions;
	}
				
	
		
	
	private boolean isInMunichShape(Id<Link> linkId) {
		boolean isInMunichShape = false;
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Coord linkCoord = link.getCoord();
		double xLink = linkCoord.getX();
		double yLink = linkCoord.getY();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(xLink ,yLink));
		for(SimpleFeature feature : this.featuresInMunich){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}
	
	private static Collection<SimpleFeature> readShape(String shapeFile) {
		final Collection<SimpleFeature> featuresInMunich;
		featuresInMunich = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInMunich;
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
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public static void main(String[] args) throws IOException{
	new AggregatingForLinkEmissions().run();
	}

}
