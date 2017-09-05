// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.router.FuturePathContainer;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

/**
 * for vehicles that are in stay task:
 *  1) stop stay task by setting stop time to 'now'
 *  2) append drive task
 *  3) append stay task for later
 */
final class StayVehicleDiversionDirective extends VehicleDiversionDirective {

    StayVehicleDiversionDirective(RoboTaxi vehicleLinkPair, Link destination, FuturePathContainer futurePathContainer) {
        super(vehicleLinkPair, destination, futurePathContainer);
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = robotaxi.getSchedule();
        final AVStayTask avStayTask = (AVStayTask) schedule.getCurrentTask(); // <- implies that task is started
        final double scheduleEndTime = avStayTask.getEndTime(); // typically 108000.0
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());

        final AVDriveTask avDriveTask = new AVDriveTask(vrpPathWithTravelData);
        final double endDriveTask = avDriveTask.getEndTime();

        if (endDriveTask < scheduleEndTime) {

            GlobalAssert.that(avStayTask.getStatus() == Task.TaskStatus.STARTED);
            avStayTask.setEndTime(robotaxi.getDivertableTime());

            schedule.addTask(avDriveTask);

            ScheduleUtils.makeWhole(robotaxi, endDriveTask, scheduleEndTime, destination);

        } else
            reportExecutionBypass(endDriveTask - scheduleEndTime);
    }

}
