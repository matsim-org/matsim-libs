package playground.andreas.aas.modules.spatialAveragingLinkDemand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.andreas.aas.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;

/**
 * @author aneumann, benjamin after fhuelsmann
 *
 */
public class SpatialAveragingForLinkDemand extends AbstractAnalyisModule{

	private final static Logger log = Logger.getLogger(SpatialAveragingForLinkDemand.class);

	private ScenarioImpl scenario;
	private Collection<SimpleFeature> shapeFile;
	private int noOfTimeBins = 1;
	
	private double simulationEndTime;
	private DemandPerLinkHandler demandHandler; 
	private ArrayList<SimpleFeature> resultingFeatures;
	
	// This should be configurable by stating a grid size
	private final int noOfXbins;
	private final int noOfYbins;
	private final int minimumNoOfLinksInCell;
	private final double smoothingRadius_m;
	private final String date;
	private final double xMin;
	private final double xMax;
	private final double yMin;
	private final double yMax;
	private final CoordinateReferenceSystem targetCRS;

	public SpatialAveragingForLinkDemand(String ptDriverPrefix) {
		super(SpatialAveragingForLinkDemand.class.getSimpleName(), ptDriverPrefix);
		log.info("enabled");
		
		// This should be configurable by stating a grid size
		this.noOfXbins = 160;
		this.noOfYbins = 120;
		
		// Make it configurable - What is really needed?
		this.minimumNoOfLinksInCell = 0;
		this.smoothingRadius_m = 500.0;
		this.date = "2012-04-13 ";
		this.xMin = 4452550.25;
		this.xMax = 4479483.33;
		this.yMin = 5324955.00;
		this.yMax = 5345696.81;
		this.targetCRS = MGC.getCRS("EPSG:20004");
	}
	
	public void init(ScenarioImpl scenario, Collection<SimpleFeature> shapeFile, int noOfTimeBins){
		this.scenario = scenario;
		this.shapeFile = shapeFile;
		this.noOfTimeBins = noOfTimeBins;
	}
	
