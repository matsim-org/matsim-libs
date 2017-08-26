// code by jph
package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.Optional;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.DispatcherUtils;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** this dispatcher is a bad example, it performs poorly in many scenarios
 * 
 * The dispatcher setsVehiclePickup if a vehicle is driving by a requests, furthermore it diverts
 * vehicles to requests within the distance DISTCLOSE
 * or requests which are waiting for more then MAXWAIT */
public class EdgyDispatcher extends RebalancingDispatcher {
    private final int dispatchPeriod;
    private final double MAXWAIT = 10 * 60;
    private final double DISTCLOSE = 1000.0;

    private EdgyDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        // this.network = network;
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
    }

    int total_abortTrip = 0;
    int total_driveOrder = 0;

    @Override
    public void redispatch(double now) {

        // stop all vehicles which are driving by an open request
        total_abortTrip += DrivebyRequestStopper //
                .stopDrivingBy(DispatcherUtils.getAVRequestsAtLinks(getAVRequests()), getDivertableRoboTaxis(), this::setRoboTaxiPickup);


        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {

            Collection<AVRequest> requests = getAVRequests();
            
            // 1) send stay vehicles to some closeby request closer than DISTCLOSE
            for (RoboTaxi robotaxi : getDivertableRoboTaxis()) {
                if (robotaxi.isInStayTask()) {
                    Optional<AVRequest> someCloseRequest = requests.stream()
                            .filter(v -> CoordUtils.calcEuclideanDistance(v.getFromLink().getCoord(), robotaxi.getDivertableLocation().getCoord()) < DISTCLOSE)
                            .findAny();
                    if (someCloseRequest.isPresent()){
                        setRoboTaxiRebalance(robotaxi, someCloseRequest.get().getFromLink()); 
                    }
                }
            }
            
            
            // 2) send some vehicle to requests waiting longer than MAXWAIT
            for( AVRequest avRequest : requests){
                if(now-avRequest.getSubmissionTime() > MAXWAIT){
                    Optional<RoboTaxi> robotaxi =  getDivertableRoboTaxis().stream().findAny();
                    if(robotaxi.isPresent()){
                        setRoboTaxiRebalance(robotaxi.get(), avRequest.getFromLink());
                    }
                }
            }
        }
    }

    @Override
    protected String getInfoLine() {
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new EdgyDispatcher( //
                    config, travelTime, router, eventsManager);
        }
    }
}
