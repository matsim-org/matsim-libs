package org.matsim.contrib.accessibility;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

/**
 * @author thibautd, dziemke
 */
final class EstimatedDrtAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	private final String mode;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final Scenario scenario;

	private final TravelDisutility travelDisutility;
	private final ScoringConfigGroup scoringConfigGroup;
	private final NetworkConfigGroup networkConfigGroup;

	private Network subNetwork;

	private final double betaWalkTT;

	private final double betaDrtTT;
	private final double walkSpeed_m_s;

	private Node fromNode = null;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

	private final LeastCostPathCalculator router;


	public EstimatedDrtAccessibilityContributionCalculator(String mode, final TravelTime travelTime, final TravelDisutilityFactory travelDisutilityFactory, Scenario scenario) {
		this.mode = mode;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.scenario = scenario;
		this.scoringConfigGroup = scenario.getConfig().scoring();
		this.networkConfigGroup = scenario.getConfig().network();

		Gbl.assertNotNull(travelDisutilityFactory);
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);
		this.router = new SpeedyALTFactory().createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);



		betaWalkTT = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		betaDrtTT = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();

		this.walkSpeed_m_s = scenario.getConfig().routing().getTeleportedModeSpeeds().get(TransportMode.walk);

//		this.drtEstimator = new EuclideanDistanceBasedDrtEstimator(scenario.getNetwork(), 1.2, 0.0842928, 337.1288522,  5 * 60, 0, 0, 0);
//		this.drtEstimator = DetourBasedDrtEstimator.normalDistributed(337.1288522, 0.0842928, 0., 0. * 60, 0);



	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		LOG.warn("Initializing calculator for mode " + mode + "...");
		LOG.warn("Full network has " + scenario.getNetwork().getNodes().size() + " nodes.");
		subNetwork = NetworkUtils.createNetwork(networkConfigGroup);
		Set<String> modeSet = new HashSet<>();
		modeSet.add(TransportMode.car);
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		filter.filter(subNetwork, modeSet);
		if (subNetwork.getNodes().size() == 0) {
			throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");
		}
		LOG.warn("sub-network for mode " + modeSet + " now has " + subNetwork.getNodes().size() + " nodes.");

		this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
		this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());


	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		this.fromNode = subNetwork.getNodes().get(fromNodeId);
//		this.lcpt.calculate(subNetwork, fromNode, departureTime);

	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
												   Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		double expSum = 0.;


		// Access
//		new ClosestAccessEgressFacilityFinder(2000, scenario.getNetwork(), facilityQuadTree);

		// DRT Trip


		// Egress



		// Old Approach: nearest link...
		Link nearestLink = NetworkUtils.getNearestLinkExactly(subNetwork, origin.getCoord());
		Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);
		double walkTravelTimeMeasuringPoint2Road_h = distance.getDistancePoint2Intersection() / (this.walkSpeed_m_s * 3600);
		// Orthogonal walk to nearest link
		double walkUtilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT);
		// Travel on section of first link to first node
		double distanceFraction = distance.getDistanceIntersection2Node() / nearestLink.getLength();
		double congestedCarUtilityRoad2Node = -travelDisutility.getLinkTravelDisutility(nearestLink, departureTime, null, null) * distanceFraction;

		// Combine all utility components (using the identity: exp(a+b) = exp(a) * exp(b))
		double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.drt, scoringConfigGroup); // TODO: update from car to drt

		for (final AggregationObject destination : aggregatedOpportunities.values()) {

			// utility during DRT ride
//			DrtEstimator.Estimate estimate = drtEstimator.estimate(origin.getCoord(), destination.getNearestBasicLocation().getCoord(), OptionalTime.defined(departureTime));

//			DrtRoute route = new DrtRoute(Id.createLinkId("xxx"), Id.createLinkId("yyy"));

			Node destinationNode = (Node) destination.getNearestBasicLocation();
//			double directRideTime = VrpPaths.calcAndCreatePath(nearestLink, toLink, departureTime, router, travelTime).getTravelTime();

//			LeastCostPathCalculator.Path vrpPath = router.calcLeastCostPath(nearestLink.getToNode(), toLink.getFromNode(), departureTime, null, null);
			LeastCostPathCalculator.Path vrpPath = router.calcLeastCostPath(nearestLink.getToNode(), destinationNode, departureTime, null, null);

			List<Link> links = vrpPath.links;
//			links.add(toLink); TODO: do we need this?
			double directRideDistance = links.stream().mapToDouble(Link::getLength).sum();

//			route.setDirectRideTime(directRideTime);
//			route.setDistance(directRideDistance);
//			route.setStartLinkId(nearestLink.getId());
//			route.setEndLinkId(toLink.getId());
//			DrtEstimator.Estimate estimate = drtEstimator.estimate(route, OptionalTime.defined(departureTime));

			double waitTime = 103.34;
			double rideTime = 47.84 + 0.1087 * directRideDistance;
			double totalTime = (waitTime + rideTime) / 3600;
			double utilityDrt = betaDrtTT * totalTime;

			// Pre-computed effect of all opportunities reachable from destination network node
			double sumExpVjkWalk = destination.getSum();

			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() *
				(walkUtilityMeasuringPoint2Road + modeSpecificConstant
				+ congestedCarUtilityRoad2Node + utilityDrt))
				* sumExpVjkWalk;
		}
		return expSum;
	}


	@Override
	public EstimatedDrtAccessibilityContributionCalculator duplicate() {
		LOG.info("Creating another EstimatedDrtAccessibilityContributionCalculator object.");
		EstimatedDrtAccessibilityContributionCalculator estimatedDrtAccessibilityContributionCalculator =
			new EstimatedDrtAccessibilityContributionCalculator(this.mode, this.travelTime, this.travelDisutilityFactory, this.scenario);
		estimatedDrtAccessibilityContributionCalculator.subNetwork = this.subNetwork;
		estimatedDrtAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
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
}
