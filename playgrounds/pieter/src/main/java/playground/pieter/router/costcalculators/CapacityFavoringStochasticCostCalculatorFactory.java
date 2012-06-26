package playground.pieter.router.costcalculators;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;

public class CapacityFavoringStochasticCostCalculatorFactory implements
		TravelDisutilityFactory {

	@Override
	public TravelDisutility createTravelDisutility(
			PersonalizableTravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		// TODO Auto-generated method stub
		return new CapacityFavoringStochasticCostCalculator(timeCalculator, cnScoringGroup);
	}

}
