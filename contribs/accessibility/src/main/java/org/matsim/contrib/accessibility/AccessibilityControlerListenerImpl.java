package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.config.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.config.M4UAccessibilityConfigUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.ZoneDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AggregateObject2NearestNode;
import org.matsim.contrib.accessibility.utils.Benchmark;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.utils.LeastCostPathTree;

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
 * improvements jan'13
 * - added pt for accessibility calculation
 * 
 * improvements june'13
 * - take "main" (reference to matsim4urbansim) out
 * - aggregation of opportunities adjusted to handle facilities
 * - zones are taken out
 * - replaced 
 *     
 * @author thomas
 *
 */
public abstract class AccessibilityControlerListenerImpl {
	
	private static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);
	
	public static final String FREESEED_FILENAME= "freeSpeedAccessibility_cellsize_";
	public static final String CAR_FILENAME 		= "carAccessibility_cellsize_";
	public static final String BIKE_FILENAME 	= "bikeAccessibility_cellsize_";
	public static final String WALK_FILENAME 	= "walkAccessibility_cellsize_";
	public static final String PT_FILENAME 		= "ptAccessibility_cellsize_";
	
	static int ZONE_BASED 	= 0;
	static int PARCEL_BASED = 1;
	
	// measuring points (origins) for accessibility calculation
	ActivityFacilitiesImpl measuringPoints;
	// containing parcel coordinates for accessibility feedback
	ActivityFacilitiesImpl parcels; 
	// destinations, opportunities like jobs etc ...
	AggregateObject2NearestNode[] aggregatedFacilities;
	
	// storing the accessibility results
	SpatialGrid freeSpeedGrid 	= null;
	SpatialGrid carGrid		= null;
	SpatialGrid bikeGrid		= null;
	SpatialGrid walkGrid		= null;
	SpatialGrid ptGrid			= null;
	
	boolean useFreeSpeedGrid 	= false;
	boolean useCarGrid 			= false;
	boolean useBikeGrid			= false;
	boolean useWalkGrid			= false;
	boolean usePtGrid				= false;
	
	// storing pt matrix
	PtMatrix ptMatrix;
	
	ArrayList<SpatialGridDataExchangeInterface> spatialGridDataExchangeListenerList = null;
	ArrayList<ZoneDataExchangeInterface> zoneDataExchangeListenerList = null;
	
	// accessibility parameter

	// yy I find it quite awkward to generate all these lines of computational code just to copy variables from one place to the other. I assume that
	// one learns to do so in adapter classes, since one does not want changes on one side of the adapter to trigger to the other side of the adapter. 
	// However, the following alternatives seem feasible:
	// * replace those package-wide variables by getters that take the info directly from the other side so that the structure becomes clear
	// * alternatively, use a more intelligent data structure in the sense of beta[car][TD].
	// kai, jul'13

	private boolean useRawSum	; //= false;
	private double logitScaleParameter;
	private double inverseOfLogitScaleParameter;
	private double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	private double betaCarTTPower;
	private double betaCarLnTT;
	private double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	private double betaCarTDPower;
	private double betaCarLnTD;
	private double betaCarTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaCarTMCPower;
	private double betaCarLnTMC;
	private double betaBikeTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaBikeTTPower;
	private double betaBikeLnTT;
	private double betaBikeTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist: 
	private double betaBikeTDPower;
	private double betaBikeLnTD;
	private double betaBikeTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaBikeTMCPower;
	private double betaBikeLnTMC;
	private double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaWalkTTPower;
	private double betaWalkLnTT;
	private double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
	private double betaWalkTDPower;
	private double betaWalkLnTD;
	private double betaWalkTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaWalkTMCPower;
	private double betaWalkLnTMC;
	private double betaPtTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
//	private double betaPtTTPower;
//	private double betaPtLnTT;
	private double betaPtTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
//	private double betaPtTDPower;
//	private double betaPtLnTD;
//	private double betaPtTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
//	private double betaPtTMCPower;
//	private double betaPtLnTMC;
	
	private double constCar;
	private double constBike;
	private double constWalk;
	private double constPt;

