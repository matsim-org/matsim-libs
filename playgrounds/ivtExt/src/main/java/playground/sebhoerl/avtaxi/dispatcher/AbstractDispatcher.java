package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;

/**
 * {@link UniversalDispatcher}
 */
public abstract class AbstractDispatcher implements AVDispatcher {
    protected final SingleRideAppender appender;
    protected final EventsManager eventsManager;

    public AbstractDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        this.appender = appender;
        this.eventsManager = eventsManager;
    }

    /**
     * function is invoked from within the registerVehicle function
     * 
     * @param vehicle
     */
    protected abstract void protected_registerVehicle(AVVehicle vehicle);

    @Override
    public final void registerVehicle(AVVehicle vehicle) {
        protected_registerVehicle(vehicle);
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

}
