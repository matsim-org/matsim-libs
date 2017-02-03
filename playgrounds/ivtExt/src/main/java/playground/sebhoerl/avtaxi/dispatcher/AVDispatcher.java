package playground.sebhoerl.avtaxi.dispatcher;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
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

    void addVehicle(AVVehicle vehicle);

    interface AVDispatcherFactory {
        AVDispatcher createDispatcher(AVDispatcherConfig config);
    }
}
