package playground.clruch.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.netdata.VirtualNetwork;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class ChopDispatcher extends PartitionedDispatcher {

    public ChopDispatcher(AVDispatcherConfig config, TravelTime travelTime, ParallelLeastCostPathCalculator router, EventsManager eventsManager, VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void redispatch(double now) {
        // TODO Auto-generated method stub

    }

}
