package playground.sebhoerl.avtaxi.dispatcher;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public interface AVDispatcher {
    void onRequestSubmitted(AVRequest request);
    void onNextTaskStarted(AVTask task);
    void onNextTimestep(double now);

    void addVehicle(AVVehicle vehicle);
}
