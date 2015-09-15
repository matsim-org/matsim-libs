package playground.andreas.aas.modules.cellBasedAccessibility;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import playground.andreas.aas.modules.cellBasedAccessibility.config.AccessibilityParameterConfigModule;
import playground.andreas.aas.modules.cellBasedAccessibility.config.ConfigurationModule;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.SpatialGrid;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.Zone;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.ZoneLayer;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects.AggregateObject2NearestNode;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects.Benchmark;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects.Distances;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.misc.ProgressBar;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.network.NetworkUtil;

import com.vividsolutions.jts.geom.Point;

/**
 * improvements aug'12
 * - accessibility calculation of unified for cell- and zone-base approach
 * 
 * 
 * @author thomas
 *
 */
public class AccessibilityControlerListenerImpl{
	
	protected static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);
	public static final String SHAPE_FILE = "SF";
	public static final String NETWORK = "NW";
	protected static String fileExtension;
	protected boolean isParcelMode = false;
	
	protected static int ZONE_BASED = 0;
	protected static int CELL_BASED = 1;
	
	// start points, measuring accessibility (cell based approach)
	protected ZoneLayer<Id> measuringPointsCell;
	// start points, measuring accessibility (zone based approach)
	protected ZoneLayer<Id> measuringPointsZone;
	protected ActivityFacilitiesImpl zones; // tnicolai: this is old! replace!!!
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
	protected double inverseOfLogitScaleParameter;
	protected double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	protected double betaCarTTPower;
	protected double betaCarLnTT;
	protected double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	protected double betaCarTDPower;
	protected double betaCarLnTD;
	protected double betaCarTC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaCarTCPower;
	protected double betaCarLnTC;
	protected double betaBikeTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	protected double betaBikeTTPower;
	protected double betaBikeLnTT;
	protected double betaBikeTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist: 
	protected double betaBikeTDPower;
	protected double betaBikeLnTD;
	protected double betaBikeTC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaBikeTCPower;
	protected double betaBikeLnTC;
	protected double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	protected double betaWalkTTPower;
	protected double betaWalkLnTT;
	protected double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
	protected double betaWalkTDPower;
	protected double betaWalkLnTD;
	protected double betaWalkTC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaWalkTCPower;
	protected double betaWalkLnTC;
	
	protected double carTT, carTTPower, carLnTT, carTD, carTDPower, carLnTD, carTC, carTCPower, carLnTC,
		   walkTT, walkTTPower, walkLnTT, walkTD, walkTDPower, walkLnTD, walkTC, walkTCPower, walkLnTC,
		   bikeTT, bikeTTPower, bikeLnTT, bikeTD, bikeTDPower, bikeLnTD, bikeTC, bikeTCPower, bikeLnTC,
		   freeTT, freeTTPower, freeLnTT, freeTD, freeTDPower, freeLnTD, freeTC, freeTCPower, freeLnTC;
	
	protected double depatureTime;
	protected double bikeSpeedMeterPerHour = -1;
	protected double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;

	/**
	 * setting parameter for accessibility calculation
	 * @param scenario
	 */
	protected void initAccessibilityParameter(ScenarioImpl scenario){
		
		AccessibilityParameterConfigModule module = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		
		useRawSum		= module.isUseRawSumsWithoutLn();
		logitScaleParameter = module.getLogitScaleParameter();
		inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
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
		
		betaBikeTT		= module.getBetaBikeTravelTime();
		betaBikeTTPower	= module.getBetaBikeTravelTimePower2();
		betaBikeLnTT	= module.getBetaBikeLnTravelTime();
		betaBikeTD		= module.getBetaBikeTravelDistance();
		betaBikeTDPower	= module.getBetaBikeTravelDistancePower2();
		betaBikeLnTD	= module.getBetaBikeLnTravelDistance();
		betaBikeTC		= module.getBetaBikeTravelCost();
		betaBikeTCPower	= module.getBetaBikeTravelCostPower2();
		betaBikeLnTC	= module.getBetaBikeLnTravelCost();
		
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
		log.info("Inverse of logit Scale Parameter: " + inverseOfLogitScaleParameter);
		log.info("Bike speed (meter/h): " + this.bikeSpeedMeterPerHour);
		log.info("Walk speed (meter/h): " + this.walkSpeedMeterPerHour + " ("+this.walkSpeedMeterPerHour/3600. +"meter/s)");
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
		log.info("Beta Bike Travel Time: " + betaBikeTT );
		log.info("Beta Bike Travel Time Power2: " + betaBikeTTPower );
		log.info("Beta Bike Ln Travel Time: " + betaBikeLnTT );
		log.info("Beta Bike Travel Distance: " + betaBikeTD );
		log.info("Beta Bike Travel Distance Power2: " + betaBikeTDPower );
		log.info("Beta Bike Ln Travel Distance: " + betaBikeLnTD );
		log.info("Beta Bike Travel Cost: " + betaBikeTC );
		log.info("Beta Bike Travel Cost Power2: " + betaBikeTCPower );
		log.info("Beta Bike Ln Travel Cost: " + betaBikeLnTC );
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
	 * @param ttc
	 * @param lcptFreeSpeedCarTravelTime
	 * @param lcptCongestedCarTravelTime
	 * @param lcptTravelDistance
	 * @param network
	 * @param inverseOfLogitScaleParameter
	 * @param accCsvWriter
	 * @param measuringPointIterator
	 */
	protected void accessibilityComputation(TravelTime ttc,
			LeastCostPathTree lcptFreeSpeedCarTravelTime,
			LeastCostPathTree lcptCongestedCarTravelTime,
			LeastCostPathTree lcptTravelDistance, NetworkImpl network,
			Iterator<Zone<Id>> measuringPointIterator,
			int size, int mode) {

		ProgressBar bar = new ProgressBar( size );
		
		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
//			// tnicolai: only for testing, disable afterwards
//			ZoneLayer<Id> testSet = createTestPoints();
//			measuringPointIterator = testSet.getZones().iterator();

		// iterates through all starting points (fromZone) and calculates their accessibility, e.g. to jobs
		while( measuringPointIterator.hasNext() ){
			
			bar.update();
			
			Zone<Id> measurePoint = measuringPointIterator.next();
			
			Point point = measurePoint.getGeometry().getCentroid();
			// get coordinate from origin (start point)
			Coord coordFromZone = new Coord(point.getX(), point.getY());
			assert( coordFromZone!=null );
			
			// from here: accessibility computation for current starting point ("fromNode")
			
			// captures the distance (as walk time) between a cell centroid and the road network
			Link nearestLink = network.getNearestLinkExactly(coordFromZone);

			// determine nearest network node (from- or toNode) based on the link 
			Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
			assert( fromNode != null );
			
			// run dijkstra on network
			lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
			lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
			lcptTravelDistance.calculate(network, fromNode, depatureTime);
			
			// captures the distance (as walk time) between a zone centroid and its nearest node
			
			Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);
			
			double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
			double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
			
			double offsetWalkTime2Node_h 				= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;
			double carTravelTime_meterpersec			= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
			double freeSpeedTravelTime_meterpersec 		= nearestLink.getFreespeed();
			
			double offsetFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTime_meterpersec * 3600);
			double offsetCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTime_meterpersec * 3600.);
			double offsetBikeTime_h						= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
			

			// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
			// Euclidean Distance (measuring point 2 nearest node):
			// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
			// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
			// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
			// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
			// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
			
			gcs.reset();

			// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
			for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
				
				// add the avg. distance of all aggregated opportunities (euclidiean distance from nearest node to opportunity)
				double averageDistanceRoad2Opportunitiy_meter = this.aggregatedOpportunities[i].getAverageDistance();
				double offsetWalkTime2Opportunity_h = averageDistanceRoad2Opportunitiy_meter / this.walkSpeedMeterPerHour;
				
				// get stored network node (this is the nearest node next to an aggregated work place)
				Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
				Id nodeID = destinationNode.getId();
				
				// using number of aggregated opportunities as weight for log sum measure
				int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

				// free speed car travel times in hours
				double freeSpeedTravelTime_h= (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + offsetFreeSpeedTime_h;
				// travel distance in meter
				double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
				// bike travel times in hours
				double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + offsetBikeTime_h; // using a constant speed of 15km/h
				// walk travel times in hours
				double walkTravelTime_h		= travelDistance_meter / this.walkSpeedMeterPerHour;
				// congested car travel times in hours
				double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
				double congestedCarTravelTime_h = ((arrivalTime - depatureTime) / 3600.) + offsetCongestedCarTime_h;
				
				sumGeneralizedCosts(gcs, 
						distanceMeasuringPoint2Road_meter + averageDistanceRoad2Opportunitiy_meter,
						distanceRoad2Node_meter, 
						offsetWalkTime2Node_h + offsetWalkTime2Opportunity_h,
						opportunityWeight, freeSpeedTravelTime_h,
						travelDistance_meter, bikeTravelTime_h,
						walkTravelTime_h, congestedCarTravelTime_h);
			}
			
			// aggregated value
			double freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility;
			if(!useRawSum){ 	// get log sum
				freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
				carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
				bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
				walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
			}
			else{ 				// get raw sum
				freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
				carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
				bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
				walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
			}
			
			if(mode == CELL_BASED){ // only for cell-based accessibility computation
				// assign log sums to current starZone object and spatial grid
				freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
				carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
				bikeGrid.setValue(bikeAccessibility , measurePoint.getGeometry().getCentroid());
				walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
			}
			
			writeCSVData(measurePoint, coordFromZone,
					fromNode, freeSpeedAccessibility, carAccessibility,
					bikeAccessibility, walkAccessibility);
		}
	}
	
	/**
	 * @param gcs
	 * @param distanceMeasuringPoint2Road2Opportunity_meter
	 * @param distanceRoad2Node_meter
	 * @param offsetWalkTime2Node2Opportunity_h
	 * @param opportunityWeight
	 * @param freeSpeedTravelTime_h
	 * @param travelDistance_meter
	 * @param bikeTravelTime_h
	 * @param walkTravelTime_h
	 * @param congestedCarTravelTime_h
	 */
	protected void sumGeneralizedCosts(GeneralizedCostSum gcs,
			double distanceMeasuringPoint2Road2Opportunity_meter,
			double distanceRoad2Node_meter, double offsetWalkTime2Node2Opportunity_h,
			int opportunityWeight, double freeSpeedTravelTime_h,
			double travelDistance_meter, double bikeTravelTime_h,
			double walkTravelTime_h, double congestedCarTravelTime_h) {
		
		// for debugging free speed accessibility
		freeTT = getAsUtilCar(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, offsetWalkTime2Node2Opportunity_h);
		freeTTPower = getAsUtilCar(betaCarTTPower, freeSpeedTravelTime_h * freeSpeedTravelTime_h, betaWalkTTPower, offsetWalkTime2Node2Opportunity_h * offsetWalkTime2Node2Opportunity_h);
		freeLnTT = getAsUtilCar(betaCarLnTT, Math.log(freeSpeedTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2Node2Opportunity_h));
		
		freeTD = getAsUtilCar(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road2Opportunity_meter);
		freeTDPower = getAsUtilCar(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road2Opportunity_meter * distanceMeasuringPoint2Road2Opportunity_meter);
		freeLnTD = getAsUtilCar(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road2Opportunity_meter));
		
		freeTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
		freeTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		freeLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		// sum free speed travel times
		gcs.addFreeSpeedCost(opportunityWeight
				* Math.exp(logitScaleParameter
						* (freeTT + freeTTPower + freeLnTT 
						 + freeTD + freeTDPower + freeLnTD 
						 + freeTC + freeTCPower + freeLnTC)));
		
		// for debugging car accessibility
		carTT = getAsUtilCar(betaCarTT, congestedCarTravelTime_h, betaWalkTT, offsetWalkTime2Node2Opportunity_h);
		carTTPower = getAsUtilCar(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, offsetWalkTime2Node2Opportunity_h * offsetWalkTime2Node2Opportunity_h);
		carLnTT	= getAsUtilCar(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2Node2Opportunity_h));
		
		carTD = getAsUtilCar(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road2Opportunity_meter); // carOffsetWalkTime2NearestLink_meter
		carTDPower = getAsUtilCar(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road2Opportunity_meter * distanceMeasuringPoint2Road2Opportunity_meter);
		carLnTD = getAsUtilCar(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road2Opportunity_meter));
		
		carTC 		= 0.; 	// since MATSim doesn't gives monetary costs jet 
		carTCPower 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		carLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		// sum congested travel times
		gcs.addCongestedCarCost(opportunityWeight
				* Math.exp(logitScaleParameter
						* (carTT + carTTPower + carLnTT 
						+ carTD + carTDPower + carLnTD 
						+ carTC + carTCPower + carLnTC)));
		
		// for debugging bike accessibility
		bikeTT 		= getAsUtilCar(betaBikeTT, bikeTravelTime_h, betaWalkTT, offsetWalkTime2Node2Opportunity_h);
		bikeTTPower = getAsUtilCar(betaBikeTTPower, bikeTravelTime_h * bikeTravelTime_h, betaWalkTTPower, offsetWalkTime2Node2Opportunity_h * offsetWalkTime2Node2Opportunity_h);
		bikeLnTT	= getAsUtilCar(betaBikeLnTT, Math.log(bikeTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2Node2Opportunity_h));
		
		bikeTD = getAsUtilCar(betaBikeTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road2Opportunity_meter); 
		bikeTDPower = getAsUtilCar(betaBikeTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road2Opportunity_meter * distanceMeasuringPoint2Road2Opportunity_meter);
		bikeLnTD = getAsUtilCar(betaBikeLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road2Opportunity_meter));
		
		bikeTC 		= 0.; 	// since MATSim doesn't gives monetary costs jet 
		bikeTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		bikeLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		// sum congested travel times
		gcs.addBikeCost(opportunityWeight
				* Math.exp(logitScaleParameter
						* (bikeTT + bikeTTPower + bikeLnTT 
						+ bikeTD + bikeTDPower + bikeLnTD 
						+ bikeTC + bikeTCPower + bikeLnTC)));
		
		// for debugging walk accessibility
		walkTT = getAsUtilWalk(betaWalkTT, walkTravelTime_h + ((distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour));
		walkTTPower = getAsUtilWalk(betaWalkTTPower, Math.pow(walkTravelTime_h + ((distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour), 2) );
		walkLnTT = getAsUtilWalk(betaWalkLnTT, Math.log( walkTravelTime_h + ((distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour) ));
		
		walkTD = getAsUtilWalk(betaWalkTD, travelDistance_meter + distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter);
		walkTDPower = getAsUtilWalk(betaWalkTDPower, Math.pow(travelDistance_meter + distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter, 2));
		walkLnTD = getAsUtilWalk(betaWalkLnTD, Math.log(travelDistance_meter + distanceMeasuringPoint2Road2Opportunity_meter + distanceRoad2Node_meter));
		
		walkTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
		walkTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		walkLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 

		// sum walk travel times (substitute for distances)
		gcs.addWalkCost(opportunityWeight
				* Math.exp(logitScaleParameter
						* (walkTT + walkTTPower + walkLnTT 
						+ walkTD + walkTDPower + walkLnTD 
						+ walkTC + walkTCPower + walkLnTC)));
	}
	
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
	
//	
//	protected ZoneLayer<Id> createTestPoints(){
//		
//		GeometryFactory factory = new GeometryFactory();
//		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
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
//		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
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
//	private void createCell(GeometryFactory factory, Set<Zone<Id>> zones, Point point, int setPoints, int srid, int gridSize) {
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
//		Zone<Id> zone = new Zone<Id>(polygon);
//		zone.setAttribute( new Id( setPoints ) );
//		zones.add(zone);
//	}
	
	/**
	 * Writes measured accessibilities as csv format to disc
	 * 
	 * @param measurePoint
	 * @param coordFromZone
	 * @param fromNode
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	protected void writeCSVData(
			Zone<Id> measurePoint, Coord coordFromZone,
			Node fromNode, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility,
			double walkAccessibility) {
		// this is just a stub and does nothing. 
		// this needs to be implemented/overwritten by an inherited class
	}
	
	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////
	
	public static class GeneralizedCostSum {
		
		private double sumFREESPEED = 0.;
		private double sumCAR  = 0.;
		private double sumBIKE = 0.;
		private double sumWALK = 0.;
		
		public void reset() {
			this.sumFREESPEED = 0.;
			this.sumCAR		  = 0.;
			this.sumBIKE	  = 0.;
			this.sumWALK	  = 0.;
		}
		
		public void addFreeSpeedCost(double cost){
			this.sumFREESPEED += cost;
		}
		
		public void addCongestedCarCost(double cost){
			this.sumCAR += cost;
		}
		
		public void addBikeCost(double cost){
			this.sumBIKE += cost;
		}
		
		public void addWalkCost(double cost){
			this.sumWALK += cost;
		}
		
		public double getFreeSpeedSum(){
			return this.sumFREESPEED;
		}
		
		public double getCarSum(){
			return this.sumCAR;
		}
		
		public double getBikeSum(){
			return this.sumBIKE;
		}
		
		public double getWalkSum(){
			return this.sumWALK;
		}
	}

}
