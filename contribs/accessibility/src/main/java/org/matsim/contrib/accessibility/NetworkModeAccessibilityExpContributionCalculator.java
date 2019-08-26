package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.roadpricing.RoadPricingCost;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.contrib.roadpricing.RoadPricingScheme;

/**
 * @author thibautd
 */
 public final class NetworkModeAccessibilityExpContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger log = Logger.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	@Deprecated // yyyy should be possible to get this from car travel disutility
	private final RoadPricingScheme scheme ;

	private final double logitScaleParameter;

	@Deprecated // yyyy should be possible to get this from car travel disutility
	private final double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	@Deprecated // yyyy should be possible to get this from car travel disutility
	private final double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	@Deprecated // yyyy should be possible to get this from car travel disutility
	private final double betaCarTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()

	private final double constCar;

	private final Network network;
	private final TravelTime travelTime;

	private final double betaWalkTT;
	private final double betaWalkTD;
	private final double walkSpeed_m_s;

	private Node fromNode = null;
	private final LeastCostPathTreeExtended lcpt;

	
	public NetworkModeAccessibilityExpContributionCalculator(final TravelTime travelTime,
			final TravelDisutilityFactory travelDisutilityFactory, final Scenario scenario,	final Network network) {		
		this.network = network;

		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();
		this.scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
		this.travelTime = travelTime;		

		Gbl.assertNotNull(travelDisutilityFactory);
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);

		this.lcpt = new LeastCostPathTreeExtended(travelTime, travelDisutility, this.scheme);

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
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
		this.lcpt.calculateExtended(network, fromNode, departureTime);
	}
	
	
	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {

//		System.out.println("oring = " + origin.getCoord().getX() + "   " + origin.getCoord().getY());
//		System.out.println("destnode = " + destination.getNearestNode().getCoord().getX() + "   " + destination.getNearestNode().getCoord().getY());
		Link nearestLink = NetworkUtils.getNearestLinkExactly(network, origin.getCoord());

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
			RoadPricingCost cost = scheme.getLinkCostInfo(nearestLink.getId(), departureTime, null, null );
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
}
