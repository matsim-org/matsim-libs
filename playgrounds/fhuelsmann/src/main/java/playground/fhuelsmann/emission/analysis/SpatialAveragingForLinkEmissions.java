package playground.fhuelsmann.emission.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

import playground.benjamin.events.emissions.ColdPollutant;
import playground.benjamin.events.emissions.EmissionEventsReader;
import playground.benjamin.events.emissions.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.EmissionsPerLinkWarmEventHandler;



public class SpatialAveragingForLinkEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkEmissions.class);

	private final String runNumber1 = "981";
//	private final String runNumber2 = "985";
	private final String runDirectory1 = "../../run" + runNumber1 + "/"+ runNumber1+".";
//	private final String runDirectory2 = "../../run" + runNumber2 + "/"+ runNumber2+".";
	private final String configFile = runDirectory1 + "output_config.xml.gz";
	private final String netFile = runDirectory1 + "output_network.xml.gz";
//	private final String configFile2 = runDirectory2 + "output_config.xml.gz";

	private final String outPath = "../../run" + runNumber1 +"/emissions/" + runNumber1 + ".";

	private final String emissionFile1 = "../../run" + runNumber1 +"/emission.events.xml.gz";
//	private final String emissionFile2 = "../../run" + runNumber2 +"/emission.events.xml.gz";

//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String configFile1 = runDirectory1 + "output_config.xml.gz";

	Scenario scenario;
	Network network;
	private FeatureType featureType;
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;

	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static int noOfTimeBins = 30;
	double simulationEndTime;

	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	static int noOfXbins = 80;
	static int noOfYbins = 60;
	static int minimumNoOfLinksInCell = 1;
	static String pollutant = WarmPollutant.PM.toString();

	// OUTPUT
	

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile);
		defineListOfPollutants();
		loadScenario(netFile);
		this.network = this.scenario.getNetwork();
		initFeatures();

		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();

		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1 = sumUpEmissions(time2warmEmissionsTotal1, time2coldEmissionsTotal1);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered1 = setNonCalculatedEmissionsAndFilter(time2EmissionsTotal1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

	
	
//		EmissionWriter eWriter = new EmissionWriter();
//		BufferedWriter writer = IOUtils.getBufferedWriter(outPathStub + "Smoothed.txt");
//		writer.append("xCentroid\tyCentroid\tCO2_TOTAL\tTIME\n");

		Collection<Feature> features = new ArrayList<Feature>();

		for(double endOfTimeInterval : time2EmissionsTotalFiltered1.keySet()){
			Map<Id, Map<String, Double>> deltaEmissionsTotal = time2EmissionsTotalFiltered1.get(endOfTimeInterval);
//			String outFile = outPathStub + (int) endOfTimeInterval + ".txt";
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
							double value = deltaEmissionsTotal.get(linkId).get(pollutant);
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
						if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / sumOfweightsForCell[xIndex][yIndex];
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
//							String outString = cellCentroid.getX() + "\t" + cellCentroid.getY() + "\t" + averageValue + "\t" + dateTimeString + "\n";
//							writer.append(outString);
						
							Point point = MGC.xy2Point(cellCentroid.getX(), cellCentroid.getY());
							try {
								Feature feature = this.featureType.create(new Object[] {
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
//		writer.close();
//		logger.info("Finished writing output to " + outPathStub + "Smoothed.txt");
		
		ShapeFileWriter.writeGeometries(features, outPath +  "." + pollutant + ".movie.emissionsPerLinkandTimePeriodSmoothed.shp");
		logger.info("Finished writing output to " + outPath +  "." + pollutant + ".movie.emissionsPerLinkTimePeriodSmoothed.shp");
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

	@SuppressWarnings("deprecation")
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
