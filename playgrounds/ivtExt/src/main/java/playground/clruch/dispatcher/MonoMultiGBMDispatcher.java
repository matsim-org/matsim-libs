package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class MonoMultiGBMDispatcher extends RebalancingDispatcher {

    private final int dispatchPeriod;
    private final int rebVehperRequest;
    private Tensor printVals = Tensors.empty();
    private final Network network;

    private MonoMultiGBMDispatcher( //
            Network network, //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebVehperRequest = safeConfig.getInteger("vehiclesPerRequest", 1);
        GlobalAssert.that(rebVehperRequest > 0);
        this.network = network;
        if (rebVehperRequest == 1)
            System.out.println("ATTENTION: only one vehicle is sent to each request, standard GlobalBipartiteMatchingDispatcher");
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {
            // dispatch vehicles to pickup locations
            printVals = BipartiteMatchingUtils.executePickup(this::setRoboTaxiPickup, getDivertableRoboTaxis(), getAVRequests(), //
                    new EuclideanDistanceFunction(), network,false);

            // perform rebalancing with remaining vehicles
            Collection<AVRequest> rebalanceRequests = getMultipleRebalanceRequests(getAVRequests(), rebVehperRequest);
            Tensor notUsed = BipartiteMatchingUtils.executeRebalance(this::setRoboTaxiRebalance, getDivertableRoboTaxis(), rebalanceRequests, //
                    new EuclideanDistanceFunction(), network,false);
        }
    }

    /** @param avRequests all open avRequests
     * @param multipl number of rebalancingVehicles to send of every request
     * @return artificial set of Requests to feed to GBPMatcher */
    private final Collection<AVRequest> getMultipleRebalanceRequests(Collection<AVRequest> avRequests, int multipl) {
        Collection<AVRequest> rebalanceRequests = new ArrayList<>();
        for (AVRequest avRequest : avRequests) {
            for (int i = 0; i < multipl; ++i) {
                rebalanceRequests.add(avRequest);
            }
        }
        return rebalanceRequests;
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
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            return new MonoMultiGBMDispatcher( //
                    network, config, travelTime, router, eventsManager, abstractRequestSelector);
        }
    }
}
