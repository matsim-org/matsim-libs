package playground.smetzler.bike;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;


public class BikeTravelDisutilityFactory implements TravelDisutilityFactory {

		
	@Inject
	BikeConfigGroup bikeConfigGroup;
	@Inject
	PlanCalcScoreConfigGroup cnScoringGroup;
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new BikeTravelDisutility(bikeConfigGroup, cnScoringGroup);
	}

}
