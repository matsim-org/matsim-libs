package playground.pieter.distributed.randomizedcarrouter;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by fouriep on 2/8/15.
 */
public class RandomizedCarRouterTravelTimeAndDisutilityModule extends AbstractModule {
    @Override
    public void install() {
        bindTo(TravelDisutilityFactory.class,RandomizedCarRouterTravelTimeAndDisutilityFactory.class);
        bindToProvider(TravelDisutility.class, TravelDisutilityProvider.class);
    }

    private static class TravelDisutilityProvider implements Provider<TravelDisutility> {

        final TravelDisutilityFactory travelDisutilityFactory;
        final Config config;
        final TravelTime travelTime;

        @Inject
        TravelDisutilityProvider(TravelDisutilityFactory travelDisutilityFactory, Config config, TravelTime travelTime) {
            this.travelDisutilityFactory = travelDisutilityFactory;
            this.config = config;
            this.travelTime = travelTime;
        }

        @Override
        public TravelDisutility get() {
            return travelDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
        }

    }
}
