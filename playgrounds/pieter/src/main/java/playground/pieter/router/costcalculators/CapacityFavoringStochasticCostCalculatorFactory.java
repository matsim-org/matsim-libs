package playground.pieter.router.costcalculators;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

class CapacityFavoringStochasticCostCalculatorFactory implements
		TravelDisutilityFactory {

	@Override
	public TravelDisutility createTravelDisutility(
			TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		return new CapacityFavoringStochasticCostCalculator(timeCalculator, cnScoringGroup);
	}

}
