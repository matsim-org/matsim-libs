package playground.pieter.distributed.randomizedcarrouter;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * Created by fouriep on 2/8/15.
 */
public class RandomizedCarRouterTravelTimeAndDisutilityFactory implements TravelDisutilityFactory {
    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
        RandomizedCarRouterTravelTimeAndDisutility disutility = new RandomizedCarRouterTravelTimeAndDisutility(timeCalculator, cnScoringGroup);
        disutility.setDataCollection(RandomizedCarRouterTravelTimeAndDisutility.DataCollection.randomizedParameters,true);
        disutility.setDataCollection(RandomizedCarRouterTravelTimeAndDisutility.DataCollection.additionalInformation,false);
        return disutility;
    }
}
