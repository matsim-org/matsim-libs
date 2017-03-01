package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public class VehicleLinkPair {
    public final AVVehicle avVehicle;
    public final LinkTimePair linkTimePair;

    public VehicleLinkPair(AVVehicle avVehicle, LinkTimePair linkTimePair) {
        this.avVehicle = avVehicle;
        this.linkTimePair = linkTimePair;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    /**
     * @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on
     */
    public Link getCurrentDriveDestination() {
        final Schedule schedule = avVehicle.getSchedule();
        final AVTask avTask = (AVTask) schedule.getCurrentTask();
        if (avTask.getAVTaskType().equals(AVTask.AVTaskType.DRIVE)) {
            final AVDriveTask avDriveTask = (AVDriveTask) avTask;
            return avDriveTask.getPath().getToLink();
        }
        return null;
    }

}
