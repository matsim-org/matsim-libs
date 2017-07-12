package playground.joel.dispatcher.MultiGBM;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.BipartiteMatchingUtils;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonoMultiGBMDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private final int maxMatchNumber; // implementation may not use this
    private final int vehiclesPerRequest;
    private Tensor printVals = Tensors.empty();

    private MonoMultiGBMDispatcher( //
                                    AVDispatcherConfig avDispatcherConfig, //
                                    TravelTime travelTime, //
                                    ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
                                    EventsManager eventsManager, //
                                    Network network, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = getDispatchPeriod(safeConfig, 10); // safeConfig.getInteger("dispatchPeriod", 10);
        maxMatchNumber = safeConfig.getInteger("maxMatchNumber", Integer.MAX_VALUE);
        vehiclesPerRequest = safeConfig.getInteger("vehiclesPerRequest", 1);
        GlobalAssert.that(vehiclesPerRequest > 0);
        if (vehiclesPerRequest == 1)
            System.out.println("ATTENTION: only one vehicle is sent to each request, standard HungarianDispatcher");
    }

    public final Map<Link, List<AVRequest>> getMultiAVRequestsAtLinks() {
        Map<Link, List<AVRequest>> multiAVRequestsAtLinks = new HashMap<>();
        this.getAVRequestsAtLinks().forEach((link, list) -> {
            List<AVRequest> tempList = new ArrayList<>();
            list.forEach(avRequest -> {
                for (int i = 0; i < vehiclesPerRequest; i++) {
                    tempList.add(avRequest);
                }
            });
            multiAVRequestsAtLinks.put(link, tempList);
        });
        return multiAVRequestsAtLinks;
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());

        if (round_now % dispatchPeriod == 0) {
            printVals = BipartiteMatchingUtils.globalBipartiteMatching(this, () -> getDivertableVehicles(), getMultiAVRequestsAtLinks());
        }
    }

    @Override
    public String getInfoLine() {
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
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
