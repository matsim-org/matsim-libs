package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Dispatcher sends vehicles to all links in the network and lets them pickup any customers which
 * are waiting along the road.
 * 
 * @author Claudio Ruch */
public class DriveByDispatcher extends RebalancingDispatcher {
    private final List<Link> links;
    int index = 0;
    double rebPos = 0.1;
    Random randGen = new Random(1234);
    private final int dispatchPeriod;
    private int total_abortTrip = 0;

    private DriveByDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, travelTime, router, eventsManager);
        links = new ArrayList<>(network.getLinks().values());
        Collections.shuffle(links);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = getDispatchPeriod(safeConfig, 120); // safeConfig.getInteger("dispatchPeriod",
                                                             // 120);
    }

    @Override
    public void redispatch(double now) {

        final long round_now = Math.round(now);
        if (round_now == 30000) {

            // select a roboTaxi
            Collection<RoboTaxi> taxis = getDivertableRoboTaxis();
            RoboTaxi rt1 = null;
            Iterator<RoboTaxi> iter = taxis.iterator();
            if (iter.hasNext()) {
                rt1 = iter.next();

            }

            // select an open request
            Collection<AVRequest> openRequests = getAVRequests();
            Optional<AVRequest> avR = openRequests.stream().findAny();

            // pickup the request
            if (avR.isPresent() && rt1 != null) {
                setRoboTaxiPickup(rt1, avR.get());
            }
        }

        // DrivebyRequestStopper.stopDrivingByExperi(getAVRequestsAtLinks(),
        // getDivertableRoboTaxis(), this::setRoboTaxiPickup);

        // stop all vehicles which are driving by an open request
        // total_abortTrip += DrivebyRequestStopper.stopDrivingBy(getAVRequestsAtLinks(),
        // getDivertableVehicleLinkPairs(), this::setVehiclePickup);

        // total_abortTrip += DrivebyRequestStopper.stopDrivingByNew(getAVRequestsAtLinks(),
        // getDivertableRoboTaxis(), this::setRoboTaxiPickup);

        // final long round_now = Math.round(now);
        // if (round_now % dispatchPeriod == 0 && 0 < getAVRequests().size()) {
        // // send vehicles to travel around the city
        // for (AVVehicle avVehicle : getDivertableVehicles())
        // if (rebPos > randGen.nextDouble()) {
        // setVehicleRebalance(avVehicle, pollNextDestination());
        // }
        // }

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
            return new DriveByDispatcher(config, travelTime, router, eventsManager, network);
        }
    }

}
