package playground.benjamin.scenarios.munich.analysis.nectar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

/**
 * @author benjamin after fhuelsmann
 *
 */
public class SpatialAveragingForLinkDemand {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkDemand.class);

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
	private final String eventsFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".events.xml.gz";
	private final String eventsFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + runNumber2 + "." + lastIteration2 + ".events.xml.gz";
	
	Network network;
	FeatureType featureType;
	Set<Feature> featuresInMunich;
	
	WeightedDemandPerLinkHandler weightedDemandPerLinkHandler;
	DemandPerLinkHandler demandHandler; 
	SortedSet<String> listOfPollutants;
	
	Map<Double, Map<Id, Integer>> time2Counts1;
	Map<Double, Map<Id, Integer>> time2Counts2;

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
	static boolean baseCaseOnly = false;
	static boolean calculateRelativeChange = false;

	private String outPathStub;

	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();
		initFeatures();
		this.featuresInMunich = readShape(munichShapeFile);

		processEvents(eventsFile1);
//		Map<Double, Map<Id, Integer>> time2Demand1 = this.demandHandler.getDemandPerLinkAndTimeInterval();
//		Map<Double, Map<Id, Double>> time2Demand1 = this.demandHandler.getCongestionTimePerLinkAndTimeInterval();
		time2Counts1 = this.demandHandler.getDemandPerLinkAndTimeInterval();
		time2Counts1 = setNonCalculatedCountsAndFilter(time2Counts1);
//		Map<Double, Map<Id, Integer>> time2DemandFiltered1 = setNonCalculatedDemandAndFilter(time2Demand1);
//		Map<Double, Map<Id, Double>> time2Demand1 = this.weightedDemandPerLinkHandler.getDemandPerLinkAndTimeInterval();
//		Map<Double, Map<Id, Double>> time2DemandFiltered1 = setNonCalculatedDemandAndFilterDouble(time2Demand1);
		this.demandHandler.reset(0);
//		this.weightedDemandPerLinkHandler.reset(0);
		
		processEvents(eventsFile2);
//		Map<Double, Map<Id, Integer>> time2Demand2 = this.demandHandler.getDemandPerLinkAndTimeInterval();
//		Map<Double, Map<Id, Double>> time2Demand2 = this.demandHandler.getCongestionTimePerLinkAndTimeInterval();
		time2Counts2 = this.demandHandler.getDemandPerLinkAndTimeInterval();
		time2Counts2 = setNonCalculatedCountsAndFilter(time2Counts2);
//		Map<Double, Map<Id, Integer>> time2DemandFiltered2 = setNonCalculatedDemandAndFilter(time2Demand2);
//		Map<Double, Map<Id, Double>> time2Demand2 = this.weightedDemandPerLinkHandler.getDemandPerLinkAndTimeInterval();
//		Map<Double, Map<Id, Double>> time2DemandFiltered2 = setNonCalculatedDemandAndFilterDouble(time2Demand2);
		
		Map<Double, Map<Id, Double>> time2DemandMapToAnalyze;
		
		if(baseCaseOnly){
//			time2DemandMapToAnalyze = convertMapToDoubleValues(time2DemandFiltered1);
			time2DemandMapToAnalyze = convertMapToDoubleValues(time2Counts1);
//			time2DemandMapToAnalyze = time2DemandFiltered1;
			outPathStub = runDirectory1 + runNumber1 + "." + lastIteration1;
		} else {
			if(calculateRelativeChange){
//				time2DemandMapToAnalyze = calculateRelativeDemandDifferences(time2DemandFiltered1, time2DemandFiltered2);
				time2DemandMapToAnalyze = calculateRelativeDemandDifferences(time2Counts1, time2Counts2);
//				time2DemandMapToAnalyze = calculateRelativeDemandDifferencesDouble(time2DemandFiltered1, time2DemandFiltered2);
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".relativeDelta";
			} else {
//				time2DemandMapToAnalyze = calcualateAbsoluteDemandDifferences(time2DemandFiltered1, time2DemandFiltered2);
				time2DemandMapToAnalyze = calcualateAbsoluteDemandDifferences(time2Counts1, time2Counts2);
//				time2DemandMapToAnalyze = calcualateAbsoluteEmissionDifferencesDouble(time2DemandFiltered1, time2DemandFiltered2);
				outPathStub = runDirectory1 + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
			}
		}
		
		Collection<Feature> features = new ArrayList<Feature>();

		for(double endOfTimeInterval : time2DemandMapToAnalyze.keySet()){
			Map<Id, Double> demandMapToAnalyze = time2DemandMapToAnalyze.get(endOfTimeInterval);

			int[][] noOfLinksInCell = new int[noOfXbins][noOfYbins];
			double[][] sumOfweightsForCell = new double[noOfXbins][noOfYbins];
			double[][] sumOfweightedValuesForCell = new double[noOfXbins][noOfYbins];
			
			for(Link link : network.getLinks().values()){
				
				if (demandMapToAnalyze.containsKey(link.getId())){
					
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
								double value = demandMapToAnalyze.get(linkId);
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
					if(noOfLinksInCell[xIndex][yIndex] >= minimumNoOfLinksInCell){
						if(isInMunichShape(cellCentroid)){
					//	if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
							
//							ddouble averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / sumOfweightsForCell[xIndex][yIndex]; // average of vehkm per cell
							
//							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m); // sum of vehkm per cell normalized to vehkm per m²
							
							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) * 1000. * 1000.; // sum of vehkm per cell normalized to vehkm per km²
							
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
					}
				//	}
				}
			}
		}
		ShapeFileWriter.writeGeometries(features, outPathStub + ".congestionTime.movie.demandPerLinkSmoothed.shp");
		logger.info("Finished writing output to " + outPathStub + ".congestionTime.movie.demandPerLinkSmoothed.shp");
