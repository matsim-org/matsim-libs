package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVehicleRequestMatcher;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.ImmobilizeVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.dispatcher.utils.RequestStopsDrivebyVehicles;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class HungarianDispatcher extends UniversalDispatcher {
    
    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    final RequestStopsDrivebyVehicles drivebyRequestStopper;
    final AbstractVehicleDestMatcher abstractVehicleDestMatcher;
    final AbstractRequestSelector requestSelector;
    
    private final int dispatchPeriod;
    @Deprecated
    private final int maxMatchNumber;

    private HungarianDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        drivebyRequestStopper = new RequestStopsDrivebyVehicles(); // FIXME
        abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
        this.requestSelector = abstractRequestSelector;

        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
        maxMatchNumber = safeConfig.getInteger("maxMatchNumber", Integer.MAX_VALUE);
    }

    int total_matchedRequests = 0;

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);
        
        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());
        
        if (round_now % dispatchPeriod == 0) {
            { // assign new destination to vehicles with bipartite matching

                Map<Link, List<AVRequest>> requests = getAVRequestsAtLinks();

                // call getDivertableVehicles again to get remaining vehicles
                Collection<VehicleLinkPair> divertableVehicles = getDivertableVehicles();

                // reduce the number of requests for a smaller running time
                // take out and match request-vehilce pairs with distance zero
                List<Link> requestlocs = requests.values().stream().flatMap(v -> v.stream()).map(AVRequest::getFromLink) // all from links of all requests with
                                                                                                                         // multiplicity
                        .collect(Collectors.toList());
                ImmobilizeVehicleDestMatcher immobilizeVehicleDestMatcher = new ImmobilizeVehicleDestMatcher();
                Map<VehicleLinkPair, Link> sameLocMatch = immobilizeVehicleDestMatcher.match(divertableVehicles, requestlocs);
                sameLocMatch.entrySet().forEach(this::setVehicleDiversion);

                /*
                 * // only select MAXMATCHNUMBER of oldest requests
                 * Collection<AVRequest> avRequests = requests.values().stream().flatMap(v -> v.stream()).collect(Collectors.toList());
                 * Collection<AVRequest> avRequestsReduced = requestSelector.selectRequests(divertableVehicles, avRequests, MAXMATCHNUMBER);
                 */

                // Save request in list which is needed for abstractVehicleDestMatcher
                /*
                 * List<Link> requestlocs = avRequestsReduced
                 * .stream()
                 * .map(AVRequest::getFromLink) // all from links of all requests with multiplicity
                 * .collect(Collectors.toList());
                 */

                // call getDivertableVehicles again to get remaining vehicles
                divertableVehicles = getDivertableVehicles();

                // find the Euclidean bipartite matching for all vehicles using the Hungarian method
                System.out.println("optimizing over " + divertableVehicles.size() + " vehicles and " + requestlocs.size() + " requests.");
                Map<VehicleLinkPair, Link> hungarianmatches = abstractVehicleDestMatcher.match(divertableVehicles, requestlocs);

                // use the result to setVehicleDiversion
                hungarianmatches.entrySet().forEach(this::setVehicleDiversion);
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
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            return new HungarianDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
