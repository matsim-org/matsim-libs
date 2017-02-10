package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.utils.ScheduleUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LazyDispatcher extends UniversalDispatcher {
    public static final String IDENTIFIER = LazyDispatcher.class.getSimpleName();
    public static final int DEBUG_PERIOD = 5 * 60;
    public static final String DEBUG_AVVEHICLE = "av_av_op1_1";

    public LazyDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    @Override
    public void redispatch(double now) {

        if (Math.round(now) % DEBUG_PERIOD == 0) {
            System.out.println("==================== TIME " + now);
            System.out.println(getStatusString());

            // BEGIN: debug info
            for (AVVehicle avVehicle : getFunctioningVehicles())
                if (avVehicle.getId().toString().equals(DEBUG_AVVEHICLE))
                    System.out.println(ScheduleUtils.scheduleOf(avVehicle));
            // END: debug info

            final Queue<Link> unmatchedRequestLinks = new LinkedList<>();
            {
                Map<Link, Queue<AVVehicle>> stayVehicles = getStayVehicles();
                Collection<AVRequest> avRequests = getAVRequests();
                for (AVRequest avRequest : avRequests) {
                    Link link = avRequest.getFromLink();
                    if (stayVehicles.containsKey(link)) {
                        Queue<AVVehicle> queue = stayVehicles.get(link);
                        if (queue.isEmpty()) {
                            unmatchedRequestLinks.add(link);
                        } else {
                            AVVehicle avVehicle = queue.poll();
                            setAcceptRequest(avVehicle, avRequest);
                        }
                    } else {
                        unmatchedRequestLinks.add(link);
                    }
                }
            }
            System.out.println("#unmatchedRequestLinks " + unmatchedRequestLinks.size());
            System.out.println(getStatusString());

            if (!unmatchedRequestLinks.isEmpty())
                for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                    // System.out.println("TESTING [" + vehicleLinkPair.avVehicle.getId() + "]");
                    if (unmatchedRequestLinks.isEmpty())
                        break;
                    Link dest = vehicleLinkPair.getDestination();
                    if (dest == null) { // vehicle in stay task
                        Link link = unmatchedRequestLinks.poll();
                        setVehicleDiversion(vehicleLinkPair, link);
                    }
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            return new LazyDispatcher( //
                    config, travelTime, router, eventsManager);
        }
    }
}
