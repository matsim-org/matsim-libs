package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.core.config.ConfigGroup;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public interface AVDispatcher {
    void onRequestSubmitted(AVRequest request);
    void onNextTaskStarted(AVVehicle vehicle);
    void onNextTimestep(double now);

    void addVehicle(AVVehicle vehicle);

    interface AVDispatcherFactory {
        AVDispatcher createDispatcher(AVDispatcherConfig config);
    }
}