//		ShapeFileWriter.writeGeometries(features, outPathStub + ".vehKmPerKmSquare.movie.demandPerLinkSmoothed.shp");
//		logger.info("Finished writing output to " + outPathStub + ".vehKmPerKmSquare.movie.demandPerLinkSmoothed.shp");
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
	
	
	private Map<Double, Map<Id,  Double>> calculateRelativeDemandDifferences(
			Map<Double, Map<Id, Integer>> time2Demand1,
			Map<Double, Map<Id, Integer>> time2Demand2) {

		Map<Double, Map<Id, Double>> time2RelativeDelta = new HashMap<Double, Map<Id, Double>>();
		for(Entry<Double, Map<Id, Integer>> entry0 : time2Demand1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Integer> linkId2Demand = entry0.getValue();
			Map<Id, Double> delta = new HashMap<Id, Double>();

			for(Entry<Id, Integer> entry1 : linkId2Demand.entrySet()){
				Id linkId = entry1.getKey();
				double demandDifferenceRatio;
				int demandBefore = entry1.getValue();
				int demandAfter = time2Demand2.get(endOfTimeInterval).get(linkId);
				if (demandBefore == 0){// cannot calculate relative change if "before" value is 0 ...
					logger.warn("setting demand in baseCase for link " + linkId + " from " + demandBefore + " to 1 ...");
					demandBefore = 1;
				} else {
					// do nothing
				}
				
				demandDifferenceRatio = (demandAfter - demandBefore) / demandBefore;
				
//				double linkLength = this.network.getLinks().get(linkId).getLength();
//				demandDifferenceRatio = ((demandAfter / linkLength) - (demandBefore / linkLength)) / (demandBefore / linkLength);
				
//				double linkFlowCap = this.network.getLinks().get(linkId).getCapacity();
//				demandDifferenceRatio = ((demandAfter / linkFlowCap) - (demandBefore / linkFlowCap)) / (demandBefore / linkFlowCap);
				
				delta.put(linkId, demandDifferenceRatio);
			}
			time2RelativeDelta.put(endOfTimeInterval, delta);
		}
		return time2RelativeDelta;
	}

	private Map<Double, Map<Id, Double>> calcualateAbsoluteDemandDifferences(
			Map<Double, Map<Id, Integer>> time2Demand1,
			Map<Double, Map<Id, Integer>> time2Demand2) {
		
		Map<Double, Map<Id, Double>> time2AbsoluteDelta = new HashMap<Double, Map<Id, Double>>();
		for(Entry<Double, Map<Id, Integer>> entry0 : time2Demand1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Integer> linkId2Demand = entry0.getValue();
			Map<Id, Double> delta = new HashMap<Id, Double>();
			
			for(Entry<Id, Integer> entry1 : linkId2Demand.entrySet()){
				Id linkId = entry1.getKey();
				int demandBefore = entry1.getValue();
				int demandAfter = time2Demand2.get(endOfTimeInterval).get(linkId);

//				double demandDifference = demandAfter - demandBefore;

				double linkLength = this.network.getLinks().get(linkId).getLength();
				double demandDifference= ((demandAfter * linkLength) - (demandBefore * linkLength));
				
				delta.put(linkId, demandDifference);
			}
			time2AbsoluteDelta.put(endOfTimeInterval, delta);
		}
		return time2AbsoluteDelta;
	}

	private Map<Double, Map<Id, Double>> calculateRelativeDemandDifferencesDouble(
			Map<Double, Map<Id, Double>> time2Demand1,
			Map<Double, Map<Id, Double>> time2Demand2) {

		Map<Double, Map<Id, Double>> time2RelativeDelta = new HashMap<Double, Map<Id, Double>>();
		for(Entry<Double, Map<Id, Double>> entry0 : time2Demand1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Double> linkId2Demand = entry0.getValue();
			Map<Id, Double> delta = new HashMap<Id, Double>();

			for(Entry<Id, Double> entry1 : linkId2Demand.entrySet()){
				Id linkId = entry1.getKey();
				double demandBefore = entry1.getValue();
				double demandAfter = time2Demand2.get(endOfTimeInterval).get(linkId);
				if (demandBefore == 0.0){// cannot calculate relative change if "before" value is 0 ...
					logger.warn("setting demand in baseCase for link " + linkId + " from " + demandBefore + " to 1.0 ...");
					demandBefore = 1;
				} else {
					// do nothing
				}
//				double demandDifferenceRatio = (demandAfter - demandBefore) / demandBefore;
//				delta.put(linkId, demandDifferenceRatio);
				
				//===
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
				
				double congestionTimePerVehKmBefore; 
				double countBefore = this.time2Counts1.get(endOfTimeInterval).get(linkId);
				if(countBefore == 0.){
					countBefore = 1.;
					logger.warn("setting count in baseCase for link " + linkId + " from " + countBefore + " to 1.0 ...");
				}
				congestionTimePerVehKmBefore = demandBefore / (countBefore * linkLength_km);
				
				double congestionTimePerVehKmAfter;
				double countAfter = this.time2Counts2.get(endOfTimeInterval).get(linkId);
				if(countAfter == 0.){
					countAfter = 1.;
					logger.warn("setting count in policyCase for link " + linkId + " from " + countAfter + " to 1.0 ...");
				}
				congestionTimePerVehKmAfter = demandAfter / (countAfter * linkLength_km);

				double demandDifferenceRatio = (congestionTimePerVehKmAfter - congestionTimePerVehKmBefore) / congestionTimePerVehKmBefore;
				delta.put(linkId, demandDifferenceRatio);
			}
			time2RelativeDelta.put(endOfTimeInterval, delta);
		}
		return time2RelativeDelta;
	}

	private Map<Double, Map<Id, Double>> calcualateAbsoluteEmissionDifferencesDouble(
			Map<Double, Map<Id, Double>> time2Demand1,
			Map<Double, Map<Id, Double>> time2Demand2) {
		
		Map<Double, Map<Id, Double>> time2AbsoluteDelta = new HashMap<Double, Map<Id, Double>>();
		for(Entry<Double, Map<Id, Double>> entry0 : time2Demand1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Double> linkId2Demand = entry0.getValue();
			Map<Id, Double> delta = new HashMap<Id, Double>();
			
			for(Entry<Id, Double> entry1 : linkId2Demand.entrySet()){
				Id linkId = entry1.getKey();
				double demandBefore = entry1.getValue();
				double demandAfter = time2Demand2.get(endOfTimeInterval).get(linkId);

//				double demandDifference = demandAfter - demandBefore;
				
				//===
				double linkLength = this.network.getLinks().get(linkId).getLength() / 1000.;
				double demandDifference= ((demandAfter * linkLength) - (demandBefore * linkLength));
				delta.put(linkId, demandDifference);

				//===
//				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
//				
//				double congestionTimePerVehKmBefore; 
//				int countBefore = this.time2Counts1.get(endOfTimeInterval).get(linkId);
//				if(countBefore != 0){
//					congestionTimePerVehKmBefore = demandBefore / (countBefore * linkLength_km);
//				} else {
//					congestionTimePerVehKmBefore = 0.0;
//				}
//				
//				double congestionTimePerVehKmAfter;
//				double countAfter = this.time2Counts2.get(endOfTimeInterval).get(linkId);
//				if(countAfter != 0){
//					congestionTimePerVehKmAfter = demandAfter / (countAfter * linkLength_km);
//				} else {
//					congestionTimePerVehKmAfter = 0.0;
//				}
//
//				double congestionTimePerVehKmDifference = congestionTimePerVehKmAfter - congestionTimePerVehKmBefore;
//				delta.put(linkId, congestionTimePerVehKmDifference);
			}
			
			time2AbsoluteDelta.put(endOfTimeInterval, delta);
		}
		return time2AbsoluteDelta;
	}

	private Map<Double, Map<Id, Double>> convertMapToDoubleValues(
			Map<Double, Map<Id, Integer>> time2Demand) {
		Map<Double, Map<Id, Double>> mapOfDoubleValues = new HashMap<Double, Map<Id, Double>>();
		for(Double endOfTimeInterval : time2Demand.keySet()){
			Map<Id, Integer> linkId2Value = time2Demand.get(endOfTimeInterval);
			Map<Id, Double> linkId2DoubleValue = new HashMap<Id, Double>();
			for(Id linkId : linkId2Value.keySet()){
				int intValue = linkId2Value.get(linkId);
				double doubleValue = intValue;
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
				double vehicleKm = doubleValue * linkLength_km;
				linkId2DoubleValue.put(linkId, vehicleKm);
//				linkId2DoubleValue.put(linkId, doubleValue);
			}
			mapOfDoubleValues.put(endOfTimeInterval, linkId2DoubleValue);
		}
		return mapOfDoubleValues;
	}

	private Map<Double, Map<Id, Integer>> setNonCalculatedDemandAndFilter(Map<Double, Map<Id, Integer>> time2Demand) {
		Map<Double, Map<Id, Integer>> time2DemandFiltered = new HashMap<Double, Map<Id, Integer>>();

		for(Double endOfTimeInterval : time2Demand.keySet()){
			Map<Id, Integer> linkId2Demand = time2Demand.get(endOfTimeInterval);
			Map<Id, Integer> linkId2DemandFiltered = new HashMap<Id, Integer>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
							if(linkId2Demand.get(linkId) != null){
								int demand = linkId2Demand.get(linkId);
								linkId2DemandFiltered.put(linkId, demand);
							} else { // setting demand for all links that were not in map to 0
								linkId2DemandFiltered.put(linkId, 0);
							}
					}
				}
			}					
		time2DemandFiltered.put(endOfTimeInterval, linkId2DemandFiltered);
	}
	return time2DemandFiltered;
}

	private Map<Double, Map<Id, Integer>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id, Integer>> time2Counts1) {
		Map<Double, Map<Id, Integer>> time2CountsTotalFiltered = new HashMap<Double, Map<Id,Integer>>();

		for(Double endOfTimeInterval : time2Counts1.keySet()){
			Map<Id, Integer> linkId2Count = time2Counts1.get(endOfTimeInterval);
			Map<Id, Integer> linkId2CountFiltered = new HashMap<Id, Integer>();
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
						if(linkId2Count.get(linkId) == null){
							linkId2CountFiltered.put(linkId, 0);
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
	
	private Map<Double, Map<Id, Double>> setNonCalculatedDemandAndFilterDouble(Map<Double, Map<Id, Double>> time2Demand) {
		Map<Double, Map<Id, Double>> time2DemandFiltered = new HashMap<Double, Map<Id, Double>>();

		for(Double endOfTimeInterval : time2Demand.keySet()){
			Map<Id, Double> linkId2Demand = time2Demand.get(endOfTimeInterval);
			Map<Id, Double> linkId2DemandFiltered = new HashMap<Id, Double>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > xMin && xLink < xMax){
					if(yLink > yMin && yLink < yMax){
						Id linkId = link.getId();
							if(linkId2Demand.get(linkId) != null){
								double demand = linkId2Demand.get(linkId);
								linkId2DemandFiltered.put(linkId, demand);
							} else { // setting demand for all links that were not in map to 0
								linkId2DemandFiltered.put(linkId, 0.0);
							}
					}
				}
			}					
		time2DemandFiltered.put(endOfTimeInterval, linkId2DemandFiltered);
	}
	return time2DemandFiltered;
	}

	private void processEvents(String eventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
//		this.weightedDemandPerLinkHandler = new WeightedDemandPerLinkHandler(this.network, this.simulationEndTime, noOfTimeBins);
//		eventsManager.addHandler(this.weightedDemandPerLinkHandler);
		this.demandHandler = new DemandPerLinkHandler(this.network, this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.demandHandler);
		reader.readFile(eventsFile);
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
		AttributeType demand = AttributeTypeFactory.newAttributeType(
				"Congestion", Double.class);
		
		Exception ex;
		try {
			this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[]
			        {point, time, demand}, "DemandPoint");
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
		new SpatialAveragingForLinkDemand().run();
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
