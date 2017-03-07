package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVehicleRequestMatcher;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class HungarianDispatcher extends UniversalDispatcher {
    private final int DISPATCH_PERIOD;

    final Network network; // <- for verifying link references
    final Collection<Link> linkReferences; // <- for verifying link references

    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    final DrivebyRequestStopper drivebyRequestStopper;

    private HungarianDispatcher( //
                                 AVDispatcherConfig avDispatcherConfig, //
                                 TravelTime travelTime, //
                                 ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
                                 EventsManager eventsManager, //
                                 Network network) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        this.network = network;
        linkReferences = new HashSet<>(network.getLinks().values());
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        DISPATCH_PERIOD = Integer.parseInt(avDispatcherConfig.getParams().get("dispatchPeriod"));
        drivebyRequestStopper = new DrivebyRequestStopper(this::setVehicleDiversion);
    }

    int total_matchedRequests = 0;

    @Override
    public void redispatch(double now) {
        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());

        final long round_now = Math.round(now);
        if (round_now % DISPATCH_PERIOD == 0) {


            int num_abortTrip = 0;
            int num_driveOrder = 0;
            
            // see if any car is driving by a request. if so, then stay there to be matched!
            num_abortTrip += drivebyRequestStopper.realize(getAVRequestsAtLinks(), getDivertableVehicles());


            { // for all remaining vehicles and requests, perform a bipartite matching

                Map<Link, List<AVRequest>> requests = getAVRequestsAtLinks();
                // call getDivertableVehicles again to get remaining vehicles
                Collection<VehicleLinkPair> divertableVehicles = getDivertableVehicles();

                // Save request in list which is needed for abstractVehicleDestMatcher
                AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
                List<Link> requestlocs =
                        requests.values()
                                .stream()
                                .flatMap(List::stream)
                                .map(AVRequest::getFromLink)
                                .collect(Collectors.toList());

                // find the Euclidean bipartite matching for all vehicles using the Hungarian method
                System.out.println("optimizing over "+divertableVehicles.size()+" vehicles and "+requestlocs.size() + " requests.");
                Map<VehicleLinkPair, Link> hungarianmatches = abstractVehicleDestMatcher.match(divertableVehicles, requestlocs);

                // use the result to setVehicleDiversion
                hungarianmatches.entrySet().forEach(this::setVehicleDiversion);
                // TODO remove commented code below, since the line above does the job
//                for (VehicleLinkPair vehicleLinkPair : hungarianmatches.keySet()) {
//                    setVehicleDiversion(vehicleLinkPair, hungarianmatches.get(vehicleLinkPair));
//                }
            }
        }
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
            return new HungarianDispatcher( //
                    config, travelTime, router, eventsManager, network);
        }
    }
}
