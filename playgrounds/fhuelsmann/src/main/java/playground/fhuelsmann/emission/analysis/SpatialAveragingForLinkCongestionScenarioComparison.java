package playground.fhuelsmann.emission.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
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
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

public class SpatialAveragingForLinkCongestionScenarioComparison {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkCongestionScenarioComparison.class);

	private final static String runNumber1 = "981";
	private final static String runNumber2 = "983";
	private final static String runDirectory1 = "../../run" + runNumber1 + "/";
	private final static String runDirectory2 = "../../run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
	
	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory2 + runNumber2 + ".output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String eventsFile1 = runDirectory1 +"/ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".events.xml.gz";
	private final String eventsFile2 = runDirectory2 +"/ITERS/it." + lastIteration2 + "/" + runNumber2 + "." + lastIteration2 + ".events.xml.gz";
	
	Scenario scenario;
	Network network;
	private FeatureType featureType;
	CongestionPerLinkHandler congestionHandler; 
	SortedSet<String> listOfPollutants;

	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static int noOfTimeBins = 1;
	double simulationEndTime;

	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	static int noOfXbins = 80;
	static int noOfYbins = 60;
	static int minimumNoOfLinksInCell = 1;

	// OUTPUT
	private final String outPathStub = runDirectory2 + "/emissions/" + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1;

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		loadScenario(netFile1);
		this.network = this.scenario.getNetwork();
		initFeatures();

		processCongestion(eventsFile1);
		Map<Double, Map<Id, Double>> time2CongestionTotal1 = this.congestionHandler.getCongestionPerLinkAndTimeInterval();
		Map<Double, Map<Id, Double>> time2CongestionTotalFiltered1 = setNonCalculatedCongestionAndFilter(time2CongestionTotal1);

		this.congestionHandler.reset(0);
		
		processCongestion(eventsFile2);
		Map<Double, Map<Id, Double>> time2congestionTotal2 = this.congestionHandler.getCongestionPerLinkAndTimeInterval();
		Map<Double, Map<Id, Double>> time2CongestionTotalFiltered2 = setNonCalculatedCongestionAndFilter(time2congestionTotal2);
		Map<Double, Map<Id, Double>> time2deltaCongestionTotal = calculateCongestionDifferences(time2CongestionTotalFiltered1, time2CongestionTotalFiltered2);
		
		Collection<Feature> features = new ArrayList<Feature>();

		for(double endOfTimeInterval : time2deltaCongestionTotal.keySet()){
			Map<Id, Double> deltaCongestionTotal = time2deltaCongestionTotal.get(endOfTimeInterval);

			int[][] noOfLinksInCell = new int[noOfXbins][noOfYbins];
			double[][] sumOfweightsForCell = new double[noOfXbins][noOfYbins];
			double[][] sumOfweightedValuesForCell = new double[noOfXbins][noOfYbins];

			for(Link link : network.getLinks().values()){
				
				if (deltaCongestionTotal.containsKey(link.getId())){
					
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
								double value = deltaCongestionTotal.get(linkId);
								// TODO: not distance between data points, but distance between
								// data point and cell centroid is used now; is the former to expensive?
								double weightOfLinkForCell = calculateWeightOfPersonForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
								sumOfweightsForCell[xIndex][yIndex] += weightOfLinkForCell;
								sumOfweightedValuesForCell[xIndex][yIndex] += weightOfLinkForCell * value;
							}
						}
					}
				}
				else{//do nothing
				}
			}
			for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(noOfLinksInCell[xIndex][yIndex] > minimumNoOfLinksInCell){
					//	if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / sumOfweightsForCell[xIndex][yIndex];
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
						
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
				//	}
				}
			}
		}
		ShapeFileWriter.writeGeometries(features, outPathStub + ".Mid.congestionIndicatorPerLinkSmoothed_filter_demand.shp");
		logger.info("Finished writing output to " + outPathStub + ".Mid.congestionIndicatorPerLinkSmoothed_filter_demand.shp");
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
	
	
	private Map<Double, Map<Id,  Double>> calculateCongestionDifferences(
			Map<Double, Map<Id, Double>> time2CongestionTotal1,
			Map<Double, Map<Id, Double>> time2CongestionTotal2) {

		Map<Double, Map<Id, Double>> time2delta = new HashMap<Double, Map<Id, Double>>();
		for(Entry<Double, Map<Id, Double>> entry0 : time2CongestionTotal1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Double> linkId2congestion = entry0.getValue();
			Map<Id, Double> delta = new HashMap<Id, Double>();

			for(Entry<Id, Double> entry1 : linkId2congestion.entrySet()){
				Id linkId = entry1.getKey();
				Double congestionDifferenceRatio;
				Double congestionBefore = entry1.getValue();
				Double congestionAfter = time2CongestionTotal2.get(endOfTimeInterval).get(linkId);
				if (congestionBefore!=0.0){
				congestionDifferenceRatio = (congestionAfter - congestionBefore)/congestionBefore;
				delta.put(linkId, congestionDifferenceRatio);
				} else {
					//do nothing
				}
			}
			time2delta.put(endOfTimeInterval, delta);
		}
		return time2delta;
	}

	private Map<Double, Map<Id, Double>> setNonCalculatedCongestionAndFilter(Map<Double, Map<Id, Double>> time2CongestionTotal) {
		Map<Double, Map<Id, Double>> time2CongestionTotalFiltered = new HashMap<Double, Map<Id, Double>>();

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
				"Point", Point.class, true, null, null, this.targetCRS);
		AttributeType time = AttributeTypeFactory.newAttributeType(
				"Time", String.class);
		AttributeType congestion = AttributeTypeFactory.newAttributeType(
				"congestion", Double.class);
		
		Exception ex;
		try {
			this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[]
			        {point, time, congestion}, "CongestionPoint");
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
		new SpatialAveragingForLinkCongestionScenarioComparison().run();
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
