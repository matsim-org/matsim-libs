package playground.clruch.dispatcher;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
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
     * TODO temporary solution, function is only called once to check if vehicle is in stayMode
     * 
     * @return null if vehicle does not have a destination
     */
    public Link getDestination() {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
        // List<AbstractTask> tasks = schedule.getTasks();
        // if (!tasks.isEmpty() && schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED))
        AbstractTask abstractTask = schedule.getCurrentTask();
        AVTask avTask = (AVTask) abstractTask;
        if (avTask.getAVTaskType().equals(AVTask.AVTaskType.DRIVE)) {
            AVDriveTask avDriveTask = (AVDriveTask) avTask;
            return avDriveTask.getPath().getToLink();
        }
        return null;
    }

}
