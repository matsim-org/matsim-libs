package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;

/**
 * Created by sebastian on 04/10/16.
 */
public interface AVDispatcher {
    void nextLinkEntered(AVDriveTask task);
    void requestSubmitted(AVRequest request);
}
