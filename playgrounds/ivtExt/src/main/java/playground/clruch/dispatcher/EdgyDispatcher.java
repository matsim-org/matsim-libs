package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleRequestMatcher;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.DrivebyRequestStopperNew;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.utils.ScheduleUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class EdgyDispatcher extends UniversalDispatcher {
    /**
     * a large value of DISPATCH_PERIOD helps to quickly test the EdgyDispatcher
     */
    private final int DISPATCH_PERIOD;
    private static final boolean DEBUG_SHOWSCHEDULE = false;
    public static final String DEBUG_AVVEHICLE = "av_av_op1_1";

    final Network network; // <- for verifying link references
    final Collection<Link> linkReferences; // <- for verifying link references

    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    final DrivebyRequestStopperNew drivebyRequestStopperNew;

    private EdgyDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        this.network = network;
        linkReferences = new HashSet<>(network.getLinks().values());
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        drivebyRequestStopperNew = new DrivebyRequestStopperNew();
        DISPATCH_PERIOD = Integer.parseInt(avDispatcherConfig.getParams().get("dispatchPeriod"));
    }

    /** verify that link references are present in the network */
    @SuppressWarnings("unused") // for verifying link references
    private void verifyLinkReferencesInvariant() {
        List<Link> testset = getDivertableVehicles().stream() //
                .map(VehicleLinkPair::getCurrentDriveDestination) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());
        if (!linkReferences.containsAll(testset))
            throw new RuntimeException("network change 1");
        if (!linkReferences.containsAll(network.getLinks().values()))
            throw new RuntimeException("network change 2");
        if (0 < testset.size())
            System.out.println("network " + linkReferences.size() + " contains all " + testset.size());
    }

    int total_matchedRequests = 0;
    int total_abortTrip = 0;
    int total_driveOrder = 0;

    long toc_lastPrint = System.nanoTime();

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);
        // verifyReferences(); // <- debugging only
        final long tic = System.nanoTime();
        // if (1e9 < tic - toc_lastPrint)
        {
            toc_lastPrint = tic;
            System.out.println(String.format("%s BEG @%6d   MR=%6d   AT=%6d   do=%6d   um=%6d == %6d", //
                    getClass().getSimpleName().substring(0, 8), //
                    round_now, //
                    total_matchedRequests, //
                    total_abortTrip, //
                    total_driveOrder, //
                    getAVRequestsAtLinks().values().stream().flatMap(s->s.stream()).count(), //
                    getAVRequestsAtLinks().values().stream().mapToInt(List::size).sum() //
                    ));
        }

        if (DEBUG_SHOWSCHEDULE) {
            System.out.println(" --- schedule before matching --- ");
            for (AVVehicle avVehicle : getFunctioningVehicles())
                if (avVehicle.getId().toString().equals(DEBUG_AVVEHICLE))
                    System.out.println(ScheduleUtils.scheduleOf(avVehicle));
        }

        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());

        // System.out.println(" --- schedule before stopping --- ");
        {

            // TODO remove this later
            // OLD IMPLEMENTATION
            /*
            Collection<VehicleLinkPair> list = //
                    drivebyRequestStopper.realize(getAVRequestsAtLinks(), getDivertableVehicles());
            */
            // OLD IMPLEMENTATION END


            // NEW IMPLEMENTATION
            Collection<VehicleLinkPair> list = drivebyRequestStopperNew.match(getAVRequestsAtLinks(),getDivertableVehicles());
            list.stream().forEach(v->setVehicleDiversion(v,v.getDivertableLocation()));
            // NEW IMPLEMENTATION END





            list.stream() //
                    .map(vlp -> "STOP " + vlp.avVehicle.getOperator().getId().toString()) //
                    .forEach(System.out::println);
            total_abortTrip += list.size();
        }

        if (DEBUG_SHOWSCHEDULE) {
            System.out.println(" --- schedule after stopping --- ");
            for (AVVehicle avVehicle : getFunctioningVehicles())
                if (avVehicle.getId().toString().equals(DEBUG_AVVEHICLE))
                    System.out.println(ScheduleUtils.scheduleOf(avVehicle));
        }

        if (round_now % DISPATCH_PERIOD == 0) {

            { // TODO this should be replaceable by some naive matcher
                Iterator<AVRequest> requestIterator = getAVRequests().iterator();
                for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                    Link dest = vehicleLinkPair.getCurrentDriveDestination();
                    if (dest == null) { // vehicle in stay task
                        if (requestIterator.hasNext()) {
                            Link link = requestIterator.next().getFromLink();
                            setVehicleDiversion(vehicleLinkPair, link);
                            ++total_driveOrder;
                        } else
                            break;
                    }
                }
            }

        }

        {
            toc_lastPrint = tic;
            System.out.println(String.format("%s END @%6d   MR=%6d   AT=%6d   do=%6d   um=%6d == %6d", //
                    getClass().getSimpleName().substring(0, 8), //
                    round_now, //
                    total_matchedRequests, //
                    total_abortTrip, //
                    total_driveOrder, //
                    getAVRequestsAtLinks().values().stream().flatMap(s->s.stream()).count(), //
                    getAVRequestsAtLinks().values().stream().mapToInt(List::size).sum() //
            ));
        }

//        if (5 < total_abortTrip)
//            System.exit(0);

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
            return new EdgyDispatcher( //
                    config, travelTime, router, eventsManager, network);
        }
    }
}
