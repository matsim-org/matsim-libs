package org.matsim.contrib.accessibility;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 */
public class MatrixBasedPtAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private final PtMatrix ptMatrix;
	private final RoutingModule routingModule = null ;

	private Node fromNode = null;

	private double logitScaleParameter;

	private final double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private final double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist:
	private final double betaPtTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private final double betaPtTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()

	private final double constPt;

	public MatrixBasedPtAccessibilityContributionCalculator(
			final PtMatrix ptMatrix,
			final Config config ) {
		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
		this.ptMatrix = ptMatrix;
		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta() ;

		betaWalkTT		= planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaWalkTD		= planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();

		betaPtTT		= planCalcScoreConfigGroup.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaPtTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getModes().get(TransportMode.pt).getMonetaryDistanceRate();

		constPt			= planCalcScoreConfigGroup.getModes().get(TransportMode.pt).getConstant();
	}


	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {
		if ( ptMatrix==null ) {
            throw new RuntimeException( "pt accessibility does only work when a PtMatrix is provided.  Provide such a matrix, or switch off "
                    + "the pt accessibility computation, or extend the Java code so that it works for this situation.") ;
        }
		final Node destinationNode = destination.getNearestNode();
		double ptTravelTime_h = 0 ;
		double ptTotalWalkTime_h = 0 ;
		double ptTravelDistance_meter = 0 ;
		double ptTotalWalkDistance_meter = 0;
		if ( ptMatrix != null ) {
			// travel time with pt:
			ptTravelTime_h	 = ptMatrix.getPtTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
			// total walking time including (i) to get to pt stop and (ii) to get from destination pt stop to destination location:
			ptTotalWalkTime_h =ptMatrix.getTotalWalkTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
			// total travel distance including walking and pt distance from/to origin/destination location:
			ptTravelDistance_meter=ptMatrix.getTotalWalkTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());
			// total walk distance  including (i) to get to pt stop and (ii) to get from destination pt stop to destination location:
			ptTotalWalkDistance_meter=ptMatrix.getPtTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());
		} else if ( routingModule != null ) {
			 List<? extends PlanElement> result = routingModule.calcRoute( null, null, 0., null ) ;
			 Leg accessWalkLeg = (Leg) result.get(0) ;
			 ptTotalWalkTime_h += accessWalkLeg.getRoute().getTravelTime() / 3600 ;
			 throw new RuntimeException("this execution path is not yet finished") ;
		}

		double ptDisutility = constPt + (ptTotalWalkTime_h * betaWalkTT) + (ptTravelTime_h * betaPtTT) + (ptTotalWalkDistance_meter * betaWalkTD) + (ptTravelDistance_meter * betaPtTD);
		return destination.getSum() * Math.exp(this.logitScaleParameter * ptDisutility);
	}
}
