package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd, dziemke
 */
final class NetworkModeAccessibilityExpContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = Logger.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	private final String mode;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final Scenario scenario;

	private final TravelDisutility travelDisutility;
	private final PlanCalcScoreConfigGroup planCalcScoreConfigGroup;

	private Network subNetwork;

	private double betaWalkTT;
	private double walkSpeed_m_s;

	private Node fromNode = null;
	private LeastCostPathTreeExtended lcpt;
	//private final DijkstraTree dijkstraTree;
	//private final MultiNodePathCalculator multiNodePathCalculator;
	//private ImaginaryNode aggregatedToNodes;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;



	public NetworkModeAccessibilityExpContributionCalculator(String mode, final TravelTime travelTime, final TravelDisutilityFactory travelDisutilityFactory, Scenario scenario) {
		this.mode = mode;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.scenario = scenario;

		Gbl.assertNotNull(travelDisutilityFactory);
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);

		planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();

		RoadPricingScheme scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
		this.lcpt = new LeastCostPathTreeExtended(travelTime, travelDisutility, scheme);
		//this.dijkstraTree = new DijkstraTree(network, travelDisutility, travelTime);
		//FastMultiNodeDijkstraFactory fastMultiNodeDijkstraFactory = new FastMultiNodeDijkstraFactory(true);
		//this.multiNodePathCalculator = (MultiNodePathCalculator) fastMultiNodeDijkstraFactory.createPathCalculator(network, travelDisutility, travelTime);

		betaWalkTT = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();

		this.walkSpeed_m_s = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		LOG.warn("Initializing calculator for mode " + mode + "...");
		LOG.warn("Full network has " + scenario.getNetwork().getNodes().size() + " nodes.");
        subNetwork = NetworkUtils.createNetwork();
        Set<String> modeSet = new HashSet<>();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
        if (mode.equals(Modes4Accessibility.freespeed.name())) {
        	modeSet.add(TransportMode.car);
		} else {
        	modeSet.add(mode);
		}
        filter.filter(subNetwork, modeSet);
        if (subNetwork.getNodes().size() == 0) {throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");}
		LOG.warn("sub-network for mode " + modeSet.toString() + " now has " + subNetwork.getNodes().size() + " nodes.");

        this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
		this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());
	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		this.fromNode = subNetwork.getNodes().get(fromNodeId);
		this.lcpt.calculateExtended(subNetwork, fromNode, departureTime);
		//this.dijkstraTree.calcLeastCostPathTree(fromNode, departureTime);
		//multiNodePathCalculator.calcLeastCostPath(fromNode, aggregatedToNodes, departureTime, null, null);
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
			Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		double expSum = 0.;

		for (final AggregationObject destination : aggregatedOpportunities.values()) {
			Link nearestLink = NetworkUtils.getNearestLinkExactly(subNetwork, origin.getCoord());

			// Orthogonal walk to nearest link
			Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);
			double walkTravelTimeMeasuringPoint2Road_h = distance.getDistancePoint2Intersection() / (this.walkSpeed_m_s * 3600);
			double walkUtilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT);

			// NEW AV MODE
			//		double waitingTime_h = (Double) origin.getAttributes().getAttribute("waitingTime_s") / 3600.;
			//		double walkUtilityMeasuringPoint2Road = ((walkTravelTimeMeasuringPoint2Road_h + waitingTime_h) * betaWalkTT)
			//					+ (distance.getDistancePoint2Intersection() * betaWalkTD);
			// END NEW AV MODE

			// Travel on section of first link to first node
			double distanceFraction = distance.getDistanceIntersection2Node() / nearestLink.getLength();
			double congestedCarUtilityRoad2Node = -travelDisutility.getLinkTravelDisutility(nearestLink, departureTime, null, null) * distanceFraction;

			// Remaining travel on network
			double congestedCarUtility = -lcpt.getTree().get(((Node) destination.getNearestBasicLocation()).getId()).getCost();
			//double congestedCarUtility = - dijkstraTree.getLeastCostPath(destination.getNearestNode()).travelCost;
			//double congestedCarUtility = - multiNodePathCalculator.constructPath(fromNode, destination.getNearestNode(), departureTime).travelCost;

			// Pre-computed effect of all opportunities reachable from destination network node
			double sumExpVjkWalk = destination.getSum();

			// Combine all utility components (using the identity: exp(a+b) = exp(a) * exp(b))
			double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, planCalcScoreConfigGroup);
			expSum += Math.exp(this.planCalcScoreConfigGroup.getBrainExpBeta() * (walkUtilityMeasuringPoint2Road + modeSpecificConstant
					+ congestedCarUtilityRoad2Node + congestedCarUtility)) * sumExpVjkWalk;
		}
		return expSum;
	}


	// Needed if MultiNodePathCalculator is used as router -- experimental
//	public void setToNodes(ImaginaryNode aggregatedToNodes) {
//		log.warn("Setting toNodes.");
//		this.aggregatedToNodes = aggregatedToNodes;
//	}


	@Override
	public NetworkModeAccessibilityExpContributionCalculator duplicate() {
		NetworkModeAccessibilityExpContributionCalculator networkModeAccessibilityExpContributionCalculator =
				new NetworkModeAccessibilityExpContributionCalculator(this.mode, this.travelTime, this.travelDisutilityFactory, this.scenario);
		networkModeAccessibilityExpContributionCalculator.subNetwork = this.subNetwork;
		networkModeAccessibilityExpContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		networkModeAccessibilityExpContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return networkModeAccessibilityExpContributionCalculator;
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