//	private double VijCarTT, VijCarTTPower, VijCarLnTT, VijCarTD, VijCarTDPower, VijCarLnTD, VijCarTMC, VijCarTMCPower, VijCarLnTMC,
//		   VijWalkTT, VijWalkTTPower, VijWalkLnTT, VijWalkTD, VijWalkTDPower, VijWalkLnTD, VijWalkTMC, VijWalkTMCPower, VijWalkLnTMC,
//		   VijBikeTT, VijBikeTTPower, VijBikeLnTT, VijBikeTD, VijBikeTDPower, VijBikeLnTD, VijBikeTMC, VijBikeTMCPower, VijBikeLnTMC,
//		   VijFreeTT, VijFreeTTPower, VijFreeLnTT, VijFreeTD, VijFreeTDPower, VijFreeLnTD, VijFreeTC, VijFreeTCPower, VijFreeLnTC,
//		   VijPtTT, VijPtTTPower, VijPtLnTT, VijPtTD, VijPtTDPower, VijPtLnTD, VijPtTMC, VijPtTMCPower, VijPtLnTMC;
	
	private double depatureTime;
	private double bikeSpeedMeterPerHour = -1;
	private double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;
	
	RoadPricingSchemeImpl scheme;
	
	/**
	 * setting parameter for accessibility calculation
	 * @param config TODO
	 */
	final void initAccessibilityParameters(Config config){
		
		AccessibilityConfigGroup moduleAPCM = M4UAccessibilityConfigUtils.getConfigModuleAndPossiblyConvert(config);
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore() ;
		
		useRawSum			= moduleAPCM.isUsingRawSumsWithoutLn();
		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta() ;
		inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		walkSpeedMeterPerHour = config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
		bikeSpeedMeterPerHour = config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) * 3600.; // should be something like 15000

		betaCarTT 	   	= planCalcScoreConfigGroup.getTraveling_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaCarTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRateCar();
		betaCarTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ;
		
		betaBikeTT		= planCalcScoreConfigGroup.getTravelingBike_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaBikeTD		= planCalcScoreConfigGroup.getMarginalUtlOfDistanceOther();
		betaBikeTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney();
		
		betaWalkTT		= planCalcScoreConfigGroup.getTravelingWalk_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaWalkTD		= planCalcScoreConfigGroup.getMarginalUtlOfDistanceWalk();
		betaWalkTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney();
		
		betaPtTT		= planCalcScoreConfigGroup.getTravelingPt_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaPtTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRatePt();
