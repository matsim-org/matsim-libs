package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.dispatcher.UniversalDispatcher;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;

/** 
 * TODO this will be integrated with {@link UniversalDispatcher}
 */
public abstract class AbstractDispatcher implements AVDispatcher {
    protected final SingleRideAppender appender; // TODO this is not used here so REMOVE!   
    protected final EventsManager eventsManager; // TODO this is not absolutely needed here so REMOVE!

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
