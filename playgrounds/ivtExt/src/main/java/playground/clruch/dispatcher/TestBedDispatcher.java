
package playground.clruch.dispatcher;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Empty Test Dispatcher, rebalances a vehicle every 30 mins and
 * performs a pickup every 30 mins if open reqeutss are present.
 * Not functional, use as startpoint to build your own dispatcher.
 * 
 * @author Claudio Ruch */
public class TestBedDispatcher extends RebalancingDispatcher {
    private final int rebalancingPeriod;
    private final Network network;

    private TestBedDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, travelTime, router, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 120);
        this.network = network;
    }

    @Override
    public void redispatch(double now) {

        // TestBedDispatcher implemenatation
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0 && 0 < getAVRequests().size()) {

            // rebalance a RoboTaxi
            RoboTaxi robotaxi = getDivertableRoboTaxis().iterator().next();
            Link rebalanceTo = network.getLinks().values().iterator().next();
            if (round_now % 1800 == 0) {
                setRoboTaxiRebalance(robotaxi, rebalanceTo);
            }

            // generate a pickup
            Collection<AVRequest> avRequests = getAVRequests();
            RoboTaxi robotaxiPickup = getDivertableRoboTaxis().iterator().next();
            if (!avRequests.isEmpty() && round_now % 1800 == 0) {
                setRoboTaxiPickup(robotaxiPickup, getAVRequests().iterator().next());
            }
        }
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s AT=%5d", //
                super.getInfoLine());
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        private Network network;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new TestBedDispatcher(config, travelTime, router, eventsManager, network);
        }
    }

}
