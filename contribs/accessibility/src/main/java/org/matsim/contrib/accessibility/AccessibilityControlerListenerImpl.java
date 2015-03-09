package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.ZoneDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Benchmark;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.utils.LeastCostPathTree;

/**
 * improvements aug'12<ul>
 * <li> accessibility calculation of unified for cell- and zone-base approach
 * <li> large computing savings due reduction of "least cost path tree" execution:
 *   In a pre-processing step all nearest nodes of measuring points (origins) are determined. 
 *   The "least cost path tree" for measuring points with the same nearest node are now only executed once. 
 *   Only the cost calculations from the measuring point to the network is done individually.
 * </ul><p/>  
 * improvements nov'12<ul>
 * <li> bug fixed aggregatedOpportunities method for compound cost factors like time and distance    
 * </ul><p/>
 * improvements jan'13<ul>
 * <li> added pt for accessibility calculation
 * </ul><p/>
 * 
 * improvements june'13<ul>
 * <li> take "main" (reference to matsim4urbansim) out
 * <li> aggregation of opportunities adjusted to handle facilities
 * <li> zones are taken out
 * <li> replaced [[??]]
 * </ul> 
 * <p/> 
 * Design comments:<ul>
 * <li> yyyy This class is quite brittle, since it does not use a central disutility object, but produces its own.  Should be changed.
 * </ul>
 *     
 * @author thomas, knagel
 *
 */
abstract class AccessibilityControlerListenerImpl {

