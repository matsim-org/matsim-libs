package playground.sebhoerl.renault;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.ivt.matsim2030.router.TransitRouterNetworkReader;

import javax.inject.Inject;

@Singleton
public class IleDeFranceTransitRouterProvider implements Provider<TransitRouter> {
    private final TransitRouterConfig config;
    private final TransitRouterNetwork routerNetwork;
    private final PreparedTransitSchedule preparedTransitSchedule;

    @Inject
    IleDeFranceTransitRouterProvider(final TransitSchedule schedule, final Config config) {
        this(schedule, new TransitRouterConfig(
                config.planCalcScore(),
                config.plansCalcRoute(),
                config.transitRouter(),
                config.vspExperimental()));
    }

    public IleDeFranceTransitRouterProvider(final TransitSchedule schedule, final TransitRouterConfig config) {
        this.config = config;

        this.routerNetwork = new TransitRouterNetwork();
        new TransitRouterNetworkReader(schedule, routerNetwork).readFile("transit_network.xml.gz");

        this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
    }

    @Override
    public TransitRouter get() {
        TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.config, this.preparedTransitSchedule);
        return new TransitRouterImpl(this.config, this.preparedTransitSchedule, this.routerNetwork, ttCalculator, ttCalculator);
    }
}
