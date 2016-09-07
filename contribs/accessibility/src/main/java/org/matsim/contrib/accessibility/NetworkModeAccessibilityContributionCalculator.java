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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

/**
 * @author thibautd
 */
public class NetworkModeAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger log = Logger.getLogger( NetworkModeAccessibilityContributionCalculator.class );

	private final RoadPricingScheme scheme ;

	private final double logitScaleParameter;
	private final double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private final double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	private final double betaCarTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()

	private final double constCar;

	private final Scenario scenario;
	private final TravelTime ttc;

	private final double betaWalkTT;
	private final double betaWalkTD;
	private final double walkSpeedMeterPerHour;

	private Node fromNode = null;
	private final LeastCostPathTreeExtended lcpt;

	public NetworkModeAccessibilityContributionCalculator(
			final TravelTime ttc,
			final TravelDisutilityFactory travelDisutilityFactory,
			final Scenario scenario){
		this.scenario = scenario;

		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();
		this.scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
		this.ttc = ttc;

		this.lcpt =
				new LeastCostPathTreeExtended(
						ttc,
						travelDisutilityFactory.createTravelDisutility(
								ttc ),
								this.scheme ) ;

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

		this.walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get( TransportMode.walk ) * 3600;
	}


	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
		this.lcpt.calculateExtended(scenario.getNetwork(), fromNode, departureTime);
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {
		// get the nearest link:
		Link nearestLink = NetworkUtils.getNearestLinkExactly(((Network)scenario.getNetwork()),origin.getCoord());

		// === (1) ORIGIN to LINK to NODE:
		// captures the distance (as walk time) between the origin via the link to the node:
		Distances distance = NetworkUtil.getDistances2Node(origin.getCoord(), nearestLink, fromNode);

		// (a) ORIGIN FACILITY to LINK:
		double walkTravelTimeMeasuringPoint2Road_h 	= distance.getDistancePoint2Road() / this.walkSpeedMeterPerHour;
		double walkDisutilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT) + (distance.getDistancePoint2Road() * betaWalkTD);
		double expVhiWalk = Math.exp(this.logitScaleParameter * walkDisutilityMeasuringPoint2Road);
		
		// (b) TRAVEL ON NETWORK to FIRST NODE:
		double toll_money = getTollMoney(departureTime, nearestLink, distance);
		double carSpeedOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, departureTime, null, null);
		double road2NodeCongestedCarTime_h 			= distance.getDistanceRoad2Node() / (carSpeedOnNearestLink_meterpersec * 3600.);
		double congestedCarDisutilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT) 
				+ (distance.getDistanceRoad2Node() * betaCarTD) + (toll_money * betaCarTMC);

		// === (2) REMAINING TRAVEL ON NETWORK:
		double congestedCarDisutility = - lcpt.getTree().get(destination.getNearestNode().getId()).getCost();	
		// travel disutility congested car on road network (including toll)

		// === (3) Pre-computed effect of all opportunities reachable from destination network node:
		double sumExpVjkWalk = destination.getSum();
		// works because something like exp(A+c1) + exp(A+c2) + ... = exp(A) * [ exp(c1) + exp(c2) + ...]  =: exp(A) * sumExpVjkWalk
		
		// === (4) Everything together:
		// note that exp(a+b+c) = exp(a) * exp(b) * exp(c), so for b and c the exponentiation has already been done.
		return Math.exp(logitScaleParameter * (constCar + congestedCarDisutilityRoad2Node + congestedCarDisutility) ) *
				expVhiWalk * sumExpVjkWalk;
	}


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
				result *= distance.getDistanceRoad2Node();
			} else {
				throw new RuntimeException("accessibility not impelemented for requested toll scheme") ;
			}
		}
		return result ;
	}
}
