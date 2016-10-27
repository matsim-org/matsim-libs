package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thomas, knagel, dziemke
 */
public final class AccessibilityCalculator {

	private static final Logger log = Logger.getLogger(AccessibilityCalculator.class);

	// measuring points (origins) for accessibility calculation
	private ActivityFacilitiesImpl measuringPoints;
	// destinations, opportunities like jobs etc ...
	private AggregationObject[] aggregatedOpportunities;

	private final Map<Modes4Accessibility, AccessibilityContributionCalculator> calculators = new HashMap<>();

	private final ArrayList<FacilityDataExchangeInterface> zoneDataExchangeListeners = new ArrayList<>();

	private boolean useRawSum; //= false;
	private double logitScaleParameter;
	private double inverseOfLogitScaleParameter;
	
	// new
	private double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
//	private double betaWalkTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	// ===
//	private Coord2CoordTimeDistanceTravelDisutility walkTravelDisutility;
	// end new
	
	private double walkSpeedMeterPerHour = -1;

	// counter for warning that capacities are not used so far ... in order not to give the same warning multiple times; dz, apr'14
	private static int cnt = 0;
	
	private Scenario scenario;
	private AccessibilityConfigGroup acg;
	
	
	@Inject
	public AccessibilityCalculator(Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactories, Scenario scenario) {
		this.scenario = scenario;
		this.acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();
		
		useRawSum = acg.isUsingRawSumsWithoutLn();
		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta();
		inverseOfLogitScaleParameter = 1 / (logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		
		// new
		if (planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance() != 0.) {
			log.error("marginal utility of distance for car different from zero but not used in accessibility computations");
		}
		if (planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.pt).getMarginalUtilityOfDistance() != 0.) {
			log.error("marginal utility of distance for pt different from zero but not used in accessibility computations");
		}
		if (planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.bike).getMonetaryDistanceRate() != 0.) {
			log.error("monetary distance cost rate for bike different from zero but not used in accessibility computations");
		}
		if (planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.walk).getMonetaryDistanceRate() != 0.) {
			log.error("monetary distance cost rate for walk different from zero but not used in accessibility computations");
		}
		
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;

		betaWalkTT = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaWalkTD = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();
//		betaWalkTMC = -planCalcScoreConfigGroup.getMarginalUtilityOfMoney();
		// ===
