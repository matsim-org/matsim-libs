package org.matsim.contrib.drt.schedule;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Drt specific timing updater that also updates current timing estimates on accepted requests.
 */
public class DrtScheduleTimingUpdater implements ScheduleTimingUpdater {

    private final ScheduleTimingUpdater delegate;
    private final PassengerStopDurationProvider stopDurationProvider;

    public DrtScheduleTimingUpdater(ScheduleTimingUpdater delegate,
                                    PassengerStopDurationProvider stopDurationProvider) {
        this.delegate = delegate;
        this.stopDurationProvider = stopDurationProvider;
    }

    @Override
    public void updateBeforeNextTask(DvrpVehicle vehicle) {
        if (vehicle.getSchedule().getStatus() != Schedule.ScheduleStatus.STARTED) {
            return;
        }
        delegate.updateBeforeNextTask(vehicle);
        updatePuDoTimes(vehicle, vehicle.getSchedule().getCurrentTask().getTaskIdx());
    }

    @Override
    public void updateTimings(DvrpVehicle vehicle) {
        if (vehicle.getSchedule().getStatus() != Schedule.ScheduleStatus.STARTED) {
            return;
        }
        delegate.updateTimings(vehicle);
        updatePuDoTimes(vehicle, vehicle.getSchedule().getCurrentTask().getTaskIdx());
    }


    @Override
    public void updateTimingsStartingFromTaskIdx(DvrpVehicle vehicle, int startIdx, double newBeginTime) {
        delegate.updateTimingsStartingFromTaskIdx(vehicle, startIdx, newBeginTime);
        updatePuDoTimes(vehicle, startIdx);
    }

    private void updatePuDoTimes(DvrpVehicle vehicle, int startIdx) {

        Schedule schedule = vehicle.getSchedule();
        List<? extends Task> tasks = schedule.getTasks();


        for (int i = startIdx; i < tasks.size(); i++) {
            if(tasks.get(i) instanceof DrtStopTask stopTask) {
                for (AcceptedDrtRequest pickup : stopTask.getPickupRequests().values()) {
                    double expectedPickupTime = Math.max(stopTask.getBeginTime(), pickup.getEarliestStartTime());
                    expectedPickupTime += stopDurationProvider.calcPickupDuration(vehicle, pickup.getRequest());

                    // if the stop task ends earlier, it means that somewhere upstream it was decided that the stop duration
                    // is shorter, which can happen if a request is merged with an existing stop
                    expectedPickupTime = Math.min(expectedPickupTime, stopTask.getEndTime());

                    RequestTiming requestTiming = pickup.getRequestTiming();
                    if(requestTiming.getPlannedPickupTime().isUndefined()
                            || expectedPickupTime != requestTiming.getPlannedPickupTime().seconds()) {
                        requestTiming.updatePlannedPickupTime(expectedPickupTime);
                    }
                }


                for (AcceptedDrtRequest dropoff : stopTask.getDropoffRequests().values()) {
                    double expectedDropoffTime = stopTask.getBeginTime() + dropoff.getDropoffDuration();
                    RequestTiming requestTiming = dropoff.getRequestTiming();
                    if(requestTiming.getPlannedDropoffTime().isUndefined() || expectedDropoffTime != requestTiming.getPlannedDropoffTime().seconds()) {
                        requestTiming.updatePlannedDropoffTime(expectedDropoffTime);
                    }
                }
            }
        }
    }
}
