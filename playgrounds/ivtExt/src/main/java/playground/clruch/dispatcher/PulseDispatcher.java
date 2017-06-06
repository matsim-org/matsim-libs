package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

@Deprecated // it is not clear what this dispatcher does and why it exists
public class PulseDispatcher extends UniversalDispatcher {
    private final List<Link> links;
    int index = 0;
    private final int dispatchPeriod;
    private int total_abortTrip = 0;

    private PulseDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, travelTime, router, eventsManager);
        links = new ArrayList<>(network.getLinks().values());
        Collections.shuffle(links);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 120);
    }

    @Override
    public void redispatch(double now) {

        final long round_now = Math.round(now);

        new InOrderOfArrivalMatcher(this::setAcceptRequest).match(getStayVehicles(), getAVRequestsAtLinks());

        total_abortTrip += new DrivebyRequestStopper(this::setVehicleDiversion) //
                .realize(getAVRequestsAtLinks(), getDivertableVehicles());

        if (round_now % dispatchPeriod == 0 && 0 < getAVRequests().size()) {

            for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles())
                setVehicleDiversion(vehicleLinkPair, pollNextDestination());

        }
    }

    private Link pollNextDestination() {
        Link link = links.get(index);
        ++index;
        index %= links.size();
        return link;
    }

    @Override
    public String getInfoLine() {
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
            return new PulseDispatcher(config, travelTime, router, eventsManager, network);
        }
    }

}
