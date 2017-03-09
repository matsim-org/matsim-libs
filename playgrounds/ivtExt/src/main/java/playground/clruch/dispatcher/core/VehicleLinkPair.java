package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public class VehicleLinkPair {
    public final AVVehicle avVehicle;
    public final LinkTimePair linkTimePair;
    private final Link currentDriveDestination;

    public VehicleLinkPair(AVVehicle avVehicle, LinkTimePair linkTimePair, Link currentDriveDestination) {
        this.avVehicle = avVehicle;
        this.linkTimePair = linkTimePair;
        this.currentDriveDestination = currentDriveDestination;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    /**
     * @return true if vehicle is driving and divertible link is also destination link of drive task;
     *         false if vehicle is in stay task, or divertible link is not destination of drive task of vehicle.
     */
    public boolean isDivertableLocationAlsoDriveTaskDestination() {
        return linkTimePair.link == currentDriveDestination;
    }

    /**
     * @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on
     */
    public Link getCurrentDriveDestination() {
        final Link link;
        final Schedule schedule = avVehicle.getSchedule();
        final AVTask avTask = (AVTask) schedule.getCurrentTask();
        if (avTask.getAVTaskType().equals(AVTask.AVTaskType.DRIVE)) {
            final AVDriveTask avDriveTask = (AVDriveTask) avTask;
            link = avDriveTask.getPath().getToLink();
        } else
            link = null;
        // TODO simply replace by return currentDriveDestination;
        GlobalAssert.that(currentDriveDestination == link);
        return link;
    }

}
