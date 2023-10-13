package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author thibautd, michaz
 */
final class LeastCostPathCalculatorAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private final LeastCostPathCalculator leastCostPathCalculator;
	private final ScoringConfigGroup scoringConfigGroup;
	private Node fromNode;
	private Double departureTime;
	private Scenario scenario;

    Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;


	public LeastCostPathCalculatorAccessibilityContributionCalculator(ScoringConfigGroup scoringConfigGroup, LeastCostPathCalculator leastCostPathCalculator, Scenario scenario) {
		this.scoringConfigGroup = scoringConfigGroup;
		this.leastCostPathCalculator = leastCostPathCalculator;
		this.scenario = scenario;
	}


    @Override
    public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
        this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, scenario.getNetwork());
        this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, scenario.getNetwork(), scenario.getConfig());
    }


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		this.fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
		this.departureTime = departureTime;
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
			Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		double expSum = 0.;

		for (final AggregationObject destination : aggregatedOpportunities.values()) {
			LeastCostPathCalculator.Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, (Node) destination.getNearestBasicLocation(), departureTime, null, null);
			expSum += destination.getSum() * Math.exp(scoringConfigGroup.getBrainExpBeta() * path.travelCost);
		}
		return expSum;
	}


	@Override
	public LeastCostPathCalculatorAccessibilityContributionCalculator duplicate() {
		LeastCostPathCalculatorAccessibilityContributionCalculator leastCostPathCalculatorAccessibilityContributionCalculator =
				new LeastCostPathCalculatorAccessibilityContributionCalculator(this.scoringConfigGroup, this.leastCostPathCalculator, this.scenario);
		return leastCostPathCalculatorAccessibilityContributionCalculator;
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
