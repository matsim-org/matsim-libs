package playground.clruch.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class EvalDispatcher extends UniversalDispatcher {

    public EvalDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager) {
        super(config, travelTime, router, eventsManager);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void redispatch(double now) {
        // TODO Auto-generated method stub

    }

}
