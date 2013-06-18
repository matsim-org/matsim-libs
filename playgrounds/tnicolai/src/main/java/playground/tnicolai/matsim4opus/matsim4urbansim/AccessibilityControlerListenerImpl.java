package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.ConfigurationModule;
import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.Distances;
import playground.tnicolai.matsim4opus.utils.helperObjects.SpatialReferenceObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

import com.vividsolutions.jts.geom.Point;

/**
 * improvements aug'12
 * - accessibility calculation of unified for cell- and zone-base approach
 * - large computing savings due reduction of "least cost path tree" execution:
 *   In a pre-processing step all nearest nodes of measuring points (origins) are determined. 
 *   The "least cost path tree" for measuring points with the same nearest node are now only executed once. 
 *   Only the cost calculations from the measuring point to the network is done individually.
 *   
 * improvements nov'12
 * - bug fixed aggregatedOpportunities method for compound cost factors like time and distance    
 *   
 * @author thomas
 *
 */
public class AccessibilityControlerListenerImpl{
	
	protected static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);
	
	public static final String FREESEED_FILENAME = "freeSpeedAccessibility_cellsize_";
	public static final String CAR_FILENAME = "carAccessibility_cellsize_";
	public static final String BIKE_FILENAME = "bikeAccessibility_cellsize_";
	public static final String WALK_FILENAME = "walkAccessibility_cellsize_";
	
	protected MATSim4UrbanSimInterface main = null;
	
	protected static int ZONE_BASED 	= 0;
	protected static int PARCEL_BASED 	= 1;
	
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
	
	protected double VijCarTT, VijCarTTPower, VijCarLnTT, VijCarTD, VijCarTDPower, VijCarLnTD, VijCarTC, VijCarTCPower, VijCarLnTC,
		   VijWalkTT, VijWalkTTPower, VijWalkLnTT, VijWalkTD, VijWalkTDPower, VijWalkLnTD, VijWalkTC, VijWalkTCPower, VijWalkLnTC,
		   VijBikeTT, VijBikeTTPower, VijBikeLnTT, VijBikeTD, VijBikeTDPower, VijBikeLnTD, VijBikeTC, VijBikeTCPower, VijBikeLnTC,
		   VijFreeTT, VijFreeTTPower, VijFreeLnTT, VijFreeTD, VijFreeTDPower, VijFreeLnTD, VijFreeTC, VijFreeTCPower, VijFreeLnTC;
	
	protected double depatureTime;
	protected double bikeSpeedMeterPerHour = -1;
	protected double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;

	
	/**
	 * setting parameter for accessibility calculation
	 * @param scenario
	 */
	protected void initAccessibilityParameter(ScenarioImpl scenario){
		
		AccessibilityParameterConfigModule moduleAPCM = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		// tnicolai TODO: use MATSimControlerConfigModuleV3 to get "timeofday", implement ConfigurationModuleVx which returns the current config modules
		
		useRawSum			= moduleAPCM.isUseRawSumsWithoutLn();
		logitScaleParameter = moduleAPCM.getLogitScaleParameter();
		inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
		bikeSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) * 3600.; // should be something like 15000
		
		betaCarTT 	   	= moduleAPCM.getBetaCarTravelTime();
		betaCarTTPower	= moduleAPCM.getBetaCarTravelTimePower2();
		betaCarLnTT		= moduleAPCM.getBetaCarLnTravelTime();
		betaCarTD		= moduleAPCM.getBetaCarTravelDistance();
		betaCarTDPower	= moduleAPCM.getBetaCarTravelDistancePower2();
		betaCarLnTD		= moduleAPCM.getBetaCarLnTravelDistance();
		betaCarTC		= moduleAPCM.getBetaCarTravelCost();
		betaCarTCPower	= moduleAPCM.getBetaCarTravelCostPower2();
		betaCarLnTC		= moduleAPCM.getBetaCarLnTravelCost();
		
		betaBikeTT		= moduleAPCM.getBetaBikeTravelTime();
		betaBikeTTPower	= moduleAPCM.getBetaBikeTravelTimePower2();
		betaBikeLnTT	= moduleAPCM.getBetaBikeLnTravelTime();
		betaBikeTD		= moduleAPCM.getBetaBikeTravelDistance();
		betaBikeTDPower	= moduleAPCM.getBetaBikeTravelDistancePower2();
		betaBikeLnTD	= moduleAPCM.getBetaBikeLnTravelDistance();
		betaBikeTC		= moduleAPCM.getBetaBikeTravelCost();
		betaBikeTCPower	= moduleAPCM.getBetaBikeTravelCostPower2();
		betaBikeLnTC	= moduleAPCM.getBetaBikeLnTravelCost();
		
		betaWalkTT		= moduleAPCM.getBetaWalkTravelTime();
		betaWalkTTPower	= moduleAPCM.getBetaWalkTravelTimePower2();
		betaWalkLnTT	= moduleAPCM.getBetaWalkLnTravelTime();
		betaWalkTD		= moduleAPCM.getBetaWalkTravelDistance();
		betaWalkTDPower	= moduleAPCM.getBetaWalkTravelDistancePower2();
		betaWalkLnTD	= moduleAPCM.getBetaWalkLnTravelDistance();
		betaWalkTC		= moduleAPCM.getBetaWalkTravelCost();
		betaWalkTCPower	= moduleAPCM.getBetaWalkTravelCostPower2();
		betaWalkLnTC	= moduleAPCM.getBetaWalkLnTravelCost();
		
		depatureTime 	= 8.*3600;	
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
		log.info("Walk speed (meter/h): " + this.walkSpeedMeterPerHour + " ("+this.walkSpeedMeterPerHour/3600. +" meter/s)");
		log.info("Bike speed (meter/h): " + this.bikeSpeedMeterPerHour + " ("+this.bikeSpeedMeterPerHour/3600. +" meter/s)");
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
	 * This aggregates the disjutilities Vjk to get from node j to all k that are attached to j.
	 * Finally the sum(Vjk) is assigned to node j, which is done in this method.
	 * 
	 *     j---k1 
	 *     |\
	 *     | \
	 *     k2 k3
	 *     
	 * @param parcelsOrZones opportunities like work places either given at a parcel- or zone level
	 * @param jobSample allows to reduce the sample size of opportunities
	 * @param network the road network
	 * @return the sum of disutilities Vjk, i.e. the disutilities to reach all opportunities k that are assigned to j from node j 
	 */
	protected AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilitiesImpl parcelsOrZones, final double jobSample, final NetworkImpl network, final boolean isParcelMode){
		
		// readJobs creates a hash map of job with key = job id
		// this hash map includes jobs according to job sample size
		List<SpatialReferenceObject> jobSampleList = this.main.getReadFromUrbanSimModel().readJobs(parcelsOrZones, jobSample, isParcelMode);
		assert( jobSampleList != null );
		
		// Since the aggregated opportunities in jobClusterArray does contain coordinates of their nearest node 
		// this result is dumped out here    tnicolai dec'12
		AnalysisWorkplaceCSVWriter.writeWorkplaceData2CSV(InternalConstants.MATSIM_4_OPUS_TEMP + "workplaces.csv", jobSampleList);
		
		log.info("Aggregating workplaces with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new HashMap<Id, AggregateObject2NearestNode>();
		
		ProgressBar bar = new ProgressBar( jobSampleList.size() );

		for(int i = 0; i < jobSampleList.size(); i++){
			bar.update();
			
			SpatialReferenceObject sro = jobSampleList.get( i );
			assert( sro.getCoord() != null );
			Node nearestNode = network.getNearestNode( sro.getCoord() );
			assert( nearestNode != null );

			// get euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(sro.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double VjkWalkTravelTime	= this.betaWalkTT * walkTravelTime_h;
			double VjkWalkPowerTravelTime=this.betaWalkTTPower * (walkTravelTime_h * walkTravelTime_h);
			double VjkWalkLnTravelTime	= this.betaWalkLnTT * Math.log(walkTravelTime_h);
			
			double VjkWalkDistance 		= this.betaWalkTD * distance_meter;
			double VjkWalkPowerDistnace	= this.betaWalkTDPower * (distance_meter * distance_meter);
			double VjkWalkLnDistance 	= this.betaWalkLnTD * Math.log(distance_meter);
			
			double VjkWalkMoney			= 0.;
			double VjkWalkPowerMoney	= 0.;
			double VjkWalkLnMoney		= 0.;

			double Vjk					= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkPowerTravelTime + VjkWalkLnTravelTime +
																			   VjkWalkDistance   + VjkWalkPowerDistnace   + VjkWalkLnDistance +
																			   VjkWalkMoney      + VjkWalkPowerMoney      + VjkWalkLnMoney) );
			// add Vjk to sum
			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
				AggregateObject2NearestNode jco = opportunityClusterMap.get( nearestNode.getId() );
				jco.addObject( sro.getObjectID(), Vjk);
			}
			// assign Vjk to given network node
			else
				opportunityClusterMap.put(
						nearestNode.getId(),
						new AggregateObject2NearestNode(sro.getObjectID(), 
														sro.getParcelID(), 
														sro.getZoneID(), 
														nearestNode.getCoord(), 
														nearestNode, 
														Vjk));
		}
		
		// convert map to array
		AggregateObject2NearestNode jobClusterArray []  = new AggregateObject2NearestNode[ opportunityClusterMap.size() ];
		Iterator<AggregateObject2NearestNode> jobClusterIterator = opportunityClusterMap.values().iterator();

		for(int i = 0; jobClusterIterator.hasNext(); i++)
			jobClusterArray[i] = jobClusterIterator.next();
		
		log.info("Aggregated " + jobSampleList.size() + " number of workplaces (sampling rate: " + jobSample + ") to " + jobClusterArray.length + " nodes.");
		
		return jobClusterArray;
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
											LeastCostPathTree lcptTravelDistance, 
											NetworkImpl network,
											Iterator<Zone<Id>> measuringPointIterator,
											int numberOfMeasuringPoints, 
											int mode) {

		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
//			// tnicolai: only for testing, disable afterwards
//			ZoneLayer<Id> testSet = createTestPoints();
//			measuringPointIterator = testSet.getZones().iterator();

		// this data structure condense measuring points (origins) that have the same nearest node on the network ...
		Map<Id,ArrayList<Zone<Id>>> aggregatedMeasurementPoints = new HashMap<Id, ArrayList<Zone<Id>>>();

		// go through all measuring points ...
		while( measuringPointIterator.hasNext() ){

			Zone<Id> measurePoint = measuringPointIterator.next();
			Point point = measurePoint.getGeometry().getCentroid();
			// get coordinate from origin (start point)
			Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
			// captures the distance (as walk time) between a cell centroid and the road network
			Link nearestLink = network.getNearestLinkExactly(coordFromZone);
			// determine nearest network node (from- or toNode) based on the link 
			Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
			
			// this is used as a key for hash map lookups
			Id id = fromNode.getId();
			
			// create new entry if key does not exist!
			if(!aggregatedMeasurementPoints.containsKey(id))
				aggregatedMeasurementPoints.put(id, new ArrayList<Zone<Id>>());
			// assign measure point (origin) to it's nearest node
			aggregatedMeasurementPoints.get(id).add(measurePoint);
		}
		
		log.info("");
		log.info("Number of measure points: " + numberOfMeasuringPoints);
		log.info("Number of aggregated measure points: " + aggregatedMeasurementPoints.size());
		log.info("");
		

		ProgressBar bar = new ProgressBar( aggregatedMeasurementPoints.size() );
		
		// contains all nodes that have a measuring point (origin) assigned
		Iterator<Id> keyIterator = aggregatedMeasurementPoints.keySet().iterator();
		// contains all network nodes
		Map<Id, Node> networkNodesMap = network.getNodes();
		
		// go through all nodes (key's) that have a measuring point (origin) assigned
		while( keyIterator.hasNext() ){
			
			bar.update();
			
			Id nodeId = keyIterator.next();
			Node fromNode = networkNodesMap.get( nodeId );
			
			// run dijkstra on network
			// this is done once for all origins in the "origins" list, see below
			lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
			lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
			lcptTravelDistance.calculate(network, fromNode, depatureTime);
			
			// get list with origins that are assigned to "fromNode"
			ArrayList<Zone<Id>> origins = aggregatedMeasurementPoints.get( nodeId );
			Iterator<Zone<Id>> originsIterator = origins.iterator();
			
			while( originsIterator.hasNext() ){
				
				Zone<Id> measurePoint = originsIterator.next();
				Point point = measurePoint.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				// captures the distance (as walk time) between a cell centroid and the road network
				Link nearestLink = network.getNearestLinkExactly(coordFromZone);
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				
				Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
				
				double walkTravelTimePoint2Road_h 			= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;

				double freeSpeedTravelTimeOnNearestLink_meterpersec= nearestLink.getFreespeed();
				double carTravelTimeOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
				
				double road2NodeFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTimeOnNearestLink_meterpersec * 3600);
				double road2NodeCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTimeOnNearestLink_meterpersec * 3600.);
				double road2NodeBikeTime_h					= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
				double road2NodeWalkTime_h					= distanceRoad2Node_meter / this.walkSpeedMeterPerHour;
				

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
					
					// get stored network node (this is the nearest node next to an aggregated work place)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					
					// tnicolai not needed anymore? since having the aggregated costs on the opportunity side
					// using number of aggregated opportunities as weight for log sum measure
					// int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects(); 

					// congested car travel times in hours
					double arrivalTime 			= lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
					double congestedCarTravelTime_h = ((arrivalTime - depatureTime) / 3600.) + road2NodeCongestedCarTime_h;
					// free speed car travel times in hours
					double freeSpeedTravelTime_h= (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + road2NodeFreeSpeedTime_h;
					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					// bike travel times in hours
					double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + road2NodeBikeTime_h; // using a constant speed of 15km/h
					// walk travel times in hours
					double walkTravelTime_h		= (travelDistance_meter / this.walkSpeedMeterPerHour) + road2NodeWalkTime_h;
					
					
					sumDisutilityOfTravel(gcs, 
							this.aggregatedOpportunities[i],
							distanceMeasuringPoint2Road_meter,
							distanceRoad2Node_meter, 
							travelDistance_meter,
							walkTravelTimePoint2Road_h,
							freeSpeedTravelTime_h,
							bikeTravelTime_h,
							walkTravelTime_h, 
							congestedCarTravelTime_h);
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
				
				if(mode == PARCEL_BASED){ // only for cell-based accessibility computation
					// assign log sums to current starZone object and spatial grid
					freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
					carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
					bikeGrid.setValue(bikeAccessibility , measurePoint.getGeometry().getCentroid());
					walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
				}
				
				writeCSVData(measurePoint, coordFromZone, fromNode, 
						freeSpeedAccessibility, carAccessibility,
						bikeAccessibility, walkAccessibility);
			}
		}
	}
	
	/**
	 * This calculates the logsum for a given origin i over all opportunities k attached to node j
	 * 
	 * i ----------j---k1
	 *             | \
	 * 			   k2 k3
	 * 
	 * This caluclation is done in 2 steps:
	 * 
	 * 1) The disutilities Vjk to get from node j to all opportunities k are attached to j.
	 *    This is already done above in "aggregatedOpportunities" method and the result is 
	 *    stored in "aggregatedOpportunities" object:
	 * 
	 * S_j = sum_k_in_j (exp(Vjk)) = exp(Vjk1) + exp(Vjk2) + exp(Vjk3)
	 * 
	 * 2) The disutility Vij to get from origin location i to destination node j is calculated in this method.
	 *    Finally the following logsum is taken:   
	 * 
	 * A_i = 1/beatascale * ln (sum_j (exp(Vij) * S_j ) )
	 * 
	 * @param gcs stores the value for the term "exp(Vik)"
	 * @param distanceMeasuringPoint2Road_meter distance in meter from origin i to the network
	 * @param distanceRoad2Node_meter if the mapping of i on the network is on a link, this is the distance in meter from this mapping to the nearest node on the network
	 * @param travelDistance_meter travel distances in meter on the network to get to destination node j
	 * @param walkTravelTimePoint2Road_h walk travel time in h to get from origin i to the network
	 * @param freeSpeedTravelTime_h free speed travel times in h on the network to get to destination node j
	 * @param bikeTravelTime_h bike travel times in h on the network to get to destination node j
	 * @param walkTravelTime_h walk travel times in h on the network to get to destination node j
	 * @param congestedCarTravelTime_h congested car travel times in h on the network to get to destination node j
	 */
	protected void sumDisutilityOfTravel(GeneralizedCostSum gcs,
									   AggregateObject2NearestNode aggregatedOpportunities,
									   double distanceMeasuringPoint2Road_meter,
									   double distanceRoad2Node_meter, 
									   double travelDistance_meter, 
									   double walkTravelTimePoint2Road_h,
									   double freeSpeedTravelTime_h,
									   double bikeTravelTime_h,
									   double walkTravelTime_h, 
									   double congestedCarTravelTime_h) {
		
		// for debugging free speed accessibility
		VijFreeTT 	= getAsUtil(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijFreeTTPower= getAsUtil(betaCarTTPower, freeSpeedTravelTime_h * freeSpeedTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijFreeLnTT = getAsUtil(betaCarLnTT, Math.log(freeSpeedTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijFreeTD 	= getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter);
		VijFreeTDPower= getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijFreeLnTD = getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijFreeTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijFreeTCPower= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijFreeLnTC = 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double expFreeSpeedVij = Math.exp(logitScaleParameter *
										 (VijFreeTT + VijFreeTTPower + VijFreeLnTT
			     					    + VijFreeTD + VijFreeTDPower + VijFreeLnTD
										+ VijFreeTC + VijFreeTCPower + VijFreeLnTC) );
		
		// sum free speed travel times
		gcs.addFreeSpeedCost( expFreeSpeedVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging car accessibility
		VijCarTT 	= getAsUtil(betaCarTT, congestedCarTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijCarTTPower= getAsUtil(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijCarLnTT	= getAsUtil(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijCarTD 	= getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); // carOffsetWalkTime2NearestLink_meter
		VijCarTDPower= getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijCarLnTD 	= getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijCarTC 	= 0.; 	// since MATSim doesn't gives monetary costs jet 
		VijCarTCPower= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijCarLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double expCongestedCarVij = Math.exp(logitScaleParameter *
											(VijCarTT + VijCarTTPower + VijCarLnTT 
										   + VijCarTD + VijCarTDPower + VijCarLnTD 
										   + VijCarTC + VijCarTCPower + VijCarLnTC));
		
		// sum congested travel times
		gcs.addCongestedCarCost( expCongestedCarVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging bike accessibility
		VijBikeTT 	= getAsUtil(betaBikeTT, bikeTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijBikeTTPower= getAsUtil(betaBikeTTPower, bikeTravelTime_h * bikeTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijBikeLnTT	= getAsUtil(betaBikeLnTT, Math.log(bikeTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijBikeTD 	= getAsUtil(betaBikeTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); 
		VijBikeTDPower= getAsUtil(betaBikeTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijBikeLnTD = getAsUtil(betaBikeLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijBikeTC 	= 0.; 	// since MATSim doesn't gives monetary costs jet 
		VijBikeTCPower= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijBikeLnTC = 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double expBikeVij = Math.exp(logitScaleParameter *
								    (VijBikeTT + VijBikeTTPower + VijBikeLnTT 
								   + VijBikeTD + VijBikeTDPower + VijBikeLnTD 
								   + VijBikeTC + VijBikeTCPower + VijBikeLnTC));
		
		// sum congested travel times
		gcs.addBikeCost( expBikeVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging walk accessibility
		double totalWalkTravelTime = walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour);
		double totalTravelDistance = travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter;
		
		VijWalkTT = getAsUtil(betaWalkTT, totalWalkTravelTime,0, 0);
		VijWalkTTPower = getAsUtil(betaWalkTTPower, totalWalkTravelTime * totalWalkTravelTime, 0 ,0);
		VijWalkLnTT = getAsUtil(betaWalkLnTT, Math.log(totalWalkTravelTime), 0, 0);
		
		VijWalkTD = getAsUtil(betaWalkTD, totalTravelDistance, 0, 0);
		VijWalkTDPower = getAsUtil(betaWalkTDPower, totalTravelDistance * totalTravelDistance, 0, 0);
		VijWalkLnTD = getAsUtil(betaWalkLnTD, Math.log(totalTravelDistance), 0, 0);

		VijWalkTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijWalkTCPower= 0.;	// since MATSim doesn't gives monetary costs jet 
		VijWalkLnTC = 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double expWalkVij = Math.exp(logitScaleParameter *
									(VijWalkTT + VijWalkTTPower + VijWalkLnTT 
				                   + VijWalkTD + VijWalkTDPower + VijWalkLnTD 
								   + VijWalkTC + VijWalkTCPower + VijWalkLnTC));

		// sum walk travel times (substitute for distances)
		gcs.addWalkCost(expWalkVij * aggregatedOpportunities.getSumVjk());
	}
	
	/**
	 * converts travel costs (e.g. travel times or distances) into utils by 
	 * using the corresponding marginal utilities
	 * 
	 * @param betaModeX marginal utility for a travel mode other than walk
	 * @param ModeTravelCostX travel costs like travel times or distances
	 * @param betaWalkX marginal utility for traveling on foot
	 * @param walkOrigin2NetworkX travel costs like travel times or distances for traveling on foot
	 * @return disutility of traveling
	 */
	protected double getAsUtil(final double betaModeX, final double ModeTravelCostX, final double betaWalkX, final double walkOrigin2NetworkX){
		if(betaModeX != 0.)
			return (betaModeX * ModeTravelCostX + betaWalkX * walkOrigin2NetworkX);
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
	
	
	/**
	 * stores travel disutilities for different modes
	 * @author thomas
	 *
	 */
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
