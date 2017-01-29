package playground.sebhoerl.renault;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.single_heuristic.SingleHeuristicDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.Set;

public class LaDefenseDispatcher implements AVDispatcher {
    private AVDispatcher delegate;
    private Set<Id<Node>> laDefenseFilter;

    public LaDefenseDispatcher(AVDispatcher delegate, Set<Id<Node>> laDefenseFilter) {
        this.delegate = delegate;
        this.laDefenseFilter = laDefenseFilter;
    }

    @Override
    public void onRequestSubmitted(AVRequest request) {
        if (!laDefenseFilter.contains(request.getFromLink().getToNode().getId())) return;
        if (!laDefenseFilter.contains(request.getToLink().getFromNode().getId())) return;

        delegate.onRequestSubmitted(request);
    }

    @Override
    public void onNextTaskStarted(AVTask task) {
        delegate.onNextTaskStarted(task);
    }

    @Override
    public void onNextTimestep(double now) {
        delegate.onNextTimestep(now);
    }

    @Override
    public void addVehicle(AVVehicle vehicle) {
        delegate.addVehicle(vehicle);
    }

    static public class LaDefenseDispatcherFactory implements AVDispatcher.AVDispatcherFactory {
        @Inject private Network network;
        @Inject private EventsManager eventsManager;

        @Inject @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject @Named(LaDefenseModule.LADEFENSE)
        Set<Id<Node>> laDefenseFilter;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            return new LaDefenseDispatcher(new SingleHeuristicDispatcher(
                    config.getParent().getId(),
                    eventsManager,
                    network,
                    new SingleRideAppender(config, router, travelTime)
            ), laDefenseFilter);
        }
    }
}
