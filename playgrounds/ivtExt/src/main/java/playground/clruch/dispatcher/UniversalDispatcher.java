package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class UniversalDispatcher extends AbstractDispatcher {

    public final List<AVVehicle> vehicles = new ArrayList<>();
    @Deprecated
    public Queue<AVVehicle> availableVehicles = new LinkedList<>(); // TODO remove
    @Deprecated
    final private Queue<AVRequest> pendingRequests = new LinkedList<>(); // TODO remove

    // ---
    public UniversalDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
    }

    Collection<VehicleLinkPair> getDivertableVehicles() {
        return null;
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request);
    }

    // TODO this will not be necessary!!!
//    @Override
//    public void onNextTaskStarted(AVTask task) {
//    }

    @Override
    public final void onNextTimestep(double now) {
        appender.update();
        reoptimize(now);

    }
    
    public abstract void reoptimize(double now) ;

    @Override
    protected final void protected_registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
        availableVehicles.add(vehicle);

    }

}
