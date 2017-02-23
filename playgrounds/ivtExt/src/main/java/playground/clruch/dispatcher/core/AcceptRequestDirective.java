package playground.clruch.dispatcher.core;

import java.util.Arrays;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;

import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.ScheduleUtils;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.data.AVVehicle;
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
class AcceptRequestDirective extends FuturePathDirective {
    final AVVehicle avVehicle;
    final AVRequest avRequest;
    final double getTimeNow;
    final double dropoffDurationPerStop;

    public AcceptRequestDirective(AVVehicle avVehicle, AVRequest avRequest, //
            FuturePathContainer futurePathContainer, final double getTimeNow, double dropoffDurationPerStop) {
        super(futurePathContainer);
        this.avVehicle = avVehicle;
        this.avRequest = avRequest;
        this.getTimeNow = getTimeNow;
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
        final AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());
        final double endDropoffTime = vrpPathWithTravelData.getArrivalTime() + dropoffDurationPerStop;

        if (endDropoffTime < scheduleEndTime) {
            
            avStayTask.setEndTime(getTimeNow); // finish the last task now

            schedule.addTask(new AVPickupTask( //
                    getTimeNow, // start of pickup
                    futurePathContainer.startTime, // end of pickup // TODO access is not elegant
                    avRequest.getFromLink(), // location of driving start
                    Arrays.asList(avRequest))); // serving only one request at a time

            schedule.addTask(new AVDriveTask( //
                    vrpPathWithTravelData, Arrays.asList(avRequest)));

            // final double endDropoffTime = vrpPathWithTravelData.getArrivalTime() + dropoffDurationPerStop;
            schedule.addTask(new AVDropoffTask( //
                    vrpPathWithTravelData.getArrivalTime(), // start of dropoff // TODO function call redundant
                    endDropoffTime, // end of dropoff
                    avRequest.getToLink(), // location of dropoff
                    Arrays.asList(avRequest)));

            ScheduleUtils.makeWhole(avVehicle, endDropoffTime, scheduleEndTime, avRequest.getToLink());

            // jan: following computation is mandatory for the internal scoring function
            final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
            avRequest.getRoute().setDistance(distance);
            
        } else 
            reportExecutionBypass(endDropoffTime - scheduleEndTime);
    }

}
