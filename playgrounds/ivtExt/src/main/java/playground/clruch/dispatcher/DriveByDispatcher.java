package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.DispatcherUtils;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Dispatcher sends vehicles to all links in the network and lets them pickup any customers which
 * are waiting along the road.
 * 
 * @author Claudio Ruch */
public class DriveByDispatcher extends RebalancingDispatcher {
    private final List<Link> links;
    private final double rebPos = 0.99;
    private final Random randGen = new Random(1234);
    private final int rebalancingPeriod;
    private int total_abortTrip = 0;

    private DriveByDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, travelTime, router, eventsManager);
        links = new ArrayList<>(network.getLinks().values());
        Collections.shuffle(links, randGen);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 120);
    }

    @Override
    public void redispatch(double now) {

        // stop all vehicles which are driving by an open request
        total_abortTrip += DrivebyRequestStopper //
                .stopDrivingBy(DispatcherUtils.getAVRequestsAtLinks(getAVRequests()), getDivertableRoboTaxis(), this::setRoboTaxiPickup);

        // send vehicles to travel around the city to random links (random loitering)
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0 && 0 < getAVRequests().size()) {
            for (RoboTaxi roboTaxi : getDivertableRoboTaxis()) {
                if (rebPos > randGen.nextDouble()) {
                    setRoboTaxiRebalance(roboTaxi, pollNextDestination());
                }
            }
        }
    }

    private Link pollNextDestination() {
        int index = randGen.nextInt(links.size());
        Link link = links.get(index);
        return link;
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s AT=%5d", //
                super.getInfoLine(), //
                total_abortTrip //
        );
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
            return new DriveByDispatcher(config, travelTime, router, eventsManager, network);
        }
    }

}