package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.dvrp.router.DijkstraTree;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
 public final class NetworkModeAccessibilityExpContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger log = Logger.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	@Deprecated // yyyy should be possible to get this from car travel disutility
	private RoadPricingScheme scheme ;

	private double logitScaleParameter;

	@Deprecated // yyyy should be possible to get this from car travel disutility
	private double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	@Deprecated // yyyy should be possible to get this from car travel disutility
	private double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	@Deprecated // yyyy should be possible to get this from car travel disutility
	private double betaCarTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()

	private double constCar;

	private String mode;
	private Scenario scenario;
	private Network network;
	private Network subNetwork;
	private final TravelTime travelTime;
	private final TravelDisutilityFactory travelDisutilityFactory;

	private double betaWalkTT;
	private double betaWalkTD;
	private double walkSpeed_m_s;

	private Node fromNode = null;
	private LeastCostPathTreeExtended lcpt;
	//private final DijkstraTree dijkstraTree;
	//private final MultiNodePathCalculator multiNodePathCalculator;
	//private ImaginaryNode aggregatedToNodes;

	Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<Node>, AggregationObject> aggregatedOpportunities;


	public NetworkModeAccessibilityExpContributionCalculator(String mode, final TravelTime travelTime, final TravelDisutilityFactory travelDisutilityFactory, Scenario scenario) {
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;

		this.mode = mode;

		this.scenario = scenario;
		this.network = scenario.getNetwork();

		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();
		this.scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );

		Gbl.assertNotNull(travelDisutilityFactory);
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);

		this.lcpt = new LeastCostPathTreeExtended(travelTime, travelDisutility, this.scheme);
		//this.dijkstraTree = new DijkstraTree(network, travelDisutility, travelTime);
		//FastMultiNodeDijkstraFactory fastMultiNodeDijkstraFactory = new FastMultiNodeDijkstraFactory(true);
		//this.multiNodePathCalculator = (MultiNodePathCalculator) fastMultiNodeDijkstraFactory.createPathCalculator(network, travelDisutility, travelTime);

		if ( planCalcScoreConfigGroup.getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance() != 0. ) {
			log.error( "marginal utility of distance for car different from zero but not used in accessibility computations");
		}

		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta() ;

		betaCarTT 	   	= planCalcScoreConfigGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaCarTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
		betaCarTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ;

		constCar		= planCalcScoreConfigGroup.getModes().get(TransportMode.car).getConstant();

		betaWalkTT		= planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaWalkTD		= planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();

		this.walkSpeed_m_s = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		log.warn("Initializing calculator for mode " + mode + "...");
        log.warn("Full network has " + network.getNodes().size() + " nodes.");
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
        log.warn("sub-network for mode " + modeSet.toString() + " now has " + subNetwork.getNodes().size() + " nodes.");

        this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
		this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());
	}


	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
		this.lcpt.calculateExtended(subNetwork, fromNode, departureTime);
		//this.dijkstraTree.calcLeastCostPathTree(fromNode, departureTime);
		//multiNodePathCalculator.calcLeastCostPath(fromNode, aggregatedToNodes, departureTime, null, null);
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {

//		System.out.println("oring = " + origin.getCoord().getX() + "   " + origin.getCoord().getY());
//		System.out.println("destnode = " + destination.getNearestNode().getCoord().getX() + "   " + destination.getNearestNode().getCoord().getY());
		Link nearestLink = NetworkUtils.getNearestLinkExactly(subNetwork, origin.getCoord());

		// === (1) ORIGIN to LINK to NODE (captures the distance (as walk time) between the origin via the link to the node):
		Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);

		// TODO: extract this walk part?
		// In the state found before modularization (june 15), this was anyway not consistent accross modes
		// (different for PtMatrix), pointing to the fact that making this mode-specific might make sense. (comment by thibaut?)
		double walkTravelTimeMeasuringPoint2Road_h 	= distance.getDistancePoint2Intersection() / (this.walkSpeed_m_s * 3600);

		// (a) disutilities to get on or off the network
		// NEW AV MODE
