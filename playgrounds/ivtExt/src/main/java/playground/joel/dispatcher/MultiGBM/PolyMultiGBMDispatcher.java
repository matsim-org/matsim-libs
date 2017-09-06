package playground.joel.dispatcher.MultiGBM;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

@Deprecated // this version is not used anymore, delete eventually. Look at playground.clruch.dispatcher. MonomultiGBMDispatcher
// version is compiliing but not working.
public class PolyMultiGBMDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private final int maxMatchNumber; // implementation may not use this
    private final int vehiclesPerRequest;
    private Tensor printVals = Tensors.empty();
    private final Map<String, Integer> vehicleIDs = new HashMap<>(); // for faster ID search

    private PolyMultiGBMDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
        maxMatchNumber = safeConfig.getInteger("maxMatchNumber", Integer.MAX_VALUE);
        vehiclesPerRequest = safeConfig.getInteger("vehiclesPerRequest", 1);
        GlobalAssert.that(vehiclesPerRequest > 0);
        if (vehiclesPerRequest == 1)
            System.out.println("ATTENTION: only one vehicle is sent to each request, standard HungarianDispatcher");
    }

    public int getId(AVVehicle avVehicle) {
        String string = avVehicle.getId().toString();
        GlobalAssert.that(string.startsWith("av_av_op1_"));
        if (!vehicleIDs.containsKey(string)) {
            int value = Integer.parseInt(string.substring(10)) - 1;
            // System.out.println("ID=[" + string + "] -> " + value);
            vehicleIDs.put(string, value);
        }
        return vehicleIDs.get(string);
    }

    // public Supplier<Collection<VehicleLinkPair>> supplier(int fleetSection) {
    // Supplier<Collection<VehicleLinkPair>> supplier = () -> getDivertableVehicles().stream() //
    // .filter(vlp -> getId(vlp.avVehicle) % vehiclesPerRequest == fleetSection).collect(Collectors.toList());
    // return supplier;
    // }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        // new InOrderOfArrivalMatcher(this::setAcceptRequest) //
        // .match(getStayVehicles(), getAVRequestsAtLinks());

        // if (round_now % dispatchPeriod == 0) {
        // for (int fleetSection = 0; fleetSection < vehiclesPerRequest; fleetSection++) {
        // printVals = BipartiteMatchingUtils.globalBipartiteMatching(this, supplier(fleetSection), this.getAVRequestsAtLinks());
        // }
        // }
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
            return new PolyMultiGBMDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
