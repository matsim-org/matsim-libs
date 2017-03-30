package playground.clruch.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public class AVLocation extends AVTaskAdapter {

    public static Link of(AVVehicle avVehicle) {
        Schedule schedule = avVehicle.getSchedule();
        return new AVLocation(schedule.getCurrentTask()).link;
    }

    /**
     * DO NOT INITIALIZE THE LINK VARIABLE !!!
     */
    private Link link;

    private AVLocation(Task task) {
        super(task);
    }

    @Override
    public void handle(AVPickupTask avPickupTask) {
        link = avPickupTask.getLink();
    }

    @Override
    public void handle(AVDropoffTask avDropoffTask) {
        link = avDropoffTask.getLink();
    }

    @Override
    public void handle(AVDriveTask avDriveTask) {
        TaskTracker taskTracker = avDriveTask.getTaskTracker();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint(); // there is a slim chance that function returns null
        if (linkTimePair != null)
            link = linkTimePair.link;
    }

    @Override
    public void handle(AVStayTask avStayTask) {
        link = avStayTask.getLink();
    }
}
