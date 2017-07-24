package playground.clruch.dispatcher;

import java.util.ArrayList;

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
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class TestDispatcher extends RebalancingDispatcher {

    private final int dispatchPeriod;
    private Tensor printVals = Tensors.empty();
    private final Network network;
    AVVehicle avVehicle;
    ArrayList<Link> links = new ArrayList<>();
    ArrayList<AVRequest> requests = new ArrayList<>();
    ArrayList<AVVehicle> vehicles = new ArrayList<>();

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
            links.add(network.getLinks().entrySet().stream().filter(v -> v.getKey().toString().equals("238283219_1")).findAny().get().getValue());
            links.add(network.getLinks().entrySet().stream().filter(v -> v.getKey().toString().equals("42145650_0")).findAny().get().getValue());
            links.add(network.getLinks().entrySet().stream().filter(v -> v.getKey().toString().equals("9904192_0")).findAny().get().getValue());
            links.add(network.getLinks().entrySet().stream().filter(v -> v.getKey().toString().equals("9904301_1")).findAny().get().getValue());
            links.add(network.getLinks().entrySet().stream().filter(v -> v.getKey().toString().equals("57932785_0")).findAny().get().getValue());

            // chose avehicle
            for (AVVehicle avVehicle : getMaintainedVehicles()) {
                vehicles.add(avVehicle);
            }
        }

        if (round_now == 300) {
            System.out.println(" there are " + getDivertableVehicles().size() + " divertable vehicles");
        }

        if (round_now == 24180) {
            setVehicleRebalance(vehicles.get(0), links.get(0));
            setVehicleRebalance(vehicles.get(1), links.get(1));
            setVehicleRebalance(vehicles.get(2), links.get(2));
            setVehicleRebalance(vehicles.get(3), links.get(3));
            setVehicleRebalance(vehicles.get(4), links.get(4));

            System.out.println(" there are " + getDivertableVehicles().size() + " divertable vehicles");
        }
        

        if (round_now == 24210) {
            for (AVRequest avRequest : getAVRequests()) {
                requests.add(avRequest);
            }
            AVRequest theRequest = requests.get(0);

            setVehiclePickup(vehicles.get(0), theRequest);
        }        

        
        if (round_now == 24220) {
            System.out.println(" there are " + getDivertableVehicles().size() + " divertable vehicles");
            
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
            return new TestDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}

// if (round_now == 300 ) {
// // rebalance the vehicle to link1
// setVehicleRebalance(avVehicle, links.get(0));
//
// }
//
// if (round_now == 310 ) {
// // rebalance the vehicle to link1
// setVehicleRebalance(avVehicle, links.get(1));
//
// }
//
// if (round_now == 320 ) {
// // rebalance the vehicle to link1
// setVehicleRebalance(avVehicle, links.get(0));
//
// }
//
// if (round_now == 330 ) {
// // rebalance the vehicle to link1
// setVehicleRebalance(avVehicle, links.get(1));
//
// }
//
// if (round_now == 24100 ) {
// // rebalance the vehicle to link1
// setVehicleRebalance(avVehicle, links.get(2));
// }
//
//
// if(round_now == 24210){
// for(AVRequest avRequest : getAVRequests()){
// requests.add(avRequest);
// }
// AVRequest theRequest = requests.get(0);
//
// setVehiclePickup(avVehicle, theRequest);
// }
//
// if(round_now == 24440){
// setVehicleRebalance(avVehicle, links.get(0));
// }
