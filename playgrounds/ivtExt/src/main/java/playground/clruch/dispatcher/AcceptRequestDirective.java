package playground.clruch.dispatcher;

import java.util.Arrays;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;

import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

class AcceptRequestDirective extends AbstractDirective {
    final AVVehicle avVehicle;
    final AVRequest avRequest;
    final double dropoffDurationPerStop;

    public AcceptRequestDirective(AVVehicle avVehicle, AVRequest avRequest, //
            FuturePathContainer futurePathContainer, double dropoffDurationPerStop) {
        super(futurePathContainer);
        this.avVehicle = avVehicle;
        this.avRequest = avRequest;
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    @Override
    void execute(final double getTimeNow) {
        VrpPathWithTravelData vrpPathWithTravelData = futurePathContainer.getVrpPathWithTravelData();

        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
        final double scheduleEndTime = schedule.getEndTime();
        {
            AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
            avStayTask.setEndTime(getTimeNow); // finish the last task now
        }

        schedule.addTask(new AVPickupTask( //
                getTimeNow, // start of pickup
                futurePathContainer.startTime, // end of pickup
                avRequest.getFromLink(), // location of driving start
                Arrays.asList(avRequest))); // serving only one request at a time

        schedule.addTask(new AVDriveTask( //
                vrpPathWithTravelData, Arrays.asList(avRequest)));

        final double endDropoffTime = vrpPathWithTravelData.getArrivalTime() + dropoffDurationPerStop;
        schedule.addTask(new AVDropoffTask( //
                vrpPathWithTravelData.getArrivalTime(), // start of dropoff // TODO function call redundant 
                endDropoffTime, // end of dropoff 
                avRequest.getToLink(), // location of dropoff 
                Arrays.asList(avRequest)));

        // TODO redundant
        if (endDropoffTime < scheduleEndTime)
            schedule.addTask(new AVStayTask( //
                    endDropoffTime, scheduleEndTime, avRequest.getToLink()));

        // jan: following computation is mandatory for the internal scoring function
        final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
        avRequest.getRoute().setDistance(distance);
    }

}
