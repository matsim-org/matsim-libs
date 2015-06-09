package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

/**
 * @author thibautd
 */
public class ConstantSpeedAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger log = Logger.getLogger( ConstantSpeedAccessibilityContributionCalculator.class );

	// estimates travel time by a constant speed along network,
	// considering all links (including highways, which seems to be realistic in south africa,
	// but less elsewhere)
	private final LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( new FreeSpeedTravelTime(), new LinkLengthTravelDisutility());

	private final Scenario scenario;
	private final double departureTime;

	private double logitScaleParameter;
	private double betaTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist:

	private double constant;

	private double speedMeterPerHour = -1;

	private Node fromNode = null;

	public ConstantSpeedAccessibilityContributionCalculator(
			final String mode,
			final Scenario scenario) {
		this.scenario = scenario;

		AccessibilityConfigGroup moduleAPCM =
				ConfigUtils.addOrGetModule(
						scenario.getConfig(),
						AccessibilityConfigGroup.GROUP_NAME,
						AccessibilityConfigGroup.class);
		this.departureTime = moduleAPCM.getTimeOfDay();

		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore() ;

		if ( planCalcScoreConfigGroup.getOrCreateModeParams( mode ).getMonetaryDistanceCostRate() != 0. ) {
			log.error( "monetary distance cost rate for "+mode+" different from zero but not used in accessibility computations");
		}

		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta() ;
		speedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get( mode ) * 3600.;

		final PlanCalcScoreConfigGroup.ModeParams modeParams = planCalcScoreConfigGroup.getOrCreateModeParams( mode );
		betaTT = modeParams.getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaTD = modeParams.getMarginalUtilityOfDistance();

		constant = modeParams.getConstant();
	}

	@Override
	public void notifyNewOriginNode(Node fromNode) {
		this.fromNode = fromNode;
		this.lcptTravelDistance.calculate(scenario.getNetwork(), fromNode, departureTime);
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination) {
		// get the nearest link:
        Link nearestLink = ((NetworkImpl)scenario.getNetwork()).getNearestLinkExactly(origin.getCoord());

        // captures the distance (as walk time) between the origin via the link to the node:
        Distances distance = NetworkUtil.getDistances2Node(origin.getCoord(), nearestLink, fromNode);

		// get stored network node (this is the nearest node next to an aggregated work place)
		Node destinationNode = destination.getNearestNode();

		double road2NodeBikeTime_h					= distance.getDistanceRoad2Node() / speedMeterPerHour;
		double travelDistance_meter = lcptTravelDistance.getTree().get(destinationNode.getId()).getCost(); 				// travel link distances on road network for bicycle and walk
		double bikeDisutilityRoad2Node = (road2NodeBikeTime_h * betaTT) + (distance.getDistanceRoad2Node() * betaTD); // toll or money ???
		double bikeDisutility = ((travelDistance_meter/ speedMeterPerHour) * betaTT) + (travelDistance_meter * betaTD);// toll or money ???
		return Math.exp(logitScaleParameter * (constant + bikeDisutility + bikeDisutilityRoad2Node));
	}
}