//		double waitingTime_h = (Double) origin.getAttributes().getAttribute("waitingTime_s") / 3600.;
//		double walkUtilityMeasuringPoint2Road = ((walkTravelTimeMeasuringPoint2Road_h + waitingTime_h) * betaWalkTT)
//					+ (distance.getDistancePoint2Intersection() * betaWalkTD);
		// END NEW AV MODE
		double walkUtilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT)
					+ (distance.getDistancePoint2Intersection() * betaWalkTD);

		// (b) TRAVEL ON NETWORK to FIRST NODE:
		double toll_money = getTollMoney(departureTime, nearestLink, distance);
		double carSpeedOnNearestLink_m_s = nearestLink.getLength() / travelTime.getLinkTravelTime(nearestLink, departureTime, null, null);
		double road2NodeCongestedCarTime_h = distance.getDistanceIntersection2Node() / (carSpeedOnNearestLink_m_s * 3600.);
//		System.out.println("road2NodeCongestedCarTime_h = " + road2NodeCongestedCarTime_h);

		// Note: This is a utility that becomes a disutility when it holds a negative value (as it does)
		double congestedCarUtilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT)
				+ (distance.getDistanceIntersection2Node() * betaCarTD) + (toll_money * betaCarTMC);
//		System.out.println("congestedCarDisutilityRoad2Node = " + congestedCarDisutilityRoad2Node);
//		// yyyyyy dzdzdz: replace the above by link disutility multiplied by fraction of link that is used according to the entry point.  (toll should be in there automatically??)

		// === (2) REMAINING TRAVEL ON NETWORK:
		// Note: This is a utility that becomes a disutility when it holds a negative value (as it does)
		double congestedCarUtility = - lcpt.getTree().get(destination.getNearestNode().getId()).getCost();
		//double congestedCarUtility = - dijkstraTree.getLeastCostPath(destination.getNearestNode()).travelCost;
		//double congestedCarUtility = - multiNodePathCalculator.constructPath(fromNode, destination.getNearestNode(), departureTime).travelCost;


		// System.out.println("congestedCarDisutility = " + congestedCarDisutility);
		// travel disutility congested car on road network (including toll)

		// === (3) Pre-computed effect of all opportunities reachable from destination network node:
		double sumExpVjkWalk = destination.getSum();
//		System.out.println("destination.getSum() = " + destination.getSum());
		// works because something like exp(A+c1) + exp(A+c2) + ... = exp(A) * [ exp(c1) + exp(c2) + ...]  =: exp(A) * sumExpVjkWalk

		// === (4) Everything together:
		// note that exp(a+b) = exp(a) * exp(b), so for b the exponentiation has already been done.
		return Math.exp(this.logitScaleParameter * (walkUtilityMeasuringPoint2Road + constCar + congestedCarUtilityRoad2Node
				+ congestedCarUtility) ) * sumExpVjkWalk;
	}


	@Deprecated // yyyy should be possible to get this from car travel disutility
	private double getTollMoney(Double departureTime, Link nearestLink, Distances distance) {
		// yy there should be a way of doing this that is closer to the mobsim (and thus more general/automatic).  kai, jun'16

		double result = 0. ;
		if(scheme != null){
			RoadPricingSchemeImpl.Cost cost = scheme.getLinkCostInfo(nearestLink.getId(), departureTime, null, null);
			if(cost != null) {
				result = cost.amount;
			}
			if(RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType())) {
				// do nothing
			} else if( RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
				result *= distance.getDistanceIntersection2Node();
			} else {
				throw new RuntimeException("accessibility not impelemented for requested toll scheme") ;
			}
		}
		return result ;
	}


	// Needed if MultiNodePathCalculator is used as router -- experimental
//	public void setToNodes(ImaginaryNode aggregatedToNodes) {
//		log.warn("Setting toNodes.");
//		this.aggregatedToNodes = aggregatedToNodes;
//	}


	@Override
	public NetworkModeAccessibilityExpContributionCalculator duplicate() {
		log.info("Creating another NetworkModeAccessibilityExpContributionCalculator object.");
		NetworkModeAccessibilityExpContributionCalculator networkModeAccessibilityExpContributionCalculator =
				new NetworkModeAccessibilityExpContributionCalculator(this.mode, this.travelTime, this.travelDisutilityFactory, this.scenario);
		networkModeAccessibilityExpContributionCalculator.subNetwork = this.subNetwork;
		networkModeAccessibilityExpContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		networkModeAccessibilityExpContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return networkModeAccessibilityExpContributionCalculator;
	}


	@Override
    public Map<Id<Node>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
        return aggregatedMeasurePoints;
    }


	@Override
	public Map<Id<Node>, AggregationObject> getAgregatedOpportunities() {
		return aggregatedOpportunities;
	}
}