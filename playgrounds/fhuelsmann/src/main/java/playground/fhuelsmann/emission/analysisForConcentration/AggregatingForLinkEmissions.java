package playground.fhuelsmann.emission.analysisForConcentration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionUtils;


/**
 * @author benjamin,friederike
 *
 */

public class AggregatingForLinkEmissions {
	
	private static final Logger logger = Logger.getLogger(AggregatingForLinkEmissions.class);

	private final static String runDirectory = "../../detEval/kuhmo/output/output_baseCase_ctd/";
	private final String netFile =runDirectory + "output_network.xml.gz";

	private static String configFile = runDirectory + "output_config.xml.gz";
	private final String emissionFile = runDirectory + "ITERS/it.1500/1500.emission.events.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	Network network;
	Set<Feature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub;

	Map<Double, Map<Id, Double>> time2CountsPerLink;
	Map<Double, Map<Id, Double>> time2HdvCountsPerLink;

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
		
		Map<Double, Map<Id, Map<String,Map<String, Double>>>> time2EmissionMapToAnalyze;
		
		processEmissions(emissionFile);
		Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		time2CountsPerLink = this.warmHandler.getTime2linkIdLeaveCount();
		time2HdvCountsPerLink=this.warmHandler.getTime2linkIdLeaveHDVCount();

		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal = sumUpEmissionsPerTimeInterval(time2warmEmissionsTotal, time2coldEmissionsTotal);
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered = filterLinks(time2EmissionsTotalFilled);
		 Map<Double, Map<Id, Map<String, Map<String, Double>>>> time2linkId2splitLinkId2EmissionsMs = calculateEmissionsPerMeterSecond(time2EmissionsTotalFilledAndFiltered);
		 Map<Double, Map<Id, Map<String, Map<String, Double>>>> EmissionsPerSplitlinkIdAggregate = AggregateEmissionsSameLinkLocation(time2linkId2splitLinkId2EmissionsMs);
		
		//map of timePeriod, linkId, array of allVehiclecounts, hdvCounts, freeSpeed=freeSpeedhdv
		Map<Double, Map<Id, Double[]>>time2CountsTotalFiltered = setNonCalculatedCountsAndFilter(time2CountsPerLink,time2HdvCountsPerLink);
		
		time2EmissionMapToAnalyze=EmissionsPerSplitlinkIdAggregate;

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		EmissionWriter emissionWriter = new EmissionWriter();
		emissionWriter.writeHour2Link2Emissions(
				listOfPollutants,
				time2EmissionMapToAnalyze,
				network,
				runDirectory +"airPollution/emissionsAggregatePerLinkAndHour.txt");
		
		CountsWriter countsWriter = new CountsWriter();
		countsWriter.writeHour2Link2Counts(
				time2CountsTotalFiltered,
				network,
				runDirectory +"airPollution/countsPerLinkAndHour.txt");

	}
	


	 /*begin of emission calculation*/
    private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
		
		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	public Map<Double, Map<Id, Map<String, Double>>> filterLinks(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
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

		/*begin writing counts and freespeed to an array*/
	private Map<Double, Map<Id, Double[]>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id, Double>> time2CountsPerLink,Map<Double, Map<Id, Double>>time2HdvCountsPerLink) {
		Map<Double, Map<Id, Double[]>> time2CountsTotalFiltered = new HashMap<Double, Map<Id,Double[]>>();
		
		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id, Double> linkIdHdvCount = time2HdvCountsPerLink.get(endOfTimeInterval);
			Map<Id, Double[]> linkId2CountFiltered = new HashMap<Id, Double[]>();
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();
				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
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
						
						} else {
							linkId2CountFiltered.put(linkId, countsFreeSpeed);
						//	System.out.print("\n***"+countsFreeSpeed[0]+" all "+countsFreeSpeed[1]+" hdv "+countsFreeSpeed[2]);
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
			//if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				//}
			//} else {
			//	Map<Id, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
			//	totalEmissions = emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}
	
	
	private Map<Double, Map<Id, Map<String, Map<String, Double>>>> calculateEmissionsPerMeterSecond(
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered) {

		Map<Double, Map<Id, Map<String, Map<String, Double>>>> time2link2emissions2MeterSecond = new HashMap<Double, Map<Id, Map<String, Map<String, Double>>>>();

		for (Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2EmissionsTotalFilledAndFiltered.entrySet()) {
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> linkId2Emissions = entry0.getValue();

			Map<Id, Map<String, Map<String, Double>>> linkId2link2emissions2ms = new HashMap<Id, Map<String, Map<String, Double>>>();

			for (Entry<Id, Map<String, Double>> entry1 : linkId2Emissions.entrySet()) {
				Id linkId = entry1.getKey();
				if (isInMunichShape(linkId)) {

					String linkString = linkId.toString();
					String[] link;
					String delimiter = "-";
			
					link = linkString.split(delimiter);
					
					Map<String, Map<String, Double>> link2emissions2ms = new HashMap<String, Map<String, Double>>();
					for (int i = 0; i < link.length; i++) {

						Map<String, Double> emission2ms = new HashMap<String, Double>();
						for (String pollutant : entry1.getValue().keySet()) {
							Double emissions = entry1.getValue().get(pollutant);
							double linkLength_m = this.network.getLinks().get(linkId).getLength();
							double emissionsPerM;
							double emissionsPerMeterSecond;
							emissionsPerM = emissions / linkLength_m;
							emissionsPerMeterSecond = emissionsPerM / 3.6 * 100; // from 1%  to 100% sample
							emission2ms.put(pollutant, emissionsPerMeterSecond);
						}
						link2emissions2ms.put(link[i], emission2ms);
					}
					linkId2link2emissions2ms.put(linkId, link2emissions2ms);
				}
			}
			time2link2emissions2MeterSecond.put(endOfTimeInterval,linkId2link2emissions2ms);
		}
		return time2link2emissions2MeterSecond;
	}
	
	private Map<Double, Map<Id, Map<String, Map<String, Double>>>> AggregateEmissionsSameLinkLocation(
			Map<Double, Map<Id, Map<String, Map<String, Double>>>> time2linkId2splitLinkId2EmissionsMs) {
		Map<Double, Map<Id, Map<String, Map<String, Double>>>> time2linkId2AggregateEmissions = new HashMap<Double, Map<Id, Map<String, Map<String, Double>>>>();

		for (Double endOfTimeInterval : time2linkId2splitLinkId2EmissionsMs.keySet()) {
			Map<Id, Map<String, Map<String, Double>>> linkId2Emissions = time2linkId2splitLinkId2EmissionsMs.get(endOfTimeInterval);
			Map<Id, Map<String, Map<String, Double>>> linkId2AggregateEmissions = new HashMap<Id, Map<String, Map<String, Double>>>();

			for (Entry<Id, Map<String, Map<String, Double>>> entry1 : linkId2Emissions.entrySet()) {
				Id linkId = entry1.getKey();
				Map<String, Map<String, Double>> linkId2SplitLinks = linkId2Emissions.get(linkId);
				Map<String, Map<String, Double>> linkId2link2Emissions = new HashMap<String, Map<String, Double>>();

				for (Entry<String, Map<String, Double>> entry2 : linkId2SplitLinks.entrySet()) {
					String link = entry2.getKey();
					Map<String, Double> emissionsAggregate = new HashMap<String, Double>();					
					
					if (!link.toString().contains("R")) {
						String linkR = link + "R";
					//	Id linkid = new IdImpl(linkR);
	
						if (linkId2SplitLinks.get(linkR) != null) {
							Map<String, Double> emissionsSoFar = linkId2SplitLinks.get(linkR);
								for (String pollutant : entry2.getValue().keySet()) {
								Double emissions = entry2.getValue().get(pollutant);
								Double previousValue = emissionsSoFar.get(pollutant);
								Double newValue = previousValue + emissions;
								emissionsAggregate.put(pollutant, newValue);
								//if (link.equals("52799310"))
								//	System.out.println("********wenn Richtung -rechts- vorhanden*********" + link+ " emissions " + emissions +" newValue "+newValue);
							}
							linkId2link2Emissions.put(link, emissionsAggregate);

						} else {
								for (String pollutant : entry2.getValue().keySet()) {
									Double emissions = entry2.getValue().get(pollutant);
									emissionsAggregate.put(pollutant, emissions);
								}
								linkId2link2Emissions.put(link,emissionsAggregate);
						}
					}

					else {
						String linkL;
						linkL = link.replaceAll("R", "");
						if (linkId2SplitLinks.get(linkL) != null) {
							Map<String, Double> emissionsSoFar = linkId2SplitLinks.get(linkL);

							for (String pollutant : entry2.getValue().keySet()) {
								Double emissions = entry2.getValue().get(pollutant);
								Double previousValue = emissionsSoFar.get(pollutant);
								Double newValue = previousValue + emissions;
								emissionsAggregate.put(pollutant, newValue);
							//	if (link.equals("586891439R"))
							//	System.out.println("********wenn Richtung -links- vorhanden*********" + link + " emissions " + emissions +" newValue "+newValue);
							}
							linkId2link2Emissions.put(link, emissionsAggregate);
						} else {
								for (String pollutant : entry2.getValue().keySet()) {
									Double emissions = entry2.getValue().get(pollutant);
									emissionsAggregate.put(pollutant, emissions);
								}
								linkId2link2Emissions.put(link,emissionsAggregate);
						}
					}
				}
				linkId2AggregateEmissions.put(linkId, linkId2link2Emissions);
			}
			time2linkId2AggregateEmissions.put(endOfTimeInterval,
					linkId2AggregateEmissions);
		}
		return time2linkId2AggregateEmissions;
	}
			
	
	private boolean isInMunichShape(Id linkId) {
		boolean isInMunichShape = false;
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Coord linkCoord = link.getCoord();
		double xLink = linkCoord.getX();
		double yLink = linkCoord.getY();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(xLink ,yLink));
		for(Feature feature : this.featuresInMunich){
			if(feature.getDefaultGeometry().contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}
	
	private static Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInMunich;
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
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.getQSimConfigGroup().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public static void main(String[] args) throws IOException{
	new AggregatingForLinkEmissions().run();
	}

}
