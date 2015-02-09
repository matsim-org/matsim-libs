package playground.pieter.distributed.randomizedcarrouter;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

/**
 * Created by fouriep on 2/8/15.
 */
public class RandomizedCarRouterTravelTimeAndDisutilityModule extends AbstractModule {
    @Override
    public void install() {
        bindTo(TravelDisutilityFactory.class,RandomizedCarRouterTravelTimeAndDisutilityFactory.class);
    }
}
