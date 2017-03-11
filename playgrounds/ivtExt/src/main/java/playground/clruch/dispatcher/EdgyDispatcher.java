package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import playground.clruch.dispatcher.utils.AbstractVehicleRequestLinkMatcher;
import playground.clruch.dispatcher.utils.AbstractVehicleRequestMatcher;
import playground.clruch.dispatcher.utils.AnyReqestDivertsVehicles;
import playground.clruch.dispatcher.utils.DivertIfCurrentDestinationEmpty;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.RequestStopsDrivebyVehicles;
import playground.clruch.dispatcher.utils.ReserveVehiclesStoppingOnLink;
import playground.clruch.utils.ScheduleUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class EdgyDispatcher extends UniversalDispatcher {
    private static final boolean DEBUG_SHOWSCHEDULE = false;
    public static final String DEBUG_AVVEHICLE = "av_av_op1_1";

    final Network network; // <- for verifying link references
    final Collection<Link> linkReferences; // <- for verifying link references

    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    final AbstractVehicleRequestLinkMatcher reserveVehiclesStoppingOnLink;
    final AbstractVehicleRequestLinkMatcher requestStopsDrivebyVehicles;
    final AbstractVehicleRequestLinkMatcher divertIfCurrentDestinationEmpty;

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
        reserveVehiclesStoppingOnLink = new ReserveVehiclesStoppingOnLink();
        requestStopsDrivebyVehicles = new RequestStopsDrivebyVehicles();
        divertIfCurrentDestinationEmpty = new DivertIfCurrentDestinationEmpty();
        // dispatchPeriod = Integer.parseInt(avDispatcherConfig.getParams().get("dispatchPeriod"));
    }

    /** verify that link references are present in the network */
    @SuppressWarnings("unused") // for verifying link references
    private void _verifyLinkReferencesInvariant() {
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

    private void _printSchedule(String string) {
        if (DEBUG_SHOWSCHEDULE) {
            System.out.println(string);
            for (AVVehicle avVehicle : getFunctioningVehicles())
                if (avVehicle.getId().toString().equals(DEBUG_AVVEHICLE))
                    System.out.println(ScheduleUtils.scheduleOf(avVehicle));
        }
    }

    int total_reserveVehicles = 0;
    int total_abortTrip = 0;
    int total_driveOrder = 0;
    int total_emptyDrive = 0;

    @Override
    public void redispatch(double now) {

        _printSchedule("before matching");

        vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());

        final Map<Link, List<AVRequest>> requests = getAVRequestsAtLinks();
        {
            Map<VehicleLinkPair, Link> pass1 = new AnyReqestDivertsVehicles().match(requests, getDivertableVehicles());
            pass1.entrySet().stream().forEach(this::setVehicleDiversion); // prevents vehicles from being redirected
            total_reserveVehicles += pass1.size();
            total_abortTrip += pass1.size();
        }
//        {
//            Map<VehicleLinkPair, Link> pass1 = reserveVehiclesStoppingOnLink.match(requests, getDivertableVehicles());
//            pass1.entrySet().stream().forEach(this::setVehicleDiversion); // prevents vehicles from being redirected
//            reserveVehicles = pass1.size();
//
//            Map<VehicleLinkPair, Link> pass2 = requestStopsDrivebyVehicles.match(requests, getDivertableVehicles());
//            pass2.entrySet().stream().forEach(this::setVehicleDiversion);
//
//            abortTrip = pass2.size();
//        }

        _printSchedule("after stopping");

        total_emptyDrive = 0;

        // Map<VehicleLinkPair, Link> pass3 = divertIfCurrentDestinationEmpty.match(requests, getDivertableVehicles());
        // pass3.entrySet().stream().forEach(this::setVehicleDiversion);
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

    @Override
    public String getInfoStringBeg() {
        return String.format("%s RV=%3d AT=%3d do=%3d ed=%4d", //
                super.getInfoStringBeg(), //
                total_reserveVehicles, //
                total_abortTrip, //
                total_driveOrder, //
                total_emptyDrive);
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
