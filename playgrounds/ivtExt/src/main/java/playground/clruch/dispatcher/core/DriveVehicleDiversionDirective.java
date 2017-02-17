package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.TaskTracker;

import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.ScheduleUtils;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

class DriveVehicleDiversionDirective extends VehicleDiversionDirective {

    DriveVehicleDiversionDirective(VehicleLinkPair vehicleLinkPair, Link destination, FuturePathContainer futurePathContainer) {
        super(vehicleLinkPair, destination, futurePathContainer);
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        AbstractTask abstractTask = schedule.getCurrentTask(); // <- implies that task is started
        final AVDriveTask avDriveTask = (AVDriveTask) abstractTask;
        {
            TaskTracker taskTracker = avDriveTask.getTaskTracker();
            OnlineDriveTaskTrackerImpl onlineDriveTaskTrackerImpl = (OnlineDriveTaskTrackerImpl) taskTracker;
            final int diversionLinkIndex = onlineDriveTaskTrackerImpl.getDiversionLinkIndex();
            final int lengthOfDiversion = vrpPathWithTravelData.getLinkCount();
            OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
            onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
            GlobalAssert.that(VrpPathUtils.isConsistent(avDriveTask.getPath()));

            final int lengthOfCombination = avDriveTask.getPath().getLinkCount();
            // System.out.println(String.format("[@%d of %d]", diversionLinkIndex, lengthOfCombination));
            if (diversionLinkIndex + lengthOfDiversion != lengthOfCombination)
                throw new RuntimeException("mismatch " + diversionLinkIndex + "+" + lengthOfDiversion + " != " + lengthOfCombination);

        }
        final double scheduleEndTime;
        {
            AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
            scheduleEndTime = avStayTask.getEndTime();
        }
        schedule.removeLastTask(); // remove stay task
        
        ScheduleUtils.makeWhole(vehicleLinkPair.avVehicle, avDriveTask.getEndTime(), scheduleEndTime, destination);
    }

}
