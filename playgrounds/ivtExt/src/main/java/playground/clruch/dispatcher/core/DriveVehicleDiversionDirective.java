// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.TaskTracker;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

/**
 * for vehicles that are currently driving, but should go to a new destination:
 *  1) change path of current drive task
 *  2) remove former stay task with old destination
 *  3) append new stay task
 */
final class DriveVehicleDiversionDirective extends VehicleDiversionDirective {

    DriveVehicleDiversionDirective(RoboTaxi robotaxi, Link destination, FuturePathContainer futurePathContainer) {
        super(robotaxi, destination, futurePathContainer);
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = robotaxi.getSchedule();
        final AVDriveTask avDriveTask = (AVDriveTask) schedule.getCurrentTask(); // <- implies that task is started
        final AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();

        TaskTracker taskTracker = avDriveTask.getTaskTracker();
        OnlineDriveTaskTrackerImpl onlineDriveTaskTrackerImpl = (OnlineDriveTaskTrackerImpl) taskTracker;
        final int diversionLinkIndex = onlineDriveTaskTrackerImpl.getDiversionLinkIndex();
        final int lengthOfDiversion = vrpPathWithTravelData.getLinkCount();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        final double newEndTime = vrpPathWithTravelData.getArrivalTime();

        if (newEndTime < scheduleEndTime) {

            onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
            GlobalAssert.that(VrpPathUtils.isConsistent(avDriveTask.getPath()));

            final int lengthOfCombination = avDriveTask.getPath().getLinkCount();
            // System.out.println(String.format("[@%d of %d]", diversionLinkIndex, lengthOfCombination));
            if (diversionLinkIndex + lengthOfDiversion != lengthOfCombination)
                throw new RuntimeException("mismatch " + diversionLinkIndex + "+" + lengthOfDiversion + " != " + lengthOfCombination);

            GlobalAssert.that(avDriveTask.getEndTime() == newEndTime);

            schedule.removeLastTask(); // remove former stay task with old destination
            ScheduleUtils.makeWhole(robotaxi, newEndTime, scheduleEndTime, destination);

        } else
            reportExecutionBypass(newEndTime - scheduleEndTime);
    }

}
