package playground.clruch.dispatcher;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.NetworkDistanceFunction;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class GlobalBipartiteMatchingDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private Tensor printVals = Tensors.empty();
    private final NetworkDistanceFunction ndf;
    private final Network network;

    private GlobalBipartiteMatchingDispatcher( //
            Network network, //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
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
                    // new EuclideanDistanceFunction(), network, false);
                    ndf, network, false);

        }
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s H=%s", //
                super.getInfoLine(), //
                printVals.toString() //
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
            return new GlobalBipartiteMatchingDispatcher( //
                    network, config, travelTime, router, eventsManager);
        }
    }
}
