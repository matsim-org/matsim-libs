package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.ConfigurationModule;
import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

public class AccessibilityControlerListenerTemplate{
	
	protected static final Logger log = Logger.getLogger(AccessibilityControlerListenerTemplate.class);
	public static final String SHAPE_FILE = "SF";
	public static final String NETWORK = "NW";
	protected static String fileExtension;
	protected boolean isParcelMode = false;
	
	// start points, measuring accessibility (cell based approach)
	protected ZoneLayer<CounterObject> measuringPoints;
	// start points, measuring accessibility (zone based approach)
	protected ActivityFacilitiesImpl zones;
	// containing parcel coordinates for accessibility feedback
	protected ActivityFacilitiesImpl parcels; 
	// destinations, opportunities like jobs etc ...
	protected AggregateObject2NearestNode[] aggregatedOpportunities;
	
	// storing the accessibility results
	protected SpatialGrid freeSpeedGrid;
	protected SpatialGrid carGrid;
	protected SpatialGrid bikeGrid;
	protected SpatialGrid walkGrid;
	
	// accessibility parameter
	protected boolean useRawSum	= false;
	protected double logitScaleParameter;
	protected double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	protected double betaCarTTPower;
	protected double betaCarLnTT;
	protected double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	protected double betaCarTDPower;
	protected double betaCarLnTD;
	protected double betaCarTC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaCarTCPower;
	protected double betaCarLnTC;
	protected double betaWalkTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	protected double betaWalkTTPower;
	protected double betaWalkLnTT;
	protected double betaWalkTD;		// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
	protected double betaWalkTDPower;
	protected double betaWalkLnTD;
	protected double betaWalkTC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaWalkTCPower;
	protected double betaWalkLnTC;
	
	protected double carTT, carTTPower, carLnTT, carTD, carTDPower, carLnTD, carTC, carTCPower, carLnTC,
		   walkTT, walkTTPower, walkLnTT, walkTD, walkTDPower, walkLnTD, walkTC, walkTCPower, walkLnTC,
		   bikeTT, bikeTTPower, bikeLnTT, bikeTD, bikeTDPower, bikeLnTD, bikeTC, bikeTCPower, bikeLnTC,
		   freeTT, freeTTPower, freeLnTT, freeTD, freeTDPower, freeLnTD, freeTC, freeTCPower, freeLnTC;
	
	protected double depatureTime;
	protected double walkSpeedMeterPerSecond = -1;
	protected double bikeSpeedMeterPerHour = -1;
	protected double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;

	/**
	 * returns an util value for given betas and travel costs/offset
	 * 
	 * @param betaCarX
	 * @param CarTravelCostX
	 * @param betaWalkX
	 * @param walkOffsetX
	 * @return
	 */
	protected double getAsUtilCar(final double betaCarX, final double CarTravelCostX, final double betaWalkX, final double walkOffsetX){
		if(betaCarX != 0.)
			return (betaCarX * CarTravelCostX + betaWalkX * walkOffsetX);
		return 0.;
	}
	
	/**
	 * returns an util value for given beta and travel costs+offset
	 * 
	 * @param betaWalkX
	 * @param walkTravelCostWithOffest
	 * @return
	 */
	protected double getAsUtilWalk(final double betaWalkX, final double walkTravelCostWithOffest){
		if(betaWalkX != 0.)
			return (betaWalkX * walkTravelCostWithOffest);
		return 0.;
	}
	
	/**
	 * setting parameter for accessibility calculation
	 * @param scenario
	 */
	protected void initAccessibilityParameter(ScenarioImpl scenario){
		
		AccessibilityParameterConfigModule module = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		
		useRawSum		= module.isUseRawSumsWithoutLn();
		logitScaleParameter = module.getLogitScaleParameter();
		walkSpeedMeterPerSecond = scenario.getConfig().plansCalcRoute().getWalkSpeed();
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 3600.;
		bikeSpeedMeterPerHour = 15000.;
		
		betaCarTT 	   	= module.getBetaCarTravelTime();
		betaCarTTPower	= module.getBetaCarTravelTimePower2();
		betaCarLnTT		= module.getBetaCarLnTravelTime();
		betaCarTD		= module.getBetaCarTravelDistance();
		betaCarTDPower	= module.getBetaCarTravelDistancePower2();
		betaCarLnTD		= module.getBetaCarLnTravelDistance();
		betaCarTC		= module.getBetaCarTravelCost();
		betaCarTCPower	= module.getBetaCarTravelCostPower2();
		betaCarLnTC		= module.getBetaCarLnTravelCost();
		
		betaWalkTT		= module.getBetaWalkTravelTime();
		betaWalkTTPower	= module.getBetaWalkTravelTimePower2();
		betaWalkLnTT	= module.getBetaWalkLnTravelTime();
		betaWalkTD		= module.getBetaWalkTravelDistance();
		betaWalkTDPower	= module.getBetaWalkTravelDistancePower2();
		betaWalkLnTD	= module.getBetaWalkLnTravelDistance();
		betaWalkTC		= module.getBetaWalkTravelCost();
		betaWalkTCPower	= module.getBetaWalkTravelCostPower2();
		betaWalkLnTC	= module.getBetaWalkLnTravelCost();
		
		depatureTime 	= 8.*3600;	// tnicolai: make configurable		
		printParameterSettings();
	}
	
