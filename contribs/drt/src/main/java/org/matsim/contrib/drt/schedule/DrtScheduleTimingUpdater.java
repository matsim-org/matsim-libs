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
                List<AcceptedDrtRequest> updatedPudos = new ArrayList<>();
                for (AcceptedDrtRequest pickup : stopTask.getPickupRequests().values()) {
                    double expectedPickupTime = Math.max(stopTask.getBeginTime(), pickup.getEarliestStartTime());
                    expectedPickupTime += stopDurationProvider.calcPickupDuration(vehicle, pickup.getRequest());
                    if(expectedPickupTime != pickup.getPlannedPickupTime().seconds()) {
                        updatedPudos.add(AcceptedDrtRequest.newBuilder(pickup).plannedPickupTime(expectedPickupTime).build());
                    }
                }

                for (AcceptedDrtRequest updatedPickup : updatedPudos) {
                    stopTask.removePickupRequest(updatedPickup.getId());
                    stopTask.addPickupRequest(updatedPickup);
                }
                updatedPudos.clear();


                for (AcceptedDrtRequest dropoff : stopTask.getDropoffRequests().values()) {
                    double expectedDropoffTime = stopTask.getBeginTime() + dropoff.getDropoffDuration();
                    if(expectedDropoffTime != dropoff.getPlannedDropoffTime().seconds()) {
                        updatedPudos.add(AcceptedDrtRequest.newBuilder(dropoff).plannedDropoffTime(expectedDropoffTime).build());
                    }
                }

                for (AcceptedDrtRequest updatedPickup : updatedPudos) {
                    stopTask.removeDropoffRequest(updatedPickup.getId());
                    stopTask.addDropoffRequest(updatedPickup);
                }
            }
        }
    }
}
