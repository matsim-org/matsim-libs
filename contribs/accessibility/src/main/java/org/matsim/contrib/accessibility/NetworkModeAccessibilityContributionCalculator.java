package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Coord2CoordTimeDistanceTravelDisutility;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
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
	private final TravelTime travelTime;
	// new dz
	private final Coord2CoordTimeDistanceTravelDisutility walkTravelDisutility;
	//

//	private final double departureTime;
	private final double betaWalkTT;
	private final double betaWalkTD;
	private final double walkSpeedMeterPerHour;

	private Node fromNode = null;
	private final LeastCostPathTreeExtended lcpt;

	
	public NetworkModeAccessibilityContributionCalculator(
			final TravelTime travelTime,
			final TravelDisutilityFactory travelDisutilityFactory,
			final Coord2CoordTimeDistanceTravelDisutility walkTravelDisutility,
			//
			final Scenario scenario){
		this.scenario = scenario;

		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();
		this.scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
		this.travelTime = travelTime;		

		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime,	planCalcScoreConfigGroup);
		// new dz
		System.err.println("travelDisutility = " + travelDisutility);
		
		this.walkTravelDisutility = walkTravelDisutility;
		//
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
		Link nearestLink = ((NetworkImpl)scenario.getNetwork()).getNearestLinkExactly(origin.getCoord());
//		Link nearestLink = NetworkUtils.getNearestLink(scenario.getNetwork(), origin.getCoord()); // TODO this would actually be more correct, dz, feb'16

		// captures the distance (as walk time) between the origin via the link to the node:
		Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);

		
		// === WALK TO NETWORK: ===
		// TODO: extract this walk part?
		// In the state found before modularization (june 15), this was anyway not consistent accross modes
		// (different for PtMatrix), pointing to the fact that making this mode-specific might make sense.
		// distance to road, and then to node.  (comment by thibaut?)
		double walkTravelTimeMeasuringPoint2Road_h 	= distance.getDistancePoint2Road() / this.walkSpeedMeterPerHour;
		//
		System.err.println("origin.getCoord() = " + origin.getCoord() + " -- destination.getNearestNode() = " + destination.getNearestNode());
		System.err.println("destination.getNumberOfObjects() = " + destination.getNumberOfObjects());
		System.err.println("distance.getDistancePoint2Road() = " + distance.getDistancePoint2Road());
//		System.err.println("this.walkSpeedMeterPerHour = " + this.walkSpeedMeterPerHour);
//		System.err.println("walkTravelTimeMeasuringPoint2Road_h = " + walkTravelTimeMeasuringPoint2Road_h);
		//

		// disutilities to get on or off the network
		double walkDisutilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT) + (distance.getDistancePoint2Road() * betaWalkTD);
		//
		System.err.println("walkDisutilityMeasuringPoint2Road = " + walkDisutilityMeasuringPoint2Road);
		//

		// dzdzdz: replace the above by coord2coordTravelDisutility
		
		// new dz
		Coord projectionCoord = CoordUtils.orthogonalProjectionOnLineSegment(nearestLink.getFromNode().getCoord(), nearestLink.getToNode().getCoord(), origin.getCoord());
		System.err.println("nearestLink.getFromNode().getCoord() = " + nearestLink.getFromNode().getCoord() + 
				" -- nearestLink.getToNode().getCoord() = " + nearestLink.getToNode().getCoord());
		System.err.println("NEW distance coord - from coord = " + CoordUtils.calcDistance(origin.getCoord(), nearestLink.getFromNode().getCoord()) + 
				" -- NEW distance coord - to coord = " + CoordUtils.calcDistance(origin.getCoord(), nearestLink.getToNode().getCoord()));
		System.err.println("projectionCoord = " + projectionCoord);
		double walkUtility = -this.walkTravelDisutility.getCoord2CoordTravelDisutility(origin.getCoord(), projectionCoord);
		
//		walkDisutilityMeasuringPoint2Road = walkUtility;
		System.err.println("NEW walkDisutility = " + walkUtility);
		System.err.println("#############");
		// end new
		
		// === NETWORK ENTRY TO FIRST NODE: ===

		// this contains the current toll based on the toll scheme
		double road2NodeToll_money = getToll(nearestLink, scheme, departureTime); // tnicolai: add this to car disutility ??? depends on the road pricing scheme ...
		double toll_money 							= 0.;
		if ( scheme != null ) {
			if(RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType()))
				toll_money = road2NodeToll_money;
			else if( RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType()))
				toll_money = road2NodeToll_money * distance.getDistanceRoad2Node();
			else
				throw new RuntimeException("accessibility not impelemented for requested toll scheme") ;
		}

		// travel time in hours to get from link enter point (position on a link given by orthogonal projection from measuring point) to the corresponding node
		double carSpeedOnNearestLink_meterpersec= nearestLink.getLength() / travelTime.getLinkTravelTime(nearestLink, departureTime, null, null);
		double road2NodeCongestedCarTime_h 			= distance.getDistanceRoad2Node() / (carSpeedOnNearestLink_meterpersec * 3600.);
		double congestedCarDisutilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT) + (distance.getDistanceRoad2Node() * betaCarTD) + (toll_money * betaCarTMC);
		
		// dzdzdz: replace the above by link disutility multiplied by fraction of link that is used according to the entry point.  (toll should be in there automatically??)

		
		// === FIRST NODE TO AGGREGATION OBJECT: ===
		
		// get stored network node (this is the nearest node next to an aggregated work place)
		Node destinationNode = destination.getNearestNode();
		
		double congestedCarDisutility = - lcpt.getTree().get(destinationNode.getId()).getCost();	// travel disutility congested car on road network (including toll)

		final double sumExpVjkWalk = destination.getSum();
		return Math.exp(logitScaleParameter * (walkDisutilityMeasuringPoint2Road + congestedCarDisutilityRoad2Node + constCar + congestedCarDisutility) ) * sumExpVjkWalk;
		
	}

	private static double getToll( final Link nearestLink, final RoadPricingScheme scheme, final double departureTime) {
		if(scheme != null){
			RoadPricingSchemeImpl.Cost cost = scheme.getLinkCostInfo(nearestLink.getId(), departureTime, null, null);
			if(cost != null) {
				return cost.amount;
			}
		}
		return 0.;
	}
}
