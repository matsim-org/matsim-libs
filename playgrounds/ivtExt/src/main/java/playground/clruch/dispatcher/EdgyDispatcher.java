// code by jph
package playground.clruch.dispatcher;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

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

    // final Network network; // <- for verifying link references
    private final double MAXWAIT = 10 * 60;
    private final double DISTCLOSE = 3000.0;

    private EdgyDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        // this.network = network;
        dispatchPeriod = getDispatchPeriod(safeConfig, 10);
    }

    int total_abortTrip = 0;
    int total_driveOrder = 0;

    @Override
    public void redispatch(double now) {

        // stop all vehicles which are driving by an open request
        total_abortTrip += DrivebyRequestStopper.stopDrivingBy(getAVRequestsAtLinks(), getDivertableRoboTaxis(), this::setRoboTaxiPickup);

        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {

            // iterate requests: send vehicles to some request closer than distClose m
            // or to a request waiting for more than double waitMax
            Iterator<AVRequest> requestIterator = getAVRequests().iterator();
            for (RoboTaxi roboTaxi : getDivertableRoboTaxis()) {
                if (roboTaxi.isVehicleInStayTask()) {
                    if (requestIterator.hasNext()) {
                        AVRequest nextRequest = requestIterator.next();
                        Link link = nextRequest.getFromLink();
                        if (CoordUtils.calcEuclideanDistance(link.getCoord(), roboTaxi.getDivertableLocation().getCoord()) < DISTCLOSE
                                || now - nextRequest.getSubmissionTime() > MAXWAIT) {
                            setRoboTaxiRebalance(roboTaxi, link);
                            ++total_driveOrder;
                        }
                    } else
                        break;
                }
            }
        }
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new EdgyDispatcher( //
                    config, travelTime, router, eventsManager);
        }
    }
}
