package playground.tnicolai.matsim4opus.testCostCalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

public class TravelDistanceCostCalculatorTest implements TravelMinCost {

	protected final TravelTime timeCalculator;
	private final double marginalCostOfDistance;

	public TravelDistanceCostCalculatorTest(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double dummyCostFactor) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.marginalCostOfDistance = dummyCostFactor;
		// normally use cost of:
		// - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney();
	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		
		return this.marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelCost(final Link link) {
		return this.marginalCostOfDistance * link.getLength();
	}
} 
