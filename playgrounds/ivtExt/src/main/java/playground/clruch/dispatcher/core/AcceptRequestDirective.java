// code by jph
package playground.clruch.dispatcher.core;

import java.util.Arrays;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

/**
 * for vehicles that are in stay task and should pickup a customer at the link:
 *  1) finish stay task
 *  2) append pickup task
 *  3) append drive task
 *  4) append dropoff task
 *  5) append new stay task
 */
final class AcceptRequestDirective extends FuturePathDirective {
    final RoboTaxi robotaxi;
    final AVRequest avRequest;
    final double getTimeNow;
    final double dropoffDurationPerStop;

    public AcceptRequestDirective(RoboTaxi robotaxi, AVRequest avRequest, //
            FuturePathContainer futurePathContainer, final double getTimeNow, double dropoffDurationPerStop) {
        super(futurePathContainer);
        this.robotaxi = robotaxi;
        this.avRequest = avRequest;
        this.getTimeNow = getTimeNow;
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    @Override
    void executeWithPath(final VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = robotaxi.getSchedule();
        final AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());
        final double begDropoffTime = vrpPathWithTravelData.getArrivalTime();
        final double endDropoffTime = begDropoffTime + dropoffDurationPerStop;

        if (endDropoffTime < scheduleEndTime) {
            
            avStayTask.setEndTime(getTimeNow); // finish the last task now

            schedule.addTask(new AVPickupTask( //
                    getTimeNow, // start of pickup
                    futurePathContainer.getStartTime(), // end of pickup
                    avRequest.getFromLink(), // location of driving start
                    Arrays.asList(avRequest))); // serving only one request at a time

            schedule.addTask(new AVDriveTask( //
                    vrpPathWithTravelData, Arrays.asList(avRequest)));

            // final double endDropoffTime = vrpPathWithTravelData.getArrivalTime() + dropoffDurationPerStop;
            schedule.addTask(new AVDropoffTask( //
                    begDropoffTime, // start of dropoff
                    endDropoffTime, // end of dropoff
                    avRequest.getToLink(), // location of dropoff
                    Arrays.asList(avRequest)));

            ScheduleUtils.makeWhole(robotaxi, endDropoffTime, scheduleEndTime, avRequest.getToLink());

            // jan: following computation is mandatory for the internal scoring function
            final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
            avRequest.getRoute().setDistance(distance);
            
        } else 
            reportExecutionBypass(endDropoffTime - scheduleEndTime);
    }

}
