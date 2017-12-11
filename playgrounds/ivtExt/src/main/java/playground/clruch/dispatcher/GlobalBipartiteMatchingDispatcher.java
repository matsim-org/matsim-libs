package playground.clruch.dispatcher;

import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.NetworkDistanceFunction;
import playground.clruch.utils.SafeConfig;

public class GlobalBipartiteMatchingDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private Tensor printVals = Tensors.empty();
    private final NetworkDistanceFunction ndf;
    private final Network network;
    private HashMap<RoboTaxi, Double> notMovingSince = new HashMap();
    private HashMap<RoboTaxi, Link> lastLocation = new HashMap();

    private GlobalBipartiteMatchingDispatcher( //
            Network network, //
            Config config, //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(config, avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        this.ndf = new NetworkDistanceFunction(network);
        this.network = network;
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {
            printVals = BipartiteMatchingUtils.executePickup(this::setRoboTaxiPickup, //
                    getDivertableRoboTaxis(), getAVRequests(), //
                    new EuclideanDistanceFunction(), network, false);
            // ndf, network, false);

            for (RoboTaxi robotaxi : getRoboTaxis()) {
                double notMoved = 0.0;
                if(!lastLocation.containsKey(robotaxi)){
                    lastLocation.put(robotaxi, robotaxi.getLastKnownLocation());
                }
                if (lastLocation.get(robotaxi).equals(robotaxi.getLastKnownLocation())) {
                    if (robotaxi.getAVStatus().equals(AVStatus.DRIVETOCUSTOMER)) {
                        if (notMovingSince.containsKey(robotaxi)) {
                            notMoved = notMoved + notMovingSince.get(robotaxi);
                        }
                    }
                }
                notMovingSince.put(robotaxi, notMoved + dispatchPeriod);
                lastLocation.put(robotaxi, robotaxi.getLastKnownLocation());
            }

            for (RoboTaxi robotaxi : getRoboTaxis()) {
                if (notMovingSince.containsKey(robotaxi)) {
                    if (notMovingSince.get(robotaxi) > 1800) {
                        System.out.println(robotaxi.getLastKnownLocation().getId().toString());
                    }
                }
            }
        }

    }

    @Override
    protected String getInfoLine() {
        return String.format("%s H=%s", //
                super.getInfoLine(), //
                printVals.toString() // This is where Dispatcher@ V... R... MR.. H is printed on console
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

        @Inject
        private Config config;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig) {
            return new GlobalBipartiteMatchingDispatcher( //
                    network, config, avconfig, travelTime, router, eventsManager);
        }
    }
}
