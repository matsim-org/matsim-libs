package org.matsim.contrib.accessibility;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.*;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author thibautd, dziemke
 */
final class EstimatedDrtAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	private final String mode;
	private final Scenario scenario;


	private final ScoringConfigGroup scoringConfigGroup;

	private Network subNetwork;

	private final double betaDrtTT_h;
	private final double betaDrtDist_m;
	private final double walkSpeed_m_h;
	private final double betaWalkTT_h;
	private final double betaWalkDist_m;
	private Node fromNode = null;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

//	private final LeastCostPathCalculator router;
	TripRouter tripRouter ;


	private DvrpRoutingModule.AccessEgressFacilityFinder stopFinder;

	private final DrtEstimator drtEstimator;


	public EstimatedDrtAccessibilityContributionCalculator(String mode, Scenario scenario, DvrpRoutingModule.AccessEgressFacilityFinder stopFinder, TripRouter tripRouter, DrtEstimator drtEstimator) {

		this.mode = mode;

		this.scenario = scenario;
		this.scoringConfigGroup = scenario.getConfig().scoring();
		this.tripRouter = tripRouter;
		this.stopFinder = stopFinder;
		this.drtEstimator = drtEstimator;

		// drt params
		this.betaDrtTT_h = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaDrtDist_m = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfDistance();

		// walk params
		this.walkSpeed_m_h = scenario.getConfig().routing().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600;
		this.betaWalkTT_h = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaWalkDist_m = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();

		// todo: I added a getter to access the stop finder. Is this ok?
//		stopFinder = ((DvrpRoutingModule) tripRouter.getRoutingModule(TransportMode.drt)).getStopFinder();

	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {

		LOG.warn("Initializing calculator for mode " + mode + "...");


		// Prepare measure points
		aggregatedMeasurePoints = new ConcurrentHashMap<>();
		Gbl.assertNotNull(measuringPoints);
		Gbl.assertNotNull(measuringPoints.getFacilities());

		// todo: should I aggregate the measuring points to the nearest drt stop. Or do we in the near future want to compare the drt routes between multiple access/egress drt stops?
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			Id<ActivityFacility> facilityId = measuringPoint.getId();
			if(!aggregatedMeasurePoints.containsKey(facilityId)) {
				aggregatedMeasurePoints.put(facilityId, new ArrayList<>());
			}
			aggregatedMeasurePoints.get(facilityId).add(measuringPoint);
		}

//		LOG.warn("Full network has " + scenario.getNetwork().getNodes().size() + " nodes.");
//		subNetwork = NetworkUtils.createNetwork(networkConfigGroup);
//		Set<String> modeSet = new HashSet<>();
//		modeSet.add(TransportMode.car);
//		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
//		filter.filter(subNetwork, modeSet);
//		if (subNetwork.getNodes().size() == 0) {
//			throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");
//		}
//		LOG.warn("sub-network for mode " + modeSet + " now has " + subNetwork.getNodes().size() + " nodes.");
//
//		this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);


		// todo: same question as above...
		this.aggregatedOpportunities = aggregateOpportunitiesWithSameNearestDrtStop(opportunities, scenario.getConfig());


	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
//		this.fromNode = subNetwork.getNodes().get(fromNodeId);
//		this.lcpt.calculate(subNetwork, fromNode, departureTime);

	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
												   Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		// initialize sum of utilities
		double expSum = 0.;

		// find closest stop to measuring point.
		Assert.isTrue(stopFinder instanceof ClosestAccessEgressFacilityFinder, "So far, findClosestStop() is only implemented in ClosestAccessEgressFacilityFinder");
		Facility nearestStopAccess = ((ClosestAccessEgressFacilityFinder) stopFinder).findClosestStop(origin);

		// use walk router to calculate access walk time. Since it is a walk trip, there should only be a single leg.
		List<? extends PlanElement> planElementsAccess = tripRouter.calcRoute(TransportMode.walk, origin, nearestStopAccess, departureTime, null, null);
		Leg accessLeg = extractLeg(planElementsAccess, TransportMode.walk);
		double accessTime_h = accessLeg.getTravelTime().seconds() / 3600;
		double accessDist_m = accessLeg.getRoute().getDistance();
		double utility_access = accessTime_h * betaWalkTT_h + accessDist_m * betaWalkDist_m;

		// now we iterate through drt stops, each of which has a set of opportunities connected to it (those which are closest to that stop)
		// we calculate sum of utilities to travel from the origin drt stop to all drt stops that have at least one opportunity close to it
		for (AggregationObject destination : aggregatedOpportunities.values()) {

			// Calculate utility of main drt leg:
			Facility nearestStopEgress = (Facility) destination.getNearestBasicLocation();

			//TODO: replace actual person with a fake person, or find workaround.
			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.car, nearestStopAccess, nearestStopEgress, departureTime, scenario.getPopulation().getPersons().values().stream().findFirst().get(), null);
			Leg mainLeg = extractLeg(planElements, TransportMode.car);
			double directRideDistance_m = mainLeg.getRoute().getDistance();


			// attempt to replace hard coded calculation w/ DRT Estimator
			DrtRoute drtRoute = new DrtRoute(Id.createLinkId("dummyFrom"), Id.createLinkId("dummyTo"));
			drtRoute.setDistance(directRideDistance_m);
			drtRoute.setDirectRideTime(mainLeg.getRoute().getTravelTime().seconds());

			DrtEstimator.Estimate estimate = drtEstimator.estimate(drtRoute, OptionalTime.defined(departureTime));
			double waitTime_s = estimate.waitingTime();
			double rideTime_s = estimate.rideTime();
			// old version, hardcoded
//			{
//				double waitTime_s = 103.34;
//				double rideTime_s = 47.84 + 0.1087 * directRideDistance_m;
//			}
			double totalTime_h = (waitTime_s + rideTime_s) / 3600;
			double utilityDrtTime = betaDrtTT_h * totalTime_h;
			double utilityDrtDistance = betaDrtDist_m * directRideDistance_m; // Todo: this doesn't include the detours


			// Pre-computed effect of all opportunities reachable from destination network node
			double sumExpVjkWalk = destination.getSum();

			double utilityDrtConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.drt, scoringConfigGroup);

			double drtUtility =
				utility_access +
				utilityDrtTime +
				utilityDrtDistance +
				Math.log(sumExpVjkWalk) / scoringConfigGroup.getBrainExpBeta() + // todo should this be included in the comparison?
				utilityDrtConstant;

			// Calculate utility of direct walk
			// todo: why is sumExpVjkWalk included in drtUtility but not in directWalkUtility...
			// should we really be aggregating around drt stops. Should we not handle each opportuninity seperately, so we can actually compare direct walk times?
			List<? extends PlanElement> planElementsDirectWalk = tripRouter.calcRoute(TransportMode.walk, origin, (DrtStopFacilityImpl) destination.getNearestBasicLocation(), departureTime, null, null);
			Leg directWalkLeg = extractLeg(planElementsDirectWalk, TransportMode.walk);
			double directWalkTime_h = directWalkLeg.getTravelTime().seconds() / 3600;
			double directWalkDist_m = directWalkLeg.getRoute().getDistance();
			double directWalkUtility = directWalkTime_h * betaWalkTT_h +
				directWalkDist_m * betaWalkDist_m +
				AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.walk, scoringConfigGroup);

