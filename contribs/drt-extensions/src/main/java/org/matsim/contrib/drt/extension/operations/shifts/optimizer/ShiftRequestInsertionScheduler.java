package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.StopDurationEstimator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftRequestInsertionScheduler implements RequestInsertionScheduler {

    private final MobsimTimer timer;
    private final TravelTime travelTime;
    private final ScheduleTimingUpdater scheduleTimingUpdater;
    private final ShiftDrtTaskFactory taskFactory;
	private StopDurationEstimator stopDurationEstimator;


	public ShiftRequestInsertionScheduler(MobsimTimer timer, TravelTime travelTime,
			ScheduleTimingUpdater scheduleTimingUpdater, ShiftDrtTaskFactory taskFactory,
										  StopDurationEstimator stopDurationEstimator) {
		this.timer = timer;
		this.travelTime = travelTime;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.taskFactory = taskFactory;
		this.stopDurationEstimator = stopDurationEstimator;
	}


    @Override
    public PickupDropoffTaskPair scheduleRequest(AcceptedDrtRequest request, InsertionWithDetourData insertion) {
        var pickupTask = insertPickup(request, insertion);
        var dropoffTask = insertDropoff(request, insertion, pickupTask);
        return new PickupDropoffTaskPair(pickupTask, dropoffTask);
    }

    private DrtStopTask insertPickup(AcceptedDrtRequest request, InsertionWithDetourData insertionWithDetourData) {
		var insertion = insertionWithDetourData.insertion;
        VehicleEntry vehicleEntry = insertion.vehicleEntry;
        Schedule schedule = vehicleEntry.vehicle.getSchedule();
        List<Waypoint.Stop> stops = vehicleEntry.stops;
        int pickupIdx = insertion.pickup.index;
        int dropoffIdx = insertion.dropoff.index;
		var detourData = insertionWithDetourData.detourData;

        Schedule.ScheduleStatus scheduleStatus = schedule.getStatus();
        Task currentTask = scheduleStatus == Schedule.ScheduleStatus.PLANNED ? null : schedule.getCurrentTask();
        Task beforePickupTask;

        if (pickupIdx == 0 && scheduleStatus != Schedule.ScheduleStatus.PLANNED && DRIVE.isBaseTypeOf(currentTask)) {
            LinkTimePair diversion = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            if (diversion != null) { // divert vehicle
                beforePickupTask = currentTask;
                VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
                        vehicleEntry.start.time, detourData.detourToPickup, travelTime);
                ((OnlineDriveTaskTracker) beforePickupTask.getTaskTracker()).divertPath(vrpPath);
            } else { // too late for diversion
                if (request.getFromLink() != vehicleEntry.start.link) { // add a new drive task
                    VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
                            vehicleEntry.start.time, detourData.detourToPickup, travelTime);
                    beforePickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
                    schedule.addTask(currentTask.getTaskIdx() + 1, beforePickupTask);
                } else { // no need for a new drive task
                    beforePickupTask = currentTask;
                }
            }
        } else { // insert pickup after an existing stop/stay task
            DrtStayTask stayTask = null;
            DrtStopTask stopTask = null;
            if (pickupIdx == 0) {
                if (scheduleStatus == Schedule.ScheduleStatus.PLANNED) {// PLANNED schedule
                    stayTask = (DrtStayTask) schedule.getTasks().get(0);
                    stayTask.setEndTime(stayTask.getBeginTime());// could get later removed with ScheduleTimingUpdater
                } else if (STAY.isBaseTypeOf(currentTask)) {
                    stayTask = (DrtStayTask) currentTask; // ongoing stay task
                    double now = timer.getTimeOfDay();
                    if (stayTask.getEndTime() > now) { // stop stay task; a new stop/drive task can be inserted now
                        stayTask.setEndTime(now);
                    }
                } else {
                    stopTask = (DrtStopTask) currentTask; // ongoing stop task
                }
            } else {
                stopTask = stops.get(pickupIdx - 1).task; // future stop task
            }

            if (stopTask != null && request.getFromLink() == stopTask.getLink()) { // no detour; no new stop task
                // add pickup request to stop task
                stopTask.addPickupRequest(request);
                if (stopTask instanceof OperationalStop) {
                    if (request.getEarliestStartTime() > stopTask.getEndTime()) {
                        throw new RuntimeException("Cannot serve request!");
                    }
                } else {
					double stopDuration = stopDurationEstimator.calcDuration(vehicleEntry.vehicle,
							stopTask.getDropoffRequests().values(), stopTask.getPickupRequests().values());
                    stopTask.setEndTime(Math.max(stopTask.getBeginTime() + stopDuration, request.getEarliestStartTime()));
                }

                /// ADDED
                //// TODO this is copied, but has not been updated !!!!!!!!!!!!!!!
                // add drive from pickup
                if (pickupIdx == dropoffIdx) {
                    // remove drive i->i+1 (if there is one)
                    if (pickupIdx < stops.size()) {// there is at least one following stop
                        DrtStopTask nextStopTask = stops.get(pickupIdx).task;
                        if (nextStopTask instanceof ShiftChangeOverTask) {
                            if (stopTask.getTaskIdx() + 3 != nextStopTask.getTaskIdx()) {// there must a drive and stay task in
                                // between
                                throw new RuntimeException();
                            }
                        } else if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {// there must a drive task in
                            // between
                            throw new RuntimeException();
                        }
                        if (nextStopTask instanceof ShiftChangeOverTask) {
                            if (stopTask.getTaskIdx() + 3 == nextStopTask.getTaskIdx()) {// there must a drive task and stay in
                                // between
                                int driveTaskIdx = stopTask.getTaskIdx() + 1;
                                final Task task = schedule.getTasks().get(driveTaskIdx);
                                schedule.removeTask(task);
                            }
                        } else {
                            if (stopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {// there must a drive task in
                                // between
                                int driveTaskIdx = stopTask.getTaskIdx() + 1;
                                final Task task = schedule.getTasks().get(driveTaskIdx);
                                schedule.removeTask(task);
                            }
                        }
                    }

                    Link toLink = request.getToLink(); // pickup->dropoff

                    VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink,
                            stopTask.getEndTime(), detourData.detourFromPickup, travelTime);
                    Task driveFromPickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath,
                            DrtDriveTask.TYPE);
                    schedule.addTask(stopTask.getTaskIdx() + 1, driveFromPickupTask);

                    // update timings
                    // TODO should be enough to update the timeline only till dropoffIdx...
                    scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
                            stopTask.getTaskIdx() + 2, driveFromPickupTask.getEndTime());
                    ///////
                }

                return stopTask;
            } else {
                StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;

                // remove drive i->i+1 (if there is one)
                if (pickupIdx < stops.size()) {// there is at least one following stop

                    DrtStopTask nextStopTask = stops.get(pickupIdx).task;

                    if (nextStopTask instanceof ShiftChangeOverTask) {
                        if (stayOrStopTask.getTaskIdx() + 3 == nextStopTask.getTaskIdx()) {
                            // removing the drive task that is in between
                            int driveTaskIdx = stayOrStopTask.getTaskIdx() + 1;
                            final Task task = schedule.getTasks().get(driveTaskIdx);
                            schedule.removeTask(task);
                        }
                    } else {
                        // check: if there is at most one drive task in between
                        if (stayOrStopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx() //
                                && stayTask != null && stayTask.getTaskIdx() + 1 != nextStopTask.getTaskIdx()) {
                            throw new RuntimeException();
                        }
                        if (stayOrStopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {
                            // removing the drive task that is in between
                            int driveTaskIdx = stayOrStopTask.getTaskIdx() + 1;
                            final Task task = schedule.getTasks().get(driveTaskIdx);
                            schedule.removeTask(task);
                        }
                    }
                }

                if (stayTask != null && request.getFromLink() == stayTask.getLink()) {
                    // the bus stays where it is
                    beforePickupTask = stayTask;
                } else {// add drive task to pickup location
                    // insert drive i->pickup
                    VrpPathWithTravelData vrpPath = VrpPaths.createPath(stayOrStopTask.getLink(), request.getFromLink(),
                            stayOrStopTask.getEndTime(), detourData.detourToPickup, travelTime);
                    beforePickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
                    schedule.addTask(stayOrStopTask.getTaskIdx() + 1, beforePickupTask);
                }
            }
        }

        // insert pickup stop task
        double startTime = beforePickupTask.getEndTime();
        int taskIdx = beforePickupTask.getTaskIdx() + 1;
		double stopDuration = stopDurationEstimator.calcDuration(vehicleEntry.vehicle, Collections.emptySet(), Collections.singleton(request));
        DrtStopTask pickupStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, startTime,
                Math.max(startTime + stopDuration, request.getEarliestStartTime()), request.getFromLink());
        schedule.addTask(taskIdx, pickupStopTask);
        pickupStopTask.addPickupRequest(request);

        // add drive from pickup
        Link toLink = pickupIdx == dropoffIdx ? request.getToLink() // pickup->dropoff
                : stops.get(pickupIdx).task.getLink(); // pickup->i+1

        VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink, pickupStopTask.getEndTime(),
                detourData.detourFromPickup, travelTime);
        Task driveFromPickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
        schedule.addTask(taskIdx + 1, driveFromPickupTask);

        // update timings
        // TODO should be enough to update the timeline only till dropoffIdx...
        scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2,
                driveFromPickupTask.getEndTime());
        return pickupStopTask;
    }

    private DrtStopTask insertDropoff(AcceptedDrtRequest request, InsertionWithDetourData insertionWithDetourData,
                                      DrtStopTask pickupTask) {
		var insertion = insertionWithDetourData.insertion;
        VehicleEntry vehicleEntry = insertion.vehicleEntry;
        Schedule schedule = vehicleEntry.vehicle.getSchedule();
        List<Waypoint.Stop> stops = vehicleEntry.stops;
        int pickupIdx = insertion.pickup.index;
        int dropoffIdx = insertion.dropoff.index;
		var detourData = insertionWithDetourData.detourData;

        Task driveToDropoffTask;
        if (pickupIdx == dropoffIdx) { // no drive to dropoff
            int pickupTaskIdx = pickupTask.getTaskIdx();
            driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
        } else {
            DrtStopTask stopTask = stops.get(dropoffIdx - 1).task;
            if (request.getToLink() == stopTask.getLink()) { // no detour; no new stop task
                // add dropoff request to stop task
                stopTask.addDropoffRequest(request);
                return stopTask;
            } else { // add drive task to dropoff location

                // remove drive j->j+1 (if j is not the last stop)
                if (dropoffIdx < stops.size()) {
                    DrtStopTask nextStopTask = stops.get(dropoffIdx).task;

                    // this is new
                    if ((nextStopTask instanceof ShiftChangeOverTask)) {
                        if (stopTask.getTaskIdx() + 3 != nextStopTask.getTaskIdx()
                        && !stopTask.getLink().getId().equals(nextStopTask.getLink().getId())) {
                            //include stay task before shift changeover
                            throw new IllegalStateException();
                        }
                    }
                    //end of new

                    else {
                        if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {
                            throw new IllegalStateException();
                        }
                    }
                    int driveTaskIdx = stopTask.getTaskIdx() + 1;

                    final Task task = schedule.getTasks().get(driveTaskIdx);
                    schedule.removeTask(task);
                }

                // insert drive i->dropoff
                VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
                        stopTask.getEndTime(), detourData.detourToDropoff, travelTime);
                driveToDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
                schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
            }
        }

        // insert dropoff stop task
        double startTime = driveToDropoffTask.getEndTime();
        int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		double stopDuration = stopDurationEstimator.calcDuration(vehicleEntry.vehicle, Collections.singleton(request), Collections.emptySet());
        DrtStopTask dropoffStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, startTime,
                startTime + stopDuration, request.getToLink());
        schedule.addTask(taskIdx, dropoffStopTask);
        dropoffStopTask.addDropoffRequest(request);

        // add drive from dropoff
        if (dropoffIdx == stops.size()) {// bus stays at dropoff
            if (taskIdx + 2 == schedule.getTaskCount()) {// remove stay task from the end of schedule,
                DrtStayTask oldStayTask = (DrtStayTask) schedule.getTasks().get(taskIdx + 1);
                schedule.removeTask(oldStayTask);
            }
            if (taskIdx + 1 == schedule.getTaskCount()) {
                // no stay task at the end if the pickup follows the existing stay task
                double beginTime = dropoffStopTask.getEndTime();
                double endTime = Math.max(beginTime, ((ShiftDvrpVehicle) vehicleEntry.vehicle).getShifts().peek().getEndTime());
                schedule.addTask(taskFactory.createStayTask(vehicleEntry.vehicle, beginTime,
                        endTime, dropoffStopTask.getLink()));
            } else {
                throw new RuntimeException();
            }
        } else {
            final DrtStopTask nextStopTask = stops.get(dropoffIdx).task;
            Link toLink = nextStopTask.getLink(); // dropoff->j+1

            VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink, startTime + stopDuration,
                    detourData.detourFromDropoff, travelTime);


            Task driveFromDropoffTask;
            if (nextStopTask instanceof ShiftChangeOverTask) {
                driveFromDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE);
                schedule.addTask(taskIdx + 1, driveFromDropoffTask);
                final List<? extends Task> tasks = vehicleEntry.vehicle.getSchedule().getTasks();
                final Task task = tasks.get(tasks.indexOf(nextStopTask) - 1);
                if (task instanceof DrtStayTask) {
                    schedule.removeTask(task);
                }
                final double arrivalTime = vrpPath.getArrivalTime();
                final double shiftEndTime = ((ShiftChangeOverTask) nextStopTask).getShift().getEndTime();
                if (arrivalTime <= shiftEndTime) {
                    DrtStayTask stayWaitShiftEndTask = taskFactory.createStayTask(vehicleEntry.vehicle, arrivalTime,
                            shiftEndTime, nextStopTask.getLink());
                    schedule.addTask(tasks.indexOf(nextStopTask), stayWaitShiftEndTask);
                }
            } else {
                driveFromDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
                schedule.addTask(taskIdx + 1, driveFromDropoffTask);
            }

            // update timings
            scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2,
                    driveFromDropoffTask.getEndTime());
        }
        return dropoffStopTask;
    }
}