//		betaPtTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ;
		
		constCar		= config.planCalcScore().getConstantCar();
		constBike		= config.planCalcScore().getConstantBike();
		constWalk		= config.planCalcScore().getConstantWalk();
		constPt			= config.planCalcScore().getConstantPt();
		
		depatureTime 	= moduleAPCM.getTimeOfDay(); // by default = 8.*3600;	
		// printParameterSettings(); // use only for debugging since otherwise it clutters the logfile (settings are printed as part of config dump)
	}
	
	/**
	 * displays settings
	 */
	final void printParameterSettings(){
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
		log.info("Beta Car Travel Monetary Cost: " + betaCarTMC );
		log.info("Beta Car Travel Monetary Cost Power2: " + betaCarTMCPower );
		log.info("Beta Car Ln Travel Monetary Cost: " + betaCarLnTMC );
		log.info("Beta Bike Travel Time: " + betaBikeTT );
		log.info("Beta Bike Travel Time Power2: " + betaBikeTTPower );
		log.info("Beta Bike Ln Travel Time: " + betaBikeLnTT );
		log.info("Beta Bike Travel Distance: " + betaBikeTD );
		log.info("Beta Bike Travel Distance Power2: " + betaBikeTDPower );
		log.info("Beta Bike Ln Travel Distance: " + betaBikeLnTD );
		log.info("Beta Bike Travel Monetary Cost: " + betaBikeTMC );
		log.info("Beta Bike Travel Monetary Cost Power2: " + betaBikeTMCPower );
		log.info("Beta Bike Ln Travel Monetary Cost: " + betaBikeLnTMC );
		log.info("Beta Walk Travel Time: " + betaWalkTT );
		log.info("Beta Walk Travel Time Power2: " + betaWalkTTPower );
		log.info("Beta Walk Ln Travel Time: " + betaWalkLnTT );
		log.info("Beta Walk Travel Distance: " + betaWalkTD );
		log.info("Beta Walk Travel Distance Power2: " + betaWalkTDPower );
		log.info("Beta Walk Ln Travel Distance: " + betaWalkLnTD );
		log.info("Beta Walk Travel Monetary Cost: " + betaWalkTMC );
		log.info("Beta Walk Travel Monetary Cost Power2: " + betaWalkTMCPower );
		log.info("Beta Walk Ln Travel Monetary Cost: " + betaWalkLnTMC );
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
	 * @param opportunities such as workplaces, either given at a parcel- or zone-level
	 * @param network giving the road network
	 * @return the sum of disutilities Vjk, i.e. the disutilities to reach all opportunities k that are assigned to j from node j 
	 */
	final AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilities opportunities, Network network){
	
		log.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new ConcurrentHashMap<Id, AggregateObject2NearestNode>();
		ProgressBar bar = new ProgressBar( opportunities.getFacilities().size() );
	
		Iterator<? extends ActivityFacility> oppIterator = opportunities.getFacilities().values().iterator();
		
		while(oppIterator.hasNext()){
			
			bar.update();
			
			ActivityFacility opprotunity = oppIterator.next();
			Node nearestNode = ((NetworkImpl)network).getNearestNode( opprotunity.getCoord() );
			
			// get Euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(opprotunity.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double VjkWalkTravelTime	= this.betaWalkTT * walkTravelTime_h;
			double VjkWalkPowerTravelTime=0.; // this.betaWalkTTPower * (walkTravelTime_h * walkTravelTime_h);
			double VjkWalkLnTravelTime	= 0.; // this.betaWalkLnTT * Math.log(walkTravelTime_h);
			
			double VjkWalkDistance 		= this.betaWalkTD * distance_meter;
			double VjkWalkPowerDistnace	= 0.; //this.betaWalkTDPower * (distance_meter * distance_meter);
			double VjkWalkLnDistance 	= 0.; //this.betaWalkLnTD * Math.log(distance_meter);
			
			double VjkWalkMoney			= this.betaWalkTMC * 0.; 			// no monetary costs for walking
			double VjkWalkPowerMoney	= 0.; //this.betaWalkTDPower * 0.; 	// no monetary costs for walking
			double VjkWalkLnMoney		= 0.; //this.betaWalkLnTMC *0.; 	// no monetary costs for walking
			
			double Vjk					= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkPowerTravelTime + VjkWalkLnTravelTime +
					   														   VjkWalkDistance   + VjkWalkPowerDistnace   + VjkWalkLnDistance +
					   														   VjkWalkMoney      + VjkWalkPowerMoney      + VjkWalkLnMoney) );
			// add Vjk to sum
			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
				AggregateObject2NearestNode jco = opportunityClusterMap.get( nearestNode.getId() );
				jco.addObject( opprotunity.getId(), Vjk);
			}
			else // assign Vjk to given network node
				opportunityClusterMap.put(
						nearestNode.getId(),
						new AggregateObject2NearestNode(opprotunity.getId(), 
														null,
														null,
														nearestNode.getCoord(), 
														nearestNode, 
														Vjk));
		}
		// convert map to array
		AggregateObject2NearestNode jobClusterArray []  = new AggregateObject2NearestNode[ opportunityClusterMap.size() ];
		Iterator<AggregateObject2NearestNode> jobClusterIterator = opportunityClusterMap.values().iterator();

		for(int i = 0; jobClusterIterator.hasNext(); i++)
			jobClusterArray[i] = jobClusterIterator.next();
		
		log.info("Aggregated " + opportunities.getFacilities().size() + " number of opportunities to " + jobClusterArray.length + " nodes.");
		
		return jobClusterArray;
	}
	
	/**
	 * @param ttc
	 * @param lcptTravelDistance
	 * @param network
	 * @param lcptFreeSpeedCarTravelTime
	 * @param lcptCongestedCarTravelTime
	 * @param inverseOfLogitScaleParameter
	 * @param accCsvWriter
	 * @param measuringPointIterator
	 */
	final void accessibilityComputation(TravelTime ttc,
											LeastCostPathTreeExtended lcptExtFreeSpeedCarTravelTime,
											LeastCostPathTreeExtended lcptExtCongestedCarTravelTime,
											LeastCostPathTree lcptTravelDistance, 
											NetworkImpl network,
											ActivityFacilitiesImpl mp,
											int mode) {

		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
		Iterator<? extends ActivityFacility> mpIterator = mp.getFacilities().values().iterator();
		
		// this data structure condense measuring points (origins) that have the same nearest node on the network ...
		Map<Id,ArrayList<ActivityFacility>> aggregatedMeasurementPointsV2 = new ConcurrentHashMap<Id, ArrayList<ActivityFacility>>();

		// go through all measuring points ...
		while( mpIterator.hasNext() ){

			ActivityFacility aFac = mpIterator.next();

			// captures the distance (as walk time) between a cell centroid and the road network
			Link nearestLink = network.getNearestLinkExactly(aFac.getCoord());
			// determine nearest network node (from- or toNode) based on the link 
			Node fromNode = NetworkUtil.getNearestNode(aFac.getCoord(), nearestLink);
			
			// this is used as a key for hash map lookups
			Id id = fromNode.getId();
			
			// create new entry if key does not exist!
			if(!aggregatedMeasurementPointsV2.containsKey(id))
				aggregatedMeasurementPointsV2.put(id, new ArrayList<ActivityFacility>());
			// assign measure point (origin) to it's nearest node
			aggregatedMeasurementPointsV2.get(id).add(aFac);
		}
		
		log.info("");
		log.info("Number of measurement points (origins): " + mp.getFacilities().values().size());
		log.info("Number of aggregated measurement points (origins): " + aggregatedMeasurementPointsV2.size());
		log.info("Now going through all origins:");
		
		ProgressBar bar = new ProgressBar( aggregatedMeasurementPointsV2.size() );
		
		// contains all nodes that have a measuring point (origin) assigned
		Iterator<Id> keyIterator = aggregatedMeasurementPointsV2.keySet().iterator();
		// contains all network nodes
		Map<Id, Node> networkNodesMap = network.getNodes();
		
		// go through all nodes (key's) that have a measuring point (origin) assigned
		while( keyIterator.hasNext() ){
			
			bar.update();
			
			Id nodeId = keyIterator.next();
			Node fromNode = networkNodesMap.get( nodeId );
			
			// run dijkstra on network
			// this is done once for all origins in the "origins" list, see below
			if(this.useFreeSpeedGrid)
				lcptExtFreeSpeedCarTravelTime.calculateExtended(network, fromNode, depatureTime);
			if(this.useCarGrid)
				lcptExtCongestedCarTravelTime.calculateExtended(network, fromNode, depatureTime);		
			lcptTravelDistance.calculate(network, fromNode, depatureTime);
			
			// get list with origins that are assigned to "fromNode"
			ArrayList<ActivityFacility> origins = aggregatedMeasurementPointsV2.get( nodeId );
			Iterator<ActivityFacility> originsIterator = origins.iterator();
			
			while( originsIterator.hasNext() ){
				
				ActivityFacility aFac = originsIterator.next();
				assert( aFac.getCoord() != null );
				// captures the distance (as walk time) between a cell centroid and the road network
				LinkImpl nearestLink = (LinkImpl)network.getNearestLinkExactly(aFac.getCoord());
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				Distances distance = NetworkUtil.getDistance2Node(nearestLink, aFac.getCoord(), fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDistancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	// distance intersection 2 node (only for orthogonal distance), this is zero if projection is on a node 
				
				// traveling on foot from measuring point to the network (link or node)
				double walkTravelTimeMeasuringPoint2Road_h 	= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;

				// get free speed and congested car travel times on a certain link
				double freeSpeedTravelTimeOnNearestLink_meterpersec = nearestLink.getFreespeedTravelTime(depatureTime);
				double carTravelTimeOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
				// travel time in hours to get from link intersection (position on a link given by orthogonal projection from measuring point) to the corresponding node
				double road2NodeFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTimeOnNearestLink_meterpersec * 3600);
				double road2NodeCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTimeOnNearestLink_meterpersec * 3600.);
				double road2NodeBikeTime_h					= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
				double road2NodeWalkTime_h					= distanceRoad2Node_meter / this.walkSpeedMeterPerHour;
				double road2NodeToll_money 					= getToll(nearestLink); // tnicolai: add this to car disutility ??? depends on the road pricing scheme ...
				
				// this contains the current toll based on the toll scheme
				double toll_money 							= 0.;
				if(this.scheme != null && RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType()))
					toll_money = road2NodeToll_money;
				else if(this.scheme != null && RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()))
					toll_money = road2NodeToll_money * distanceRoad2Node_meter;
				
				gcs.reset();

				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedFacilities.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated work place)
					Node destinationNode = this.aggregatedFacilities[i].getNearestNode();
					Id nodeID = destinationNode.getId();

					// disutilities on the road network
					double congestedCarDisutility = Double.NaN;
					if(this.useCarGrid)
						congestedCarDisutility = - lcptExtCongestedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility congested car on road network (including toll)
					double freeSpeedCarDisutility = Double.NaN;
					if(this.useFreeSpeedGrid)
						freeSpeedCarDisutility = - lcptExtFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility free speed car on road network (including toll)
					double travelDistance_meter = Double.NaN;
					if(this.useBikeGrid || this.useWalkGrid)
						travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost(); 				// travel link distances on road network for bicycle and walk

					// disutilities to get on or off the network
					double walkDisutilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT) + (distanceMeasuringPoint2Road_meter * betaWalkTD);
					double expVhiWalk = Math.exp(this.logitScaleParameter * walkDisutilityMeasuringPoint2Road);
					double sumExpVjkWalk = aggregatedFacilities[i].getSumVjk();
					
					// travel times and distances for pseudo pt
					if(this.usePtGrid){
						if ( ptMatrix==null ) {
							throw new RuntimeException( "pt accessibility does only work when a PtMatrix is provided.  Provide such a matrix, or switch off "
									+ "the pt accessibility computation, or extend the Java code so that it works for this situation.") ;
						}
						// travel time with pt:
						double ptTravelTime_h	 = ptMatrix.getPtTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
						// total walking time including (i) to get to pt stop and (ii) to get from destination pt stop to destination location:
						double ptTotalWalkTime_h =ptMatrix.getTotalWalkTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
						// total travel distance including walking and pt distance from/to origin/destination location:
						double ptTravelDistance_meter=ptMatrix.getTotalWalkTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());
						// total walk distance  including (i) to get to pt stop and (ii) to get from destination pt stop to destination location:
						double ptTotalWalkDistance_meter=ptMatrix.getPtTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());

						double ptDisutility = constPt + (ptTotalWalkTime_h * betaWalkTT) + (ptTravelTime_h * betaPtTT) + (ptTotalWalkDistance_meter * betaWalkTD) + (ptTravelDistance_meter * betaPtTD);					
						double expVijPt = Math.exp(this.logitScaleParameter * ptDisutility);
						double expVhkPt = expVijPt * sumExpVjkWalk;
						gcs.addPtCost( expVhkPt );
					}
					
					// total disutility congested car
					if(this.useCarGrid){
						double congestedCarDisutilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
						double expVijCongestedCar = Math.exp(this.logitScaleParameter * (constCar + congestedCarDisutilityRoad2Node + congestedCarDisutility) );
						double expVhkCongestedCar = expVhiWalk * expVijCongestedCar * sumExpVjkWalk;
						gcs.addCongestedCarCost( expVhkCongestedCar );
					}
					
					// total disutility free speed car
					if(this.useFreeSpeedGrid){
						double freeSpeedCarDisutilityRoad2Node = (road2NodeFreeSpeedTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
						double expVijFreeSpeedCar = Math.exp(this.logitScaleParameter * (constCar + freeSpeedCarDisutilityRoad2Node + freeSpeedCarDisutility) );
						double expVhkFreeSpeedCar = expVhiWalk * expVijFreeSpeedCar * sumExpVjkWalk;
						gcs.addFreeSpeedCost( expVhkFreeSpeedCar );
					}
					
					// total disutility bicycle
					if(this.useBikeGrid){
						double bikeDisutilityRoad2Node = (road2NodeBikeTime_h * betaBikeTT) + (distanceRoad2Node_meter * betaBikeTD); // toll or money ???
						double bikeDisutility = ((travelDistance_meter/this.bikeSpeedMeterPerHour) * betaBikeTT) + (travelDistance_meter * betaBikeTD);// toll or money ???
						double expVijBike = Math.exp(this.logitScaleParameter * (constBike + bikeDisutility + bikeDisutilityRoad2Node));
						double expVhkBike = expVhiWalk * expVijBike * sumExpVjkWalk;
						gcs.addBikeCost( expVhkBike );
					}
					
					// total disutility walk
					if(this.useWalkGrid){
						double walkDisutilityRoad2Node = (road2NodeWalkTime_h * betaWalkTT) + (distanceRoad2Node_meter * betaWalkTD);  // toll or money ???
						double walkDisutility = ( (travelDistance_meter / this.walkSpeedMeterPerHour) * betaWalkTT) + ( travelDistance_meter * betaWalkTD);// toll or money ???
						double expVijWalk = Math.exp(this.logitScaleParameter * (constWalk + walkDisutility + walkDisutilityRoad2Node));
						double expVhkWalk = expVhiWalk * expVijWalk * sumExpVjkWalk;
						gcs.addWalkCost( expVhkWalk );
					}
				}
				
				// aggregated value
				double freeSpeedAccessibility = Double.NaN; 
				double carAccessibility = Double.NaN; 
				double bikeAccessibility= Double.NaN;
				double walkAccessibility= Double.NaN;
				double ptAccessibility 	= Double.NaN;
				if(!useRawSum){ 	// get log sum
					if(this.useFreeSpeedGrid)
						freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
					if(this.useCarGrid)
						carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
					if(this.useBikeGrid)
						bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
					if(this.useWalkGrid)
						walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
					if(this.usePtGrid)
						ptAccessibility	 = inverseOfLogitScaleParameter * Math.log( gcs.getPtSum() );
				}
				else{ 				// get raw sum
					if(this.useFreeSpeedGrid)
						freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
					if(this.useCarGrid)
						carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
					if(this.useBikeGrid)
						bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
					if(this.useWalkGrid)
						walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
					if(this.usePtGrid)
						ptAccessibility  = inverseOfLogitScaleParameter * gcs.getPtSum();
				}

				if(mode == PARCEL_BASED){ // only for cell-based accessibility computation
					// assign log sums to current starZone object and spatial grid
					if(this.freeSpeedGrid != null)
						freeSpeedGrid.setValue(freeSpeedAccessibility, aFac.getCoord().getX(), aFac.getCoord().getY());
					if(this.carGrid != null)
						carGrid.setValue(carAccessibility ,aFac.getCoord().getX(), aFac.getCoord().getY());
					if(this.bikeGrid != null)
						bikeGrid.setValue(bikeAccessibility , aFac.getCoord().getX(), aFac.getCoord().getY());
					if(this.walkGrid != null)
						walkGrid.setValue(walkAccessibility , aFac.getCoord().getX(), aFac.getCoord().getY());
					if(this.ptGrid != null)
						ptGrid.setValue(ptAccessibility, aFac.getCoord().getX(), aFac.getCoord().getY());
				}
				
				// writing measured accessibilities for current measuring point 
				// (aFac) in csv format to disc
				writeCSVData(aFac, fromNode, 
							 freeSpeedAccessibility, 
							 carAccessibility, 
							 bikeAccessibility,
							 walkAccessibility, 
							 ptAccessibility);
				
				if(this.zoneDataExchangeListenerList != null){
					for(int i = 0; i < this.zoneDataExchangeListenerList.size(); i++)
						this.zoneDataExchangeListenerList.get(i).getZoneAccessibilities(aFac, freeSpeedAccessibility, carAccessibility,
								bikeAccessibility, walkAccessibility, ptAccessibility);
				}
				
			}
		}
		
	}

	/**
	 * @param nearestLink
	 */
	double getToll(Link nearestLink) {
		if(scheme != null){
			Cost cost = scheme.getLinkCostInfo(nearestLink.getId(), depatureTime, null);
			if(cost != null)
				return cost.amount;
		}
		return 0.;
	}
	
	/**
	 * activates the SpatialGrid for free speed car 
	 * <p/>
	 * Comments:<ul>
	 * <li>In the longer run, I would rather have some "list" of modes rather than separate switches. kai, jun'13
	 * </ul>
	 * @param val TODO
	 */
	public void setComputingAccessibilityForFreeSpeedCar(boolean val){
		this.useFreeSpeedGrid = val;
	}
	
	/**
	 * activates the SpatialGrid for congested car 
	 * <p/>
	 * Comments:<ul>
	 * <li>In the longer run, I would rather have some "list" of modes rather than separate switches. kai, jun'13
	 * </ul>
	 * @param val TODO
	 */
	public void setComputingAccessibilityForCongestedCar(boolean val){
		this.useCarGrid = val;
	}
	
	/**
	 * activates the SpatialGrid for bicycle
	 * <p/>
	 * Comments:<ul>
	 * <li>In the longer run, I would rather have some "list" of modes rather than separate switches. kai, jun'13
	 * </ul>
	 * @param val TODO
	 */
	public void setComputingAccessibilityForBike(boolean val){
		this.useBikeGrid = val;
	}
	
	/**
	 * activates the SpatialGrid for traveling on foot
	 * <p/>
	 * Comments:<ul>
	 * <li>In the longer run, I would rather have some "list" of modes rather than separate switches. kai, jun'13
	 * </ul>
	 * @param val TODO
	 */
	public void setComputingAccessibilityForWalk(boolean val){
		this.useWalkGrid = val;
	}
	
	/**
	 * activates the SpatialGrid for public transport (improved pseudo pt)
	 * <p/>
	 * Comments:<ul>
	 * <li>In the longer run, I would rather have some "list" of modes rather than separate switches. kai, jun'13
	 * </ul>
	 * @param val TODO
	 */
	public void setComputingAccessibilityForPt(boolean val){
		this.usePtGrid = val;
	}
	
	/**
	 * This adds listeners to write out accessibility results for parcels in UrbanSim format
	 * @param l
	 */
	public void addSpatialGridDataExchangeListener(SpatialGridDataExchangeInterface l){
		if(this.spatialGridDataExchangeListenerList == null)
			this.spatialGridDataExchangeListenerList = new ArrayList<SpatialGridDataExchangeInterface>();
		
		log.info("Adding new SpatialGridDataExchange listener...");
		this.spatialGridDataExchangeListenerList.add(l);
		log.info("... done!");
	}
	
	/**
	 * This adds listeners to write out accessibility results for parcels in UrbanSim format
	 * @param l
	 */
	public void addZoneDataExchangeListener(ZoneDataExchangeInterface l){
		if(this.zoneDataExchangeListenerList == null)
			this.zoneDataExchangeListenerList = new ArrayList<ZoneDataExchangeInterface>();
		
		log.info("Adding new SpatialGridDataExchange listener...");
		this.zoneDataExchangeListenerList.add(l);
		log.info("... done!");
	}

	/**
	 * Writes measured accessibilities as csv format to disc
	 * 
	 * @param measurePoint
	 * @param fromNode
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	abstract void writeCSVData(
			ActivityFacility measurePoint, Node fromNode,
			double freeSpeedAccessibility, double carAccessibility,
			double bikeAccessibility, double walkAccessibility,
			double ptAccessibility) ;
	
	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////
	
	
	/**
	 * stores travel disutilities for different modes
	 */
	public static class GeneralizedCostSum {
		
		private double sumFREESPEED = 0.;
		private double sumCAR  	= 0.;
		private double sumBIKE 	= 0.;
		private double sumWALK 	= 0.;
		private double sumPt   	= 0.;
		
		public void reset() {
			this.sumFREESPEED 	= 0.;
			this.sumCAR		  	= 0.;
			this.sumBIKE	  	= 0.;
			this.sumWALK	  	= 0.;
			this.sumPt		  	= 0.;
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
		
		public void addPtCost(double cost){
			this.sumPt += cost;
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
		
		public double getPtSum(){
			return this.sumPt;
		}
	}

}