	/**
	 * displays settings
	 */
	protected void printParameterSettings(){
		log.info("Computing and writing grid based accessibility measures with following settings:" );
		log.info("Returning raw sum (not logsum): " + useRawSum);
		log.info("Logit Scale Parameter: " + logitScaleParameter);
		log.info("Bike speed (meter/h): " + this.bikeSpeedMeterPerHour);
		log.info("Walk speed (meter/h): " + this.walkSpeedMeterPerHour + " ("+this.walkSpeedMeterPerSecond+"meter/s)");
		log.info("Depature time (in seconds): " + depatureTime);
		log.info("Beta Car Travel Time: " + betaCarTT );
		log.info("Beta Car Travel Time Power2: " + betaCarTTPower );
		log.info("Beta Car Ln Travel Time: " + betaCarLnTT );
		log.info("Beta Car Travel Distance: " + betaCarTD );
		log.info("Beta Car Travel Distance Power2: " + betaCarTDPower );
		log.info("Beta Car Ln Travel Distance: " + betaCarLnTD );
		log.info("Beta Car Travel Cost: " + betaCarTC );
		log.info("Beta Car Travel Cost Power2: " + betaCarTCPower );
		log.info("Beta Car Ln Travel Cost: " + betaCarLnTC );
		log.info("Beta Walk Travel Time: " + betaWalkTT );
		log.info("Beta Walk Travel Time Power2: " + betaWalkTTPower );
		log.info("Beta Walk Ln Travel Time: " + betaWalkLnTT );
		log.info("Beta Walk Travel Distance: " + betaWalkTD );
		log.info("Beta Walk Travel Distance Power2: " + betaWalkTDPower );
		log.info("Beta Walk Ln Travel Distance: " + betaWalkLnTD );
		log.info("Beta Walk Travel Cost: " + betaWalkTC );
		log.info("Beta Walk Travel Cost Power2: " + betaWalkTCPower );
		log.info("Beta Walk Ln Travel Cost: " + betaWalkLnTC );
	}
	
	/**
	 * This method expects the nearest link to a given measure point. 
	 * It calculates the eucclidian distance for both nodes of the link, 
	 * "fromNode" and "toNode" and returns the node with shorter distance
	 * 
	 * @param coordFromZone
	 * @param nearestLink
	 */
	protected Node getNearestNode(Coord coordFromZone, Link nearestLink) {
		Node toNode = nearestLink.getToNode();
		Node fromNode= nearestLink.getFromNode();
		
		double distanceToNode = NetworkUtil.getEuclidianDistance(coordFromZone, toNode.getCoord());
		double distanceFromNode=NetworkUtil.getEuclidianDistance(coordFromZone, fromNode.getCoord());
		
		if(distanceToNode < distanceFromNode)
			return toNode;
		return fromNode;
	}
//	
//	protected ZoneLayer<CounterObject> createTestPoints(){
//		
//		GeometryFactory factory = new GeometryFactory();
//		Set<Zone<CounterObject>> zones = new HashSet<Zone<CounterObject>>();
//		int setPoints = 1;
//		int srid = InternalConstants.SRID_SWITZERLAND;
//		int gridSize = 10;
//		
//		Point point1 = factory.createPoint(new Coordinate(680699.1, 250976.0)); // oben links
//		Point point2 = factory.createPoint(new Coordinate(681410.0, 250670.0)); // oben mitte
//		Point point3 = factory.createPoint(new Coordinate(682419.0, 250232.0)); // oben rechts
//		Point point4 = factory.createPoint(new Coordinate(680602.2, 250934.2)); // unten links
//		
//		createCell(factory, zones, point1, setPoints++, srid, gridSize);
//		createCell(factory, zones, point2, setPoints++, srid, gridSize);
//		createCell(factory, zones, point3, setPoints++, srid, gridSize);
//		createCell(factory, zones, point4, setPoints++, srid, gridSize);
//		
//		ZoneLayer<CounterObject> layer = new ZoneLayer<CounterObject>(zones);
//		return layer;
//	}
//
//	/**
//	 * This is for testing purposes only
//	 * 
//	 * @param factory
//	 * @param zones
//	 * @param setPoints
//	 * @param srid
//	 */
//	private void createCell(GeometryFactory factory, Set<Zone<CounterObject>> zones, Point point, int setPoints, int srid, int gridSize) {
//		
//		double x = point.getCoordinate().x;
//		double y = point.getCoordinate().y;
//		
//		Coordinate[] coords = new Coordinate[5];
//		coords[0] = new Coordinate(x-gridSize, y-gridSize); 	// links unten
//		coords[1] = new Coordinate(x-gridSize, y + gridSize);	// links oben
//		coords[2] = new Coordinate(x + gridSize, y + gridSize);	// rechts oben
//		coords[3] = new Coordinate(x + gridSize, y-gridSize);	// rechts unten
//		coords[4] = new Coordinate(x-gridSize, y-gridSize); 	// links unten
//		// Linear Ring defines an artificial zone
//		LinearRing linearRing = factory.createLinearRing(coords);
//		Polygon polygon = factory.createPolygon(linearRing, null);
//		polygon.setSRID( srid ); 
//		
//		Zone<CounterObject> zone = new Zone<CounterObject>(polygon);
//		zone.setAttribute( new CounterObject( setPoints ) );
//		zones.add(zone);
//	}

}
