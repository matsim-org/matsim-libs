package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.MatchRequestsWithStayVehicles;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class EdgyDispatcher extends UniversalDispatcher {
    public static final int DEBUG_PERIOD = 30;

    private EdgyDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);
        if (round_now % DEBUG_PERIOD == 0 && now < 100000) {

            int num_matchedRequests = 0;
            int num_abortTrip = 0;
            int num_driveOrder = 0;
            
            num_matchedRequests = MatchRequestsWithStayVehicles.inOrderOfArrival(this);

            { // see if any car is driving by a request. if so, then stay there to be matched!
                Map<Link, List<AVRequest>> requests = getAVRequestsAtLinks(); // TODO lazy implementation
                Collection<VehicleLinkPair> divertableVehicles = getDivertableVehicles();

                for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                    Link link = vehicleLinkPair.getDivertableLocation(); // TODO check if this should apply only to driving vehicles
                    if (requests.containsKey(link)) {
                        List<AVRequest> requestList = requests.get(link);
                        if (!requestList.isEmpty()) {
                            requestList.remove(0);
                            setVehicleDiversion(vehicleLinkPair, link);
                            ++num_abortTrip;
                        }
                    }
                }
            }

            {
                Iterator<AVRequest> requestIterator = getAVRequests().iterator();
                Collection<VehicleLinkPair> divertableVehicles = getDivertableVehicles();

                for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                    Link dest = vehicleLinkPair.getDestination();
                    if (dest == null) { // vehicle in stay task
                        if (requestIterator.hasNext()) {
                            Link link = requestIterator.next().getFromLink();
                            setVehicleDiversion(vehicleLinkPair, link);
                            ++num_driveOrder;
                        } else
                            break;
                    }
                }
            }

            System.out.println(String.format("@%6d   mr=%4d     at=%4d     do=%4d", //
                    round_now, //
                    num_matchedRequests, //
                    num_abortTrip, //
                    num_driveOrder));
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            return new EdgyDispatcher( //
                    config, travelTime, router, eventsManager);
        }
    }
}