	@Override
	public void preProcessData() {
		this.simulationEndTime = this.scenario.getConfig().qsim().getEndTime();
		log.info("Simulation end time is: " + this.simulationEndTime / 3600 + " hours.");
		log.info("Aggregating emissions for " + (int) (this.simulationEndTime / 3600 / this.noOfTimeBins) + " hour time bins.");
		
		this.demandHandler = new DemandPerLinkHandler(this.scenario.getNetwork(), this.simulationEndTime, this.noOfTimeBins);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.demandHandler);
		return handler;
	}

	@Override
	public void postProcessData() {
		
		SimpleFeatureType featureType = createFeatures();
		
		Map<Double, Map<Id<Link>, Integer>> time2Counts1 = setNonCalculatedCountsAndFilter(this.demandHandler.getDemandPerLinkAndTimeInterval());
		this.demandHandler.reset(0); // really needed?!
		
		Map<Double, Map<Id<Link>, Double>> time2DemandMapToAnalyze = convertMapToDoubleValues(time2Counts1);
		this.resultingFeatures = new ArrayList<SimpleFeature>();

		for(double endOfTimeInterval : time2DemandMapToAnalyze.keySet()){
			Map<Id<Link>, Double> demandMapToAnalyze = time2DemandMapToAnalyze.get(endOfTimeInterval);

			int[][] noOfLinksInCell = new int[this.noOfXbins][this.noOfYbins];
			double[][] sumOfweightsForCell = new double[this.noOfXbins][this.noOfYbins];
			double[][] sumOfweightedValuesForCell = new double[this.noOfXbins][this.noOfYbins];
			
			for(Link link : this.scenario.getNetwork().getLinks().values()){
				
				if (demandMapToAnalyze.containsKey(link.getId())){
					
					Id<Link> linkId = link.getId();
					Coord linkCoord = link.getCoord();
					double xLink = linkCoord.getX();
					double yLink = linkCoord.getY();

					Integer xbin = mapXCoordToBin(xLink);
					Integer ybin = mapYCoordToBin(yLink);
					if ( xbin != null && ybin != null ){

						noOfLinksInCell[xbin][ybin] ++;
					
						for(int xIndex = 0; xIndex < this.noOfXbins; xIndex++){
							for(int yIndex = 0; yIndex < this.noOfYbins; yIndex++){
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
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
			for(int xIndex = 0; xIndex < this.noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex < this.noOfYbins; yIndex++){
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(noOfLinksInCell[xIndex][yIndex] >= this.minimumNoOfLinksInCell){
						if(coordIsInShape(cellCentroid)){
							
							double averageValue = sumOfweightedValuesForCell[xIndex][yIndex] / (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) * 1000. * 1000.; // sum of vehkm per cell normalized to vehkm per square km
							
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
						
							Point point = MGC.xy2Point(cellCentroid.getX(), cellCentroid.getY());
							try {
								SimpleFeature feature = builder.buildFeature(null, new Object[] {
										point, dateTimeString, averageValue
								});
								this.resultingFeatures.add(feature);
							} catch (IllegalArgumentException e1) {
								throw new RuntimeException(e1);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		String outFile = outputFolder + "congestionTime.movie.demandPerLinkSmoothed.shp";
		ShapeFileWriter.writeGeometries(this.resultingFeatures, outFile);
		log.info("Finished writing output to " + outFile);
	}

	private boolean coordIsInShape(Coord cellCentroid) {
		boolean isInShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.shapeFile){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}

	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = this.date + time;
		return dateTimeString;
	}

	private double calculateWeightOfPersonForCell(double x1, double y1, double x2, double y2) {
		double distance = Math.abs(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
		return Math.exp((-distance * distance) / (this.smoothingRadius_m * this.smoothingRadius_m));
	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = this.yMin + ((yIndex + .5) / this.noOfYbins) * (this.yMax - this.yMin);
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = this.xMin + ((xIndex + .5) / this.noOfXbins) * (this.xMax - this.xMin);
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
		if (yCoord <= this.yMin || yCoord >= this.yMax) return null; // yHome is not in area of interest
		double relativePositionY = ((yCoord - this.yMin) / (this.yMax - this.yMin) * this.noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= this.xMin || xCoord >= this.xMax) return null; // xHome is not in area of interest
		double relativePositionX = ((xCoord - this.xMin) / (this.xMax - this.xMin) * this.noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	/**
	 * Is this really necessary?! AN
	 * @param time2Counts1
	 * @return
	 */
	private Map<Double, Map<Id<Link>, Double>> convertMapToDoubleValues(Map<Double, Map<Id<Link>, Integer>> time2Counts1) {
		Map<Double, Map<Id<Link>, Double>> mapOfDoubleValues = new HashMap<>();
		for(Double endOfTimeInterval : time2Counts1.keySet()){
			Map<Id<Link>, Integer> linkId2Value = time2Counts1.get(endOfTimeInterval);
			Map<Id<Link>, Double> linkId2DoubleValue = new HashMap<>();
			for(Id<Link> linkId : linkId2Value.keySet()){
				int intValue = linkId2Value.get(linkId);
				double doubleValue = intValue;
				double linkLength_km = this.scenario.getNetwork().getLinks().get(linkId).getLength() / 1000.;
				double vehicleKm = doubleValue * linkLength_km;
				linkId2DoubleValue.put(linkId, vehicleKm);
			}
			mapOfDoubleValues.put(endOfTimeInterval, linkId2DoubleValue);
		}
		return mapOfDoubleValues;
	}

	private Map<Double, Map<Id<Link>, Integer>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id<Link>, Integer>> time2Counts1) {
		Map<Double, Map<Id<Link>, Integer>> time2CountsTotalFiltered = new HashMap<>();

		for(Double endOfTimeInterval : time2Counts1.keySet()){
			Map<Id<Link>, Integer> linkId2Count = time2Counts1.get(endOfTimeInterval);
			Map<Id<Link>, Integer> linkId2CountFiltered = new HashMap<>();
			for(Link link : this.scenario.getNetwork().getLinks().values()){
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				if(xLink > this.xMin && xLink < this.xMax){
					if(yLink > this.yMin && yLink < this.yMax){
						Id<Link> linkId = link.getId();
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
	
	private SimpleFeatureType createFeatures() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("DemandPoint");
		b.setCRS(this.targetCRS);
		b.add("location", Point.class);
		b.add("Time", String.class);
		b.add("Congestion", Double.class);
		return b.buildFeatureType();
	}
}
