package playground.pieter.distributed.plans.router;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouterFactory;

/**
 * Created by fouriep on 1/30/15.
 */
public class DefaultTripRouterFactoryForPlanGenomesModule extends AbstractModule {
    @Override
    public void install() {
        bind(TripRouterFactory.class).to(DefaultTripRouterFactoryForPlanGenomes.class);
    }
}
