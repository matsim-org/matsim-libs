package playground.clruch.dispatcher;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class TestDispatcher extends RebalancingDispatcher {

    private final int dispatchPeriod;
    private Tensor printVals = Tensors.empty();
    private final Network network;
    AVVehicle avVehicle;
    Link link1;
    Link link2;

    private TestDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network networkIn, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        network = networkIn;
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = getDispatchPeriod(safeConfig, 10); // safeConfig.getInteger("dispatchPeriod", 10);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now == 290) {
            // chose two links
            link1 = network.getLinks().entrySet().stream().filter(v->v.getKey().toString().equals("238283219_1")).findAny().get().getValue();
            System.out.println("link chosen = " + link1.getId().toString());

            link2 = network.getLinks().entrySet().stream().filter(v->v.getKey().toString().equals("42145650_0")).findAny().get().getValue();
            System.out.println("link chosen = " + link2.getId().toString());

            // chose avehicle
            avVehicle = getMaintainedVehicles().stream().findAny().get();
            System.out.println("vehicle chosen = " + avVehicle.getId().toString());
        }

        if (round_now == 300) {
            // rebalance the vehicle to link1
            setVehicleRebalance(avVehicle, link1);

        }

        if (round_now == 900) {
            // rebalance the vehicle to link1
            setVehicleRebalance(avVehicle, link2);

        }

        super.endofStepTasks();
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
            return new TestDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
