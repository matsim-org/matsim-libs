package playground.tnicolai.matsim4opus.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelMinDisutility;
import org.matsim.core.router.util.TravelTime;

public class TravelTimeCostCalculatorTest implements TravelMinDisutility {

	protected final TravelTime timeCalculator;
	private final double marginalCostOfTime;

	public TravelTimeCostCalculatorTest(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double dummyCostFactor) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.marginalCostOfTime = dummyCostFactor;
		// normally use cost of:
		// (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);	
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		
		return this.marginalCostOfTime * travelTime;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
	}
} 
