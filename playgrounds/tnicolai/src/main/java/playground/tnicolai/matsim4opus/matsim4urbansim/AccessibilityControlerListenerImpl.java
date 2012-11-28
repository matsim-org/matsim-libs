package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import playground.tnicolai.matsim4opus.utils.helperObjects.PersonAndJobsObject;
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
 * @author thomas
 *
 */
public class AccessibilityControlerListenerImpl{
	
	protected static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);
	
	protected MATSim4UrbanSimInterface main = null;
	
	public static final String SHAPE_FILE = "SF";
	public static final String NETWORK 	= "NW";
	protected static String fileExtension;
	
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
		
		AccessibilityParameterConfigModule moduleAPCM = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		// tnicolai TODO: use MATSimControlerConfigModuleV3 to get "timeofday", implement ConfigurationModuleVx which returns the current config modules
		
		useRawSum			= moduleAPCM.isUseRawSumsWithoutLn();
		logitScaleParameter = moduleAPCM.getLogitScaleParameter();
		inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 3600.;
		bikeSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getBikeSpeed() * 3600.; // should be something like 15000
		
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
	 * Aggregates opportunities having the same nearest node on the network
	 * @param parcelsOrZones
	 * @param jobSample
	 * @param network
	 * @return
	 */
	protected AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilitiesImpl parcelsOrZones, final double jobSample, final NetworkImpl network, final boolean isParcelMode){
		
		// readJobs creates a hash map of job with key = job id
		// this hash map includes jobs according to job sample size
		List<PersonAndJobsObject> jobSampleList = this.main.getReadFromUrbanSimModel().readJobs(parcelsOrZones, jobSample, isParcelMode);
		assert( jobSampleList != null );
		
		// Since the aggregated opportunities in jobClusterArray does contain coordinates of their nearest node 
		// this result is dumped out here    tnicolai dec'12
		AnalysisWorkplaceCSVWriter.writeWorkplaceData2CSV(InternalConstants.MATSIM_4_OPUS_TEMP + "workplaces.csv", jobSampleList);
		
		log.info("Aggregating workplaces with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new HashMap<Id, AggregateObject2NearestNode>();
		
		ProgressBar bar = new ProgressBar( jobSampleList.size() );

		for(int i = 0; i < jobSampleList.size(); i++){
			bar.update();
			
			PersonAndJobsObject jo = jobSampleList.get( i );
			assert( jo.getCoord() != null );
			Node nearestNode = network.getNearestNode( jo.getCoord() );
			assert( nearestNode != null );

			// get euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(jo.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double walkDistanceCost 	= (this.betaWalkTD != 0.) 		? Math.exp(this.logitScaleParameter * this.betaWalkTD * distance_meter) 						: 0.;
			double walkPowerDistanceCost= (this.betaWalkTCPower != 0.) 	? Math.exp(this.logitScaleParameter * this.betaWalkTDPower * (distance_meter * distance_meter))	: 0.;
			double walkLnDistanceCost 	= (this.betaWalkLnTD != 0.) 	? Math.exp(this.logitScaleParameter * this.betaWalkLnTD * Math.log(distance_meter)) 			: 0.;
			
			double walkTravelTimeCost 	= (this.betaWalkTT != 0.) 		? Math.exp(this.logitScaleParameter * this.betaWalkTT * walkTravelTime_h)						: 0.;
			double walkPowerTravelTimeCost=(this.betaWalkTTPower != 0.) ? Math.exp(this.logitScaleParameter * this.betaWalkTTPower * (walkTravelTime_h * walkTravelTime_h)): 0.;
			double walkLnTravelTimeCost = (this.betaWalkLnTT != 0.)		? Math.exp(this.logitScaleParameter * this.betaWalkLnTT * Math.log(walkTravelTime_h)) 			: 0.;
			
			double walkMonetaryTravelCost= 0.;
			double walkPowerMonetaryTravelCost= 0.;
			double walkLnMonetaryTravelCost = 0.;
			
			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
				AggregateObject2NearestNode jco = opportunityClusterMap.get( nearestNode.getId() );
				jco.addObject( jo.getObjectID(), 
						walkDistanceCost, walkPowerDistanceCost, walkLnDistanceCost,
						walkTravelTimeCost, walkPowerTravelTimeCost, walkLnTravelTimeCost,
						walkMonetaryTravelCost, walkPowerMonetaryTravelCost, walkLnMonetaryTravelCost);
			}
			else
				opportunityClusterMap.put(
						nearestNode.getId(),
						new AggregateObject2NearestNode(jo.getObjectID(), 
														jo.getParcelID(), 
														jo.getZoneID(), 
														nearestNode.getCoord(), 
														nearestNode, 
														walkDistanceCost, walkPowerDistanceCost, walkLnDistanceCost,
														walkTravelTimeCost, walkPowerTravelTimeCost, walkLnTravelTimeCost,
														walkMonetaryTravelCost, walkPowerMonetaryTravelCost, walkLnMonetaryTravelCost));
		}

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
	 * @param gcs
	 * @param distanceMeasuringPoint2Road_meter
	 * @param distanceRoad2Node_meter
	 * @param walkTravelTimePoint2Road_h
	 * @param opportunityWeight
	 * @param freeSpeedTravelTime_h
	 * @param travelDistance_meter
	 * @param bikeTravelTime_h
	 * @param walkTravelTime_h
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
		
		double sumWalkExpCostDestinationNode2Opportunities = (aggregatedOpportunities.getSumWalkTravelTimeCost() +
															  aggregatedOpportunities.getSumWalkPowerTravelTimeCost() +
															  aggregatedOpportunities.getSumWalkLnTravelTimeCost() +
															  aggregatedOpportunities.getSumWalkDistanceCost() +
															  aggregatedOpportunities.getSumWalkPowerDistanceCost());
		
		// for debugging free speed accessibility
		freeTT = getAsUtil(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		freeTTPower = getAsUtil(betaCarTTPower, freeSpeedTravelTime_h * freeSpeedTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		freeLnTT = getAsUtil(betaCarLnTT, Math.log(freeSpeedTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		freeTD = getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter);
		freeTDPower = getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		freeLnTD = getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		freeTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
		freeTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		freeLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double freeSpeedExpCostOrigin2DestinationNode = Math.exp(logitScaleParameter *
																  (freeTT + freeTTPower + freeLnTT
																  + freeTD + freeTDPower + freeLnTD
																  + freeTC + freeTCPower + freeLnTC) );
		
		// sum free speed travel times
		gcs.addFreeSpeedCost( freeSpeedExpCostOrigin2DestinationNode * sumWalkExpCostDestinationNode2Opportunities);
		
		// for debugging car accessibility
		carTT = getAsUtil(betaCarTT, congestedCarTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		carTTPower = getAsUtil(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		carLnTT	= getAsUtil(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		carTD = getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); // carOffsetWalkTime2NearestLink_meter
		carTDPower = getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		carLnTD = getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		carTC 		= 0.; 	// since MATSim doesn't gives monetary costs jet 
		carTCPower 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		carLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double congestedCarExpCostOrigin2DestinationNode = Math.exp(logitScaleParameter *
																	(carTT + carTTPower + carLnTT 
																	+ carTD + carTDPower + carLnTD 
																	+ carTC + carTCPower + carLnTC));
		
		// sum congested travel times
		gcs.addCongestedCarCost( congestedCarExpCostOrigin2DestinationNode * sumWalkExpCostDestinationNode2Opportunities);
		
		// for debugging bike accessibility
		bikeTT 		= getAsUtil(betaBikeTT, bikeTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		bikeTTPower = getAsUtil(betaBikeTTPower, bikeTravelTime_h * bikeTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		bikeLnTT	= getAsUtil(betaBikeLnTT, Math.log(bikeTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		bikeTD = getAsUtil(betaBikeTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); 
		bikeTDPower = getAsUtil(betaBikeTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		bikeLnTD = getAsUtil(betaBikeLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		bikeTC 		= 0.; 	// since MATSim doesn't gives monetary costs jet 
		bikeTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		bikeLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double bikeExpCostOrigin2DestinationNode = Math.exp(logitScaleParameter
															* (bikeTT + bikeTTPower + bikeLnTT 
															+ bikeTD + bikeTDPower + bikeLnTD 
															+ bikeTC + bikeTCPower + bikeLnTC));
		
		// sum congested travel times
		gcs.addBikeCost( bikeExpCostOrigin2DestinationNode * sumWalkExpCostDestinationNode2Opportunities);
		
		// for debugging walk accessibility
		double totalWalkTravelTime = walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour);
		double totalTravelDistance = travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter;
		
		walkTT = getAsUtil(betaWalkTT, totalWalkTravelTime,0, 0);
		walkTTPower = getAsUtil(betaWalkTTPower, totalWalkTravelTime * totalWalkTravelTime, 0 ,0);
		walkLnTT = getAsUtil(betaWalkLnTT, Math.log(totalWalkTravelTime), 0, 0);
		
		walkTD = getAsUtil(betaWalkTD, totalTravelDistance, 0, 0);
		walkTDPower = getAsUtil(betaWalkTDPower, totalTravelDistance * totalTravelDistance, 0, 0);
		walkLnTD = getAsUtil(betaWalkLnTD, Math.log(totalTravelDistance), 0, 0);

		walkTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
		walkTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
		walkLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
		
		double walkExpCostOrigin2DestinationNode = Math.exp(logitScaleParameter
															* (walkTT + walkTTPower + walkLnTT 
															+ walkTD + walkTDPower + walkLnTD 
															+ walkTC + walkTCPower + walkLnTC));

		// sum walk travel times (substitute for distances)
		gcs.addWalkCost(walkExpCostOrigin2DestinationNode * sumWalkExpCostDestinationNode2Opportunities);
	}
	
	/**
	 * returns an util value for given betas and travel costs/offset
	 * 
	 * @param betaModeX
	 * @param ModeTravelCostX
	 * @param betaWalkX
	 * @param walkOrigin2NetworkX
	 * @return
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
