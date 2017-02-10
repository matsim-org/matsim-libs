package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LazyDispatcher extends UniversalDispatcher {
    public static final String IDENTIFIER = LazyDispatcher.class.getSimpleName();

    private Link[] destLinks = null;

    public LazyDispatcher(EventsManager eventsManager, SingleRideAppender appender, Link[] sendAVtoLink) {
        super(eventsManager, appender);
        this.destLinks = sendAVtoLink;
    }

    @Override
    public void redispatch(double now) {

        if (Math.round(now) % 30 == 0) {
            // System.out.println("============ TIME " + now);

            Set<Link> unmatched = new HashSet<>();
            {
                Map<Link, Queue<AVVehicle>> map = getStayVehicles();
                Collection<AVRequest> collection = getAVRequests();
                if (!map.isEmpty() && !collection.isEmpty()) {
                    // System.out.println(now + " @ " + map.size() + " <-> " + collection.size());
                    for (AVRequest avRequest : collection) {
                        Link link = avRequest.getFromLink();
                        if (map.containsKey(link)) {
                            Queue<AVVehicle> queue = map.get(link);
                            if (queue.isEmpty()) {
                                unmatched.add(link);
                            } else {
                                AVVehicle avVehicle = queue.poll();
                                setAcceptRequest(avVehicle, avRequest);
                            }
                        }
                    }
                }
            }

            if (!unmatched.isEmpty()) {
                Collection<VehicleLinkPair> collection = getDivertableVehicles();

                for (VehicleLinkPair vehicleLinkPair : collection) {
                    if (unmatched.isEmpty())
                        break;
                    Link dest = vehicleLinkPair.getDestination();
                    if (dest == null) { // stay task
                        Link link = unmatched.iterator().next();
                        setVehicleDiversion(vehicleLinkPair, link);
                        unmatched.remove(link);
                    }
                }
            }
        }
        // System.out.println("" + now + " " + getAVRequests().size());
        /*
         * while (requestIterator.hasNext()) {
         * AVRequest request = requestIterator.next();
         * Link custLocation = request.getFromLink();
         * Iterator<AVVehicle> vehicleIterator = availableVehicles.iterator();
         * while (vehicleIterator.hasNext()) {
         * AVVehicle vehicle = vehicleIterator.next();
         * Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
         * AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
         * Link avLocation = stayTask.getLink();
         * if (avLocation.equals(custLocation)) {
         * requestIterator.remove();
         * vehicleIterator.remove();
         * appender.schedule(request, vehicle, now);
         * //System.out.println("matched AV and customer at link " + avLocation.getId().toString());
         * break;
         * }
         * }
         * }
         */
        // int min = (int) Math.round(now/60);
        // if (2 * 60 < now) {
        // if (now < 5 * 60) {
        // for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
        // setVehicleDiversion(vehicleLinkPair, destLinks[0]);
        // }
        // } else //
        // if (now < 20 * 60) {
        // for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
        // setVehicleDiversion(vehicleLinkPair, destLinks[3]);
        // }
        // } else //
        // if (now < 65 * 60) {
        // for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
        // setVehicleDiversion(vehicleLinkPair, destLinks[1]);
        // }
        // } else //
        // // if (now < 25 * 60) {
        // // for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
        // // setVehicleDiversion(vehicleLinkPair, destLinks[2]);
        // // }
        // // } else //
        // if (now < 3100) {
        // // for (VehicleLinkPair vehicleLinkPair : getStayableVehicles()) {
        // // setVehicleStay(vehicleLinkPair);
        // // }
        // }
        //
        // }
    }

    static public class Factory implements AVDispatcherFactory {
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            // load some random link from the network
            Id<Link> l1 = Id.createLinkId("238277009_1_r");
            Id<Link> l2 = Id.createLinkId("236193238_1_r");
            Id<Link> l3 = Id.createLinkId("9904005_1_rL2");
            Id<Link> l4 = Id.createLinkId("237049585_0L3");
            Link sendAVtoLink1 = network.getLinks().get(l1);
            Link sendAVtoLink2 = network.getLinks().get(l2);
            Link sendAVtoLink3 = network.getLinks().get(l3);
            Link sendAVtoLink4 = network.getLinks().get(l4);
            Link[] sendAVtoLinks = new Link[] { sendAVtoLink1, sendAVtoLink2, sendAVtoLink3, sendAVtoLink4 };
            // put the link into the lazy dispatcher
            return new LazyDispatcher(eventsManager, new SingleRideAppender(config, router, travelTime), sendAVtoLinks);
        }
    }
}