//			final Coord toCoord = destination.getNearestBasicLocation().getCoord();
//			double directDistance_m = CoordUtils.calcEuclideanDistance(origin.getCoord(), toCoord);
////			double directWalkUtility =
//				directDistance_m / walkSpeed_m_h * betaWalkTT_h + // utility based on travel time
//				directDistance_m * betaWalkDist_m + // utility based on travel distance
//				AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.walk, scoringConfigGroup); // ASC

			// Utilities for traveling are generally negative (unless there is a high ASC).
			// We choose walk if it's utility is higher (less negative) than that of DRT
			// the access drt stop is same as the egress drt stop (which means they don't actually take drt)
			double travelUtility;

			if (nearestStopAccess.equals(nearestStopEgress) || directWalkUtility > drtUtility) {
				travelUtility = directWalkUtility;
			} else {
				travelUtility = drtUtility;
			}

			// this is added to the sum of utilities from the measuring point
			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() * travelUtility);

		}
		return expSum;
	}

	private static Leg extractLeg(List<? extends PlanElement> planElementsMain, String mode) {
		List<Leg> legList = planElementsMain.stream().filter(pe -> pe instanceof Leg && ((Leg) pe).getMode().equals(mode)).map(pe -> (Leg) pe).toList();

		if (legList.size() != 1) {
			throw new RuntimeException("for these accessibility calculations, there should be exactly one leg");
		}

		return legList.get(0);
	}


	@Override
	public EstimatedDrtAccessibilityContributionCalculator duplicate() {
		LOG.info("Creating another EstimatedDrtAccessibilityContributionCalculator object.");
		EstimatedDrtAccessibilityContributionCalculator estimatedDrtAccessibilityContributionCalculator =
			new EstimatedDrtAccessibilityContributionCalculator(this.mode, this.scenario, this.stopFinder, this.tripRouter, this.drtEstimator);
		estimatedDrtAccessibilityContributionCalculator.subNetwork = this.subNetwork;
//		estimatedDrtAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		estimatedDrtAccessibilityContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return estimatedDrtAccessibilityContributionCalculator;
	}


	@Override
	public Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
		return aggregatedMeasurePoints;
	}


	@Override
	public Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities() {
		return aggregatedOpportunities;
	}


	private Map<Id<? extends BasicLocation>, AggregationObject> aggregateOpportunitiesWithSameNearestDrtStop(
		final ActivityFacilities opportunities, Config config ) {

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with same nearest drt stop...");
		Map<Id<? extends BasicLocation>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {

			Assert.isTrue(stopFinder instanceof ClosestAccessEgressFacilityFinder, "So far, findClosestStop() is only implemented in ClosestAccessEgressFacilityFinder");
			DrtStopFacility nearestStop = (DrtStopFacility) ((ClosestAccessEgressFacilityFinder) stopFinder).findClosestStop(opportunity);

			// todo: should I use the euclidean distance or the distance w/ beeline factor?
			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.walk, nearestStop, opportunity, 10 * 3600., null, null);// departure time shouldn't matter for walk
			Leg leg = extractLeg(planElements, TransportMode.walk);
			double egressTime_s = leg.getTravelTime().seconds();

			double VjkWalkTravelTime = egressTime_s / 3600 * betaWalkTT_h; // a.k.a utility_egress

			double expVjk = Math.exp(config.scoring().getBrainExpBeta() * VjkWalkTravelTime);


			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get(nearestStop.getId());
			if (jco == null) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestStop, 0.);
				opportunityClusterMap.put(nearestStop.getId(), jco);
			}
			if (acg.isUseOpportunityWeights()) {
				if (opportunity.getAttributes().getAttribute( Labels.WEIGHT ) == null) {
					throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + Labels.WEIGHT + ".");
				} else {
					double weight = Double.parseDouble(opportunity.getAttributes().getAttribute( Labels.WEIGHT ).toString() );
					jco.addObject(opportunity.getId(), expVjk * Math.pow(weight, acg.getWeightExponent()));
				}
			} else {
				jco.addObject(opportunity.getId(), expVjk);
			}
		}
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " drt stops.");
		return opportunityClusterMap;
	}
}
