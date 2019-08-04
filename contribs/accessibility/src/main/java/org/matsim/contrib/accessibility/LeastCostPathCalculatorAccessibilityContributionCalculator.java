package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 * @author michaz
 */
public final class LeastCostPathCalculatorAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private final LeastCostPathCalculator leastCostPathCalculator;
	private final PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private Node fromNode;
	private Double departureTime;

	public LeastCostPathCalculatorAccessibilityContributionCalculator(PlanCalcScoreConfigGroup planCalcScoreConfigGroup, LeastCostPathCalculator leastCostPathCalculator) {
		this.planCalcScoreConfigGroup = planCalcScoreConfigGroup;
		this.leastCostPathCalculator = leastCostPathCalculator;
	}

	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
		this.departureTime = departureTime;
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {
		LeastCostPathCalculator.Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, destination.getNearestNode(), departureTime, null, null);
		return destination.getSum() * Math.exp(planCalcScoreConfigGroup.getBrainExpBeta() * path.travelCost);
	}


	@Override
	public LeastCostPathCalculatorAccessibilityContributionCalculator duplicate() {
		LeastCostPathCalculatorAccessibilityContributionCalculator leastCostPathCalculatorAccessibilityContributionCalculator =
				new LeastCostPathCalculatorAccessibilityContributionCalculator(this.planCalcScoreConfigGroup, this.leastCostPathCalculator);
		return leastCostPathCalculatorAccessibilityContributionCalculator;
	}
}
