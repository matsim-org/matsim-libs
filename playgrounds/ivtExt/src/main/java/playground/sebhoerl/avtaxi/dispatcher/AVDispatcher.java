package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.framework.AVQSimModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public interface AVDispatcher {
    /**
     * function called from {@link AVOptimizer} when new requests are available
     * 
     * @param request
     */
    void onRequestSubmitted(AVRequest request);
    
    /**
     * function called from {@link AVOptimizer} TODO comment logic
     * 
     * @param task
     */
    void onNextTaskStarted(AVTask task);
    
    void onNextTimestep(double now);

    /**
     * TODO function called in {@link AVQSimModule}
     * during initialization of an {@link AVDispatcher}
     * 
     * @param vehicle
     */
    void registerVehicle(AVVehicle vehicle);

    /**
     * Function was introduced with sebhoerl's help
     * function is invoked from {@link AVOptimizer}
     * whenever a vehicle moves to the next link.
     * 
     * However, this happens in a multi-threaded environment,
     * so information may not be in-sync from within the dispatcher.
     * Use with care.  
     * 
     * @param avVehicle
     * @param driveTask
     * @param linkTimePair
     */
    // TODO probably can remove function since we have access to this info nevertheless
    @Deprecated
    default void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // default behavior is intentionally empty
    }

    interface AVDispatcherFactory {
        AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig);
    }
}
