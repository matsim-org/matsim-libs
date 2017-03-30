package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class NotAsDumbDispatcher extends UniversalDispatcher {
    private final int dispatchPeriod;

    final Network network; // <- for verifying link references
    final Collection<Link> linkReferences; // <- for verifying link references


    private NotAsDumbDispatcher( //
                                 AVDispatcherConfig avDispatcherConfig, //
                                 TravelTime travelTime, //
                                 ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
                                 EventsManager eventsManager, //
                                 Network network) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        this.network = network;
        linkReferences = new HashSet<>(network.getLinks().values());
//        vehicleRequestMatcher = ;
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);

    }

    /**
     * verify that link references are present in the network
     */
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

    int total_abortTrip = 0;
    int total_driveOrder = 0;

    @Override
    public void redispatch(double now) {
        // simulate this with a dispatchPeriod of about 4 minutes to simulate a heuristic dumb dispatcher

        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());


        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {
            total_abortTrip += new DrivebyRequestStopper(this::setVehicleDiversion).realize(getAVRequestsAtLinks(), getDivertableVehicles());
            {
                Iterator<AVRequest> requestIterator = getAVRequests().iterator();
                for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                    Link dest = vehicleLinkPair.getCurrentDriveDestination();
                    Link vehicleLink = vehicleLinkPair.getDivertableLocation();
                    if (dest == null) { // vehicle in stay task
                        if (requestIterator.hasNext()) {
                            Link closestLink = requestIterator.next().getFromLink();
                            while(requestIterator.hasNext()){
                                Link newLink = requestIterator.next().getFromLink();
                                if(iscloserthan(closestLink,newLink,vehicleLink)){
                                    closestLink = newLink;
                                }
                            }
                            setVehicleDiversion(vehicleLinkPair, closestLink);
                            ++total_driveOrder;
                        } else
                            break;
                    }
                }
            }

        }
    }

    /**
     *
     * @param A
     * @param B
     * @param C
     * @return true if B is closer than A from C
     */
    boolean iscloserthan(Link A,Link B,Link C){
        Coord coordA = A.getCoord();
        Coord coordB = B.getCoord();
        Coord coordC = C.getCoord();

        if(distCoord(coordB,coordC)<distCoord(coordA,coordC)){
            return  true;
        }else
            return false;

    }

    double distCoord(Coord c1, Coord c2){
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.hypot(dx,dy);
    }


    @Override
    public String getInfoLine() {
        return String.format("%s AT=%5d do=%5d", //
                super.getInfoLine(), //
                total_abortTrip, //
                total_driveOrder //
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
            return new NotAsDumbDispatcher( //
                    config, travelTime, router, eventsManager, network);
        }
    }
}