	private static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);

	int ZONE_BASED 	= 0;
	int PARCEL_BASED = 1;

	// measuring points (origins) for accessibility calculation
	ActivityFacilitiesImpl measuringPoints;
	// containing parcel coordinates for accessibility feedback
	ActivityFacilitiesImpl parcels; 
	// destinations, opportunities like jobs etc ...
	AggregationObject[] aggregatedOpportunities;
	
	// storing the accessibility results
	private Map<Modes4Accessibility,SpatialGrid> accessibilityGrids = new HashMap<Modes4Accessibility,SpatialGrid>() ;

	Map<Modes4Accessibility,Boolean> isComputingMode = new HashMap<Modes4Accessibility,Boolean>() ;

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
	private double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	private double betaCarTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaBikeTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaBikeTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist: 
	private double betaBikeTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
	private double betaWalkTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaPtTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	private double betaPtTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()

	private double constCar;
	private double constBike;
	private double constWalk;
	private double constPt;

	private double depatureTime;
	private double bikeSpeedMeterPerHour = -1;
	private double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;
	
	// counter for warning that capacities are not used so far ... in order not to give the same warning multiple times; dz, apr'14
	private static int cnt = 0 ;

	RoadPricingScheme scheme;

	private boolean urbansimMode = true;

	AccessibilityControlerListenerImpl() {
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			this.isComputingMode.put( mode, false ) ;
		}
	}

	
	/**
	 * setting parameter for accessibility calculation
	 * @param config TODO
	 */
	final void initAccessibilityParameters(Config config){

		AccessibilityConfigGroup moduleAPCM = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore() ;

		if ( planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance() != 0. ) {
			log.error( "marginal utility of distance for car different from zero but not used in accessibility computations");
		}
		if ( planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.pt).getMarginalUtilityOfDistance() != 0. ) {
			log.error( "marginal utility of distance for pt different from zero but not used in accessibility computations");
		}
		if ( planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.bike).getMonetaryDistanceCostRate() != 0. ) {
			log.error( "monetary distance cost rate for bike different from zero but not used in accessibility computations");
		}
		if ( planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.walk).getMonetaryDistanceCostRate() != 0. ) {
			log.error( "monetary distance cost rate for walk different from zero but not used in accessibility computations");
		}

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
		log.info("Beta Car Travel Distance: " + betaCarTD );
		log.info("Beta Car Travel Monetary Cost: " + betaCarTMC );
		log.info("Beta Bike Travel Time: " + betaBikeTT );
		log.info("Beta Bike Travel Distance: " + betaBikeTD );
		log.info("Beta Bike Travel Monetary Cost: " + betaBikeTMC );
		log.info("Beta Walk Travel Time: " + betaWalkTT );
		log.info("Beta Walk Travel Distance: " + betaWalkTD );
		log.info("Beta Walk Travel Monetary Cost: " + betaWalkTMC );
	}

	
	/**
	 * This aggregates the disutilities Vjk to get from node j to all k that are attached to j.
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
	final AggregationObject[] aggregatedOpportunities(final ActivityFacilities opportunities, Network network){
		// yyyy this method ignores the "capacities" of the facilities.  kai, mar'14

		log.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with identical nearest node ...");
		Map<Id<Node>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<Id<Node>, AggregationObject>();
		ProgressBar bar = new ProgressBar( opportunities.getFacilities().size() );

		for ( ActivityFacility opportunity : opportunities.getFacilities().values() ) {
			bar.update();

			Node nearestNode = ((NetworkImpl)network).getNearestNode( opportunity.getCoord() );

			// get Euclidian distance to nearest node
			double distance_meter 	= NetworkUtils.getEuclidianDistance(opportunity.getCoord(), nearestNode.getCoord());
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

			double expVjk					= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkPowerTravelTime + VjkWalkLnTravelTime +
					VjkWalkDistance   + VjkWalkPowerDistnace   + VjkWalkLnDistance +
					VjkWalkMoney      + VjkWalkPowerMoney      + VjkWalkLnMoney) );
			// add Vjk to sum
//			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
//				AggregationObject jco = opportunityClusterMap.get( nearestNode.getId() );
//				jco.addObject( opportunity.getId(), expVjk);
//			} else {
//				// assign Vjk to given network node
//				opportunityClusterMap.put(
//						nearestNode.getId(),
//						new AggregationObject(opportunity.getId(), null, null, nearestNode, expVjk) 
//						);
//			}
			AggregationObject jco = opportunityClusterMap.get( nearestNode.getId() ) ;
			if ( jco == null ) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestNode, 0. ); // initialize with zero!
				opportunityClusterMap.put( nearestNode.getId(), jco ) ; 
			}
			if ( cnt == 0 ) {
				cnt++;
				log.warn("ignoring the capacities of the facilities");
				log.warn(Gbl.ONLYONCE);
			}
			jco.addObject( opportunity.getId(), expVjk ) ;
			// yyyy if we knew the activity type, we could to do capacities as follows:
//			ActivityOption opt = opportunity.getActivityOptions().get("type") ;
//			Assert.assertNotNull(opt);
//			final double capacity = opt.getCapacity();
//			Assert.assertNotNull(capacity) ; // we do not know what that would mean
//			if ( capacity < Double.POSITIVE_INFINITY ) { // this is sometimes the value of "undefined" 
//				jco.addObject( opportunity.getId(), capacity * expVjk ) ;
//			} else {
//				jco.addObject( opportunity.getId(), expVjk ) ; // fix if capacity is "unknown".
//			}
			
		}
		// convert map to array
		AggregationObject jobClusterArray []  = new AggregationObject[ opportunityClusterMap.size() ];
		Iterator<AggregationObject> jobClusterIterator = opportunityClusterMap.values().iterator();
		for(int i = 0; jobClusterIterator.hasNext(); i++) {
			jobClusterArray[i] = jobClusterIterator.next();
		}

		// yy maybe replace by following?? Needs to be tested.  kai, mar'14
//		AggregateObject2NearestNode[] jobClusterArray = (AggregateObject2NearestNode[]) opportunityClusterMap.values().toArray() ;

		log.info("Aggregated " + opportunities.getFacilities().size() + " number of opportunities to " + jobClusterArray.length + " nodes.");

		return jobClusterArray;
	}

	
	final void accessibilityComputation(TravelTime ttc,
			LeastCostPathTreeExtended lcptExtFreeSpeedCarTravelTime,
			LeastCostPathTreeExtended lcptExtCongestedCarTravelTime,
			LeastCostPathTree lcptTravelDistance, 
			NetworkImpl network,
			ActivityFacilities  mp,
			int runMode) {

		SumOfExpUtils[] gcs = new SumOfExpUtils[Modes4Accessibility.values().length] ;
		// this could just be a double array, or a Map.  Not using a Map for computational speed reasons (untested);
		// not using a simple double array for type safety in long argument lists. kai, feb'14
		for ( int ii=0 ; ii<gcs.length ; ii++ ) {
			gcs[ii] = new SumOfExpUtils() ;
		}


		// this data structure condense measuring points (origins) that have the same nearest node on the network ...
		Map<Id<Node>,ArrayList<ActivityFacility>> aggregatedOrigins = new ConcurrentHashMap<Id<Node>, ArrayList<ActivityFacility>>();
		// ========================================================================
		for ( ActivityFacility aFac : mp.getFacilities().values() ) {

			// determine nearest network node (from- or toNode) based on the link 
			Node fromNode = NetworkUtils.getCloserNodeOnLink(aFac.getCoord(), network.getNearestLinkExactly(aFac.getCoord()));

			// this is used as a key for hash map lookups
			Id<Node> nodeId = fromNode.getId();

			// create new entry if key does not exist!
			if(!aggregatedOrigins.containsKey(nodeId)) {
				aggregatedOrigins.put(nodeId, new ArrayList<ActivityFacility>());
			}
			// assign measure point (origin) to it's nearest node
			aggregatedOrigins.get(nodeId).add(aFac);
		}
		// ========================================================================

		log.info("");
		log.info("Number of measurement points (origins): " + mp.getFacilities().values().size());
		log.info("Number of aggregated measurement points (origins): " + aggregatedOrigins.size());
		log.info("Now going through all origins:");

		ProgressBar bar = new ProgressBar( aggregatedOrigins.size() );
		// ========================================================================
		// go through all nodes (keys) that have a measuring point (origin) assigned
		for ( Id<Node> nodeId : aggregatedOrigins.keySet() ) {

			bar.update();

			Node fromNode = network.getNodes().get( nodeId );

			// run Dijkstra on network
			// this is done once for all origins in the "origins" list, see below
			if(this.isComputingMode.get(Modes4Accessibility.freeSpeed) ) {
				lcptExtFreeSpeedCarTravelTime.calculateExtended(network, fromNode, depatureTime);
			}
			if(this.isComputingMode.get(Modes4Accessibility.car) ) {
				lcptExtCongestedCarTravelTime.calculateExtended(network, fromNode, depatureTime);
			}
			lcptTravelDistance.calculate(network, fromNode, depatureTime);

			// get list with origins that are assigned to "fromNode"
			for ( ActivityFacility origin : aggregatedOrigins.get( nodeId ) ) {
				assert( origin.getCoord() != null );

				// get the nearest link:
				Link nearestLink = network.getNearestLinkExactly(origin.getCoord());

				// captures the distance (as walk time) between the origin via the link to the node:
				Distances distance = NetworkUtil.getDistances2Node(origin.getCoord(), nearestLink, fromNode);

				// distance to road, and then to node:
				double walkTravelTimeMeasuringPoint2Road_h 	= distance.getDistancePoint2Road() / this.walkSpeedMeterPerHour;

				// get free speed and congested car travel times on a certain link
				double freeSpeedOnNearestLink_meterpersec = ((LinkImpl)nearestLink).getFreespeedTravelTime(depatureTime);
				double carSpeedOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);

				// travel time in hours to get from link enter point (position on a link given by orthogonal projection from measuring point) to the corresponding node
				double road2NodeFreeSpeedTime_h				= distance.getDistanceRoad2Node() / (freeSpeedOnNearestLink_meterpersec * 3600);
				double road2NodeCongestedCarTime_h 			= distance.getDistanceRoad2Node() / (carSpeedOnNearestLink_meterpersec * 3600.);
				double road2NodeBikeTime_h					= distance.getDistanceRoad2Node() / this.bikeSpeedMeterPerHour;
				double road2NodeWalkTime_h					= distance.getDistanceRoad2Node() / this.walkSpeedMeterPerHour;
				double road2NodeToll_money 					= getToll(nearestLink); // tnicolai: add this to car disutility ??? depends on the road pricing scheme ...

				// this contains the current toll based on the toll scheme
				double toll_money 							= 0.;
				if ( this.scheme != null ) {
					if(RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType()))
						toll_money = road2NodeToll_money;
					else if( RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()))
						toll_money = road2NodeToll_money * distance.getDistanceRoad2Node();
					else 
						throw new RuntimeException("accessibility not impelemented for requested toll scheme") ;
				}

				for ( int ii = 0 ; ii<gcs.length ; ii++ ) {
					gcs[ii].reset();
				}

				// --------------------------------------------------------------------------------------------------------------
				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate/add their exp(U) contributions:
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					final AggregationObject aggregatedFacility = this.aggregatedOpportunities[i];

					computeAndAddExpUtilContributions(lcptExtFreeSpeedCarTravelTime, lcptExtCongestedCarTravelTime,
							lcptTravelDistance, gcs, fromNode, distance.getDistancePoint2Road(), distance.getDistanceRoad2Node(),
							walkTravelTimeMeasuringPoint2Road_h, road2NodeFreeSpeedTime_h, road2NodeCongestedCarTime_h,
							road2NodeBikeTime_h, road2NodeWalkTime_h, toll_money, aggregatedFacility);
					// yy terribly long argument list :-(.  I extracted this method but further improvements seem possible :-). kai, feb'14
				}
				// --------------------------------------------------------------------------------------------------------------
				// What does the aggregation of the starting locations save if we do the just ended loop for all starting
				// points separately anyways?  Answer: The trees need to be computed only once.  (But one could save more.) kai, feb'14

				// aggregated value
				Map< Modes4Accessibility, Double> accessibilities  = new HashMap< Modes4Accessibility, Double >() ;

				for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
					if ( this.isComputingMode.get(mode) ) {
						if(!useRawSum){ 	// get log sum
							accessibilities.put( mode, inverseOfLogitScaleParameter * Math.log( gcs[mode.ordinal()].getSum() ) ) ;
						} else {
							accessibilities.put( mode, inverseOfLogitScaleParameter * gcs[mode.ordinal()].getSum() ) ;
							// yyyy why _multiply_ with "inverseOfLogitScaleParameter"??  If anything, would need to take the power:
							// a * ln(b) = ln( b^a ).  kai, jan'14
						}
						if(runMode == PARCEL_BASED){ // only for cell-based accessibility computation
							// assign log sums to current starZone[[???]] object and spatial grid
							this.accessibilityGrids.get(mode).setValue( accessibilities.get(mode), origin.getCoord().getX(), origin.getCoord().getY() ) ; 
						}
					}
				}

				if ( this.urbansimMode ) {
					// writing measured accessibilities for current measuring point 
					writeCSVData4Urbansim(origin, fromNode, accessibilities ) ;
					// (I think the above is the urbansim output.  Better not touch it. kai, feb'14)
				}
				
				if(this.zoneDataExchangeListenerList != null){
					for(int i = 0; i < this.zoneDataExchangeListenerList.size(); i++)
						this.zoneDataExchangeListenerList.get(i).getZoneAccessibilities(origin, accessibilities );
				}

			}

		}
		// ========================================================================
	}

	
	private void computeAndAddExpUtilContributions(LeastCostPathTreeExtended lcptExtFreeSpeedCarTravelTime,
			LeastCostPathTreeExtended lcptExtCongestedCarTravelTime, LeastCostPathTree lcptTravelDistance, SumOfExpUtils[] gcs,
			Node fromNode, double distanceMeasuringPoint2Road_meter, double distanceRoad2Node_meter,
			double walkTravelTimeMeasuringPoint2Road_h, double road2NodeFreeSpeedTime_h, double road2NodeCongestedCarTime_h,
			double road2NodeBikeTime_h, double road2NodeWalkTime_h, double toll_money,
			final AggregationObject aggregatedFacility) {
		// get stored network node (this is the nearest node next to an aggregated work place)
		Node destinationNode = aggregatedFacility.getNearestNode();
		Id<Node> nodeID = destinationNode.getId();

		// disutilities on the road network
		double congestedCarDisutility = Double.NaN;
		if(this.isComputingMode.get(Modes4Accessibility.car)) {
			congestedCarDisutility = - lcptExtCongestedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility congested car on road network (including toll)
		}
		double freeSpeedCarDisutility = Double.NaN;
		if(this.isComputingMode.get(Modes4Accessibility.freeSpeed ) ){
			freeSpeedCarDisutility = - lcptExtFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility free speed car on road network (including toll)
		}
		double travelDistance_meter = Double.NaN;
		if( this.isComputingMode.get(Modes4Accessibility.bike) || this.isComputingMode.get( Modes4Accessibility.walk ) ) {
			travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost(); 				// travel link distances on road network for bicycle and walk
		}

		// disutilities to get on or off the network
		double walkDisutilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT) + (distanceMeasuringPoint2Road_meter * betaWalkTD);
		double expVhiWalk = Math.exp(this.logitScaleParameter * walkDisutilityMeasuringPoint2Road);
		double sumExpVjkWalk = aggregatedFacility.getSum();

		// travel times and distances for pseudo pt
		if(this.isComputingMode.get(Modes4Accessibility.pt) ){
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
			gcs[Modes4Accessibility.pt.ordinal()].addExpUtils( expVhkPt );
		}

		// total disutility congested car
		if(this.isComputingMode.get(Modes4Accessibility.car)){
			double congestedCarDisutilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
			double expVijCongestedCar = Math.exp(this.logitScaleParameter * (constCar + congestedCarDisutilityRoad2Node + congestedCarDisutility) );
			double expVhkCongestedCar = expVhiWalk * expVijCongestedCar * sumExpVjkWalk;
			gcs[Modes4Accessibility.car.ordinal()].addExpUtils( expVhkCongestedCar );
		}

		// total disutility free speed car
		if(this.isComputingMode.get(Modes4Accessibility.freeSpeed)){
			double freeSpeedCarDisutilityRoad2Node = (road2NodeFreeSpeedTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
			double expVijFreeSpeedCar = Math.exp(this.logitScaleParameter * (constCar + freeSpeedCarDisutilityRoad2Node + freeSpeedCarDisutility) );
			double expVhkFreeSpeedCar = expVhiWalk * expVijFreeSpeedCar * sumExpVjkWalk;
			gcs[Modes4Accessibility.freeSpeed.ordinal()].addExpUtils( expVhkFreeSpeedCar );
		}

		// total disutility bicycle
		if(this.isComputingMode.get(Modes4Accessibility.bike)){
			double bikeDisutilityRoad2Node = (road2NodeBikeTime_h * betaBikeTT) + (distanceRoad2Node_meter * betaBikeTD); // toll or money ???
			double bikeDisutility = ((travelDistance_meter/this.bikeSpeedMeterPerHour) * betaBikeTT) + (travelDistance_meter * betaBikeTD);// toll or money ???
			double expVijBike = Math.exp(this.logitScaleParameter * (constBike + bikeDisutility + bikeDisutilityRoad2Node));
			double expVhkBike = expVhiWalk * expVijBike * sumExpVjkWalk;
			gcs[Modes4Accessibility.bike.ordinal()].addExpUtils( expVhkBike );
		}

		// total disutility walk
		if(this.isComputingMode.get(Modes4Accessibility.walk)){
			double walkDisutilityRoad2Node = (road2NodeWalkTime_h * betaWalkTT) + (distanceRoad2Node_meter * betaWalkTD);  // toll or money ???
			double walkDisutility = ( (travelDistance_meter / this.walkSpeedMeterPerHour) * betaWalkTT) + ( travelDistance_meter * betaWalkTD);// toll or money ???
			double expVijWalk = Math.exp(this.logitScaleParameter * (constWalk + walkDisutility + walkDisutilityRoad2Node));
			double expVhkWalk = expVhiWalk * expVijWalk * sumExpVjkWalk;
			gcs[Modes4Accessibility.walk.ordinal()].addExpUtils( expVhkWalk );
		}
	}

	
	/**
	 * @param nearestLink
	 */
	double getToll(Link nearestLink) {
		if(scheme != null){
			Cost cost = scheme.getLinkCostInfo(nearestLink.getId(), depatureTime, null, null);
			if(cost != null)
				return cost.amount;
		}
		return 0.;
	}
	
	public void setComputingAccessibilityForMode( Modes4Accessibility mode, boolean val ) {
		this.isComputingMode.put( mode, val ) ;
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
	abstract void writeCSVData4Urbansim( ActivityFacility measurePoint, Node fromNode, Map<Modes4Accessibility, Double> accessibilities ) ;
	// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)
	
	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////


	/**
	 * Set to true if you are using this module in urbansim mode.  With false, some (or all) of the m4u output files are not written
	 * (since they cannot be modified anyways).
	 */
	public void setUrbansimMode(boolean urbansimMode) {
		this.urbansimMode = urbansimMode;
	}

	Map<Modes4Accessibility,SpatialGrid> getAccessibilityGrids() {
		return accessibilityGrids;
	}



	/**
	 * stores travel disutilities for different modes
	 */
	static class SumOfExpUtils {
		// could just use Map<Modes4Accessibility,Double>, but since it is fairly far inside the loop, I leave it with primitive
		// variables on the (unfounded) intuition that this helps with computational speed.  kai, 

		private double sum  	= 0.;

		void reset() {
			this.sum		  	= 0.;
		}

		void addExpUtils(double val){
			this.sum += val;
		}

		double getSum(){
			return this.sum;
		}
	}

}
