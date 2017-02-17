package playground.sebhoerl.avtaxi.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

public interface AVTask extends Task {
    public static enum AVTaskType {
        PICKUP, //
        DROPOFF, //
        DRIVE, //
        STAY
    }

    AVTaskType getAVTaskType();

}
