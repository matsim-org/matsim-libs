package playground.pieter.router.costcalculators;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

class CapacityFavoringStochasticCostCalculatorFactory implements TravelDisutilityFactory {

	@Inject PlanCalcScoreConfigGroup cnScoringGroup;
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new CapacityFavoringStochasticCostCalculator(timeCalculator, cnScoringGroup);
	}

}