//		TravelTime walkTravelTime = travelTimes.get(TransportMode.walk);
//		this.walkTravelDisutility = (Coord2CoordTimeDistanceTravelDisutility) travelDisutilityFactories.get(TransportMode.walk).createTravelDisutility(
////				travelTimes.get(TransportMode.walk));
//				walkTravelTime);
		// end new
		
		initDefaultContributionCalculators(travelTimes, travelDisutilityFactories, scenario);
	}
	
	private void initDefaultContributionCalculators(Map<String, TravelTime> travelTimes,
			Map<String, TravelDisutilityFactory> travelDisutilityFactories, Scenario scenario) {
		calculators.put(
				Modes4Accessibility.car,
				new NetworkModeAccessibilityExpContributionCalculator(
						travelTimes.get(TransportMode.car),
						travelDisutilityFactories.get(TransportMode.car),
						// new
//						walkTravelDisutility,
						// ===
						null,
						// new
						scenario));
		calculators.put(
				Modes4Accessibility.freeSpeed,
				new NetworkModeAccessibilityExpContributionCalculator(
						new FreeSpeedTravelTime(),
						travelDisutilityFactories.get(TransportMode.car),
						// new
//						walkTravelDisutility,
						// ===
						null,
						// new
						scenario));
		calculators.put(
				Modes4Accessibility.walk,
				new ConstantSpeedAccessibilityExpContributionCalculator(
						TransportMode.walk,
						scenario));
		calculators.put(
				Modes4Accessibility.bike,
				new ConstantSpeedAccessibilityExpContributionCalculator(
						TransportMode.bike,
						scenario));
	}

	
	public void addFacilityDataExchangeListener(FacilityDataExchangeInterface l){
		this.zoneDataExchangeListeners.add(l);
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
	 * Calculates the sum of disutilities Vjk, i.e. the disutilities to reach all opportunities k that are assigned to j from node j
	 *
	 * @param opportunities such as workplaces, either given at a parcel- or zone-level
	 * @param network giving the road network
	 */
	private final void aggregateOpportunities(final ActivityFacilities opportunities, Network network){
		// yyyy this method ignores the "capacities" of the facilities. kai, mar'14
		// for now, we decided not to add "capacities" as it is not needed for current projects. dz, feb'16

		log.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with identical nearest node ...");
		Map<Id<Node>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();
		ProgressBar bar = new ProgressBar( opportunities.getFacilities().size() );

		for ( ActivityFacility opportunity : opportunities.getFacilities().values() ) {
			bar.update();

			Node nearestNode = NetworkUtils.getNearestNode(((Network)network),opportunity.getCoord());
			
			// new
//			double walkUtility = -this.walkTravelDisutility.getCoord2CoordTravelDisutility(opportunity.getCoord(), nearestNode.getCoord());
//			double expVjk = Math.exp(this.logitScaleParameter * walkUtility);
			// ===
			// get Euclidian distance to nearest node
			double distance_meter 	= NetworkUtils.getEuclideanDistance(opportunity.getCoord(), nearestNode.getCoord());
			double VjkWalkTravelTime	= this.betaWalkTT * (distance_meter / this.walkSpeedMeterPerHour);
			double VjkWalkDistance 		= this.betaWalkTD * distance_meter;

			double expVjk				= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkDistance ) );
			// end new
			
			// add Vjk to sum
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
			jco.addObject(opportunity.getId(), expVjk);
		}
		log.info("Aggregated " + opportunities.getFacilities().size() + " number of opportunities to " + opportunityClusterMap.size() + " nodes.");
		this.aggregatedOpportunities = opportunityClusterMap.values().toArray(new AggregationObject[opportunityClusterMap.size()]);
	}

	
	public final void computeAccessibilities( Double departureTime, ActivityFacilities opportunities) {
		aggregateOpportunities(opportunities, scenario.getNetwork());

		Map<Modes4Accessibility,ExpSum> expSums = new HashMap<>() ;
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			expSums.put(mode, new ExpSum());
		}

		// Condense measuring points (origins) that have the same nearest node on the network
		Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedOrigins = aggregateMeasurePointsWithSameNearestNode();
		
		log.info("Now going through all origins:");
		ProgressBar bar = new ProgressBar(aggregatedOrigins.size());
		
		// go through all nodes (keys) that have a measuring point (origin) assigned
		for (Id<Node> nodeId : aggregatedOrigins.keySet()) {
			bar.update();

			Node fromNode = scenario.getNetwork().getNodes().get(nodeId);

			for (AccessibilityContributionCalculator calculator : calculators.values()) {
				calculator.notifyNewOriginNode(fromNode, departureTime);
			}

			// get list with origins that are assigned to "fromNode"
			for ( ActivityFacility origin : aggregatedOrigins.get( nodeId ) ) {
				assert( origin.getCoord() != null );
				
				for ( ExpSum expSum : expSums.values() ) {
					expSum.reset();
				}

				// --------------------------------------------------------------------------------------------------------------
				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate/add their exp(U) contributions:
				for (final AggregationObject aggregatedFacility : this.aggregatedOpportunities) {
					computeAndAddExpUtilContributions( expSums, origin, aggregatedFacility, departureTime );
					// yyyy might be nicer to not pass gcs into the method. kai, oct'16
				}
				// --------------------------------------------------------------------------------------------------------------
				// What does the aggregation of the starting locations save if we do the just ended loop for all starting
				// points separately anyways?  Answer: The trees need to be computed only once.  (But one could save more.) kai, feb'14

				// aggregated value
				Map< Modes4Accessibility, Double> accessibilities  = new HashMap<>() ;

				for ( Modes4Accessibility mode : acg.getIsComputingMode() ) {
					// TODO introduce here a config parameter "computation mode" that can be set to "rawSum", "minimum" or "exponential/logsum/hansen", dz, sept'16
					if(!useRawSum){
						accessibilities.put( mode, inverseOfLogitScaleParameter * Math.log( expSums.get(mode).getSum() ) ) ;
					} else {
						// this was used by IVT within SustainCity.  Not sure if we should maintain this; they could, after all, just exp the log results. kai, may'15
						accessibilities.put( mode, expSums.get(mode).getSum() ) ;
					}
				}

				for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
					//log.info("here");
					zoneDataExchangeInterface.setFacilityAccessibilities(origin, departureTime, accessibilities);
				}
			}
		}
		for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
			zoneDataExchangeInterface.finish();
		}
	}

	
	/**
	 * This method condenses measuring points (origins) that have the same nearest node on the network
	 */
	private Map<Id<Node>, ArrayList<ActivityFacility>> aggregateMeasurePointsWithSameNearestNode() {
		Map<Id<Node>,ArrayList<ActivityFacility>> aggregatedOrigins = new ConcurrentHashMap<>();
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {

			// determine nearest network node (from- or toNode) based on the link
			Node fromNode = NetworkUtils.getCloserNodeOnLink(measuringPoint.getCoord(),
					NetworkUtils.getNearestLinkExactly(scenario.getNetwork(), measuringPoint.getCoord()));

			// this is used as a key for hash map lookups
			Id<Node> nodeId = fromNode.getId();

			// create new entry if key does not exist!
			if(!aggregatedOrigins.containsKey(nodeId)) {
				aggregatedOrigins.put(nodeId, new ArrayList<ActivityFacility>());
			}
			// assign measure point (origin) to it's nearest node
			aggregatedOrigins.get(nodeId).add(measuringPoint);
		}
		log.info("Number of measurement points (origins): " + measuringPoints.getFacilities().values().size());
		log.info("Number of aggregated measurement points (origins): " + aggregatedOrigins.size());
		return aggregatedOrigins;
	}

	
	private void computeAndAddExpUtilContributions( Map<Modes4Accessibility,ExpSum> expSums, ActivityFacility origin, 
			final AggregationObject aggregatedFacility, Double departureTime) 
	{
		for ( Map.Entry<Modes4Accessibility, AccessibilityContributionCalculator> calculatorEntry : calculators.entrySet() ) {
			final Modes4Accessibility mode = calculatorEntry.getKey();

			if ( !this.acg.getIsComputingMode().contains(mode) ) {
				continue; // XXX should be configured by adding only the relevant calculators
			}

			final double expVhk = calculatorEntry.getValue().computeContributionOfOpportunity( origin , aggregatedFacility, departureTime );

			expSums.get(mode).addExpUtils( expVhk );
		}
	}

	
	@Deprecated // should be replaced by something like what follows after 
	public final void setComputingAccessibilityForMode( Modes4Accessibility mode, boolean val ) {
		this.acg.setComputingAccessibilityForMode(mode, val);
	}
	
	public final void putAccessibilityCalculator( Modes4Accessibility mode, AccessibilityContributionCalculator calc ) {
		this.calculators.put( mode , calc ) ;
	}
	
	
	public Set<Modes4Accessibility> getIsComputingMode() {
		return this.acg.getIsComputingMode();
	}

	
	/**
	 * stores travel disutilities for different modes
	 */
	static class ExpSum {
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

	
	public final void setPtMatrix(PtMatrix ptMatrix) {
		calculators.put(
				Modes4Accessibility.pt,
				PtMatrixAccessibilityContributionCalculator.create(
						ptMatrix,
						scenario.getConfig()));
	}

	
	public void setMeasuringPoints(ActivityFacilitiesImpl measuringPoints) {
		this.measuringPoints = measuringPoints;
	}
}