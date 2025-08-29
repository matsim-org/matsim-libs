package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftTaskSchedulerImpl implements ShiftTaskScheduler {

    private final static Logger logger = LogManager.getLogger(ShiftTaskSchedulerImpl.class);

    private final TravelTime travelTime;
    private final MobsimTimer timer;
    private final ShiftDrtTaskFactory taskFactory;
    private final LeastCostPathCalculator router;

    private final ShiftsParams shiftsParams;

    private final Network network;

    public ShiftTaskSchedulerImpl(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
                                  MobsimTimer timer, ShiftDrtTaskFactory taskFactory, ShiftsParams shiftsParams) {
        this.travelTime = travelTime;
        this.timer = timer;
        this.taskFactory = taskFactory;
        this.network = network;
        this.shiftsParams = shiftsParams;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }

    @Override
    public void relocateForBreak(ShiftDvrpVehicle vehicle, OperationFacility breakFacility, DrtShift shift) {
        final Schedule schedule = vehicle.getSchedule();
        final Task currentTask = schedule.getCurrentTask();
        final Link toLink = network.getLinks().get(breakFacility.getLinkId());
        if (currentTask instanceof DriveTask
                && currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
                && currentTask.equals(schedule.getTasks().get(schedule.getTaskCount() - 2))) {
            // Try to divert/cancel relocation
            LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            VrpPathWithTravelData path;
            if (start != null) {
                path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router, travelTime);
                ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).divertPath(path);
                // Remove STAY task
                schedule.removeLastTask();
            } else {
                start = new LinkTimePair(((DriveTask) currentTask).getPath().getToLink(), currentTask.getEndTime());
                path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router, travelTime);
                schedule.removeLastTask();
                schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE));
            }
            double startTime = path.getArrivalTime();
            double endTime = startTime + shift.getBreak().orElseThrow().getDuration();
            double latestDetourArrival = path.getDepartureTime() + path.getTravelTime();
            relocateForBreakImpl(vehicle, startTime, endTime, latestDetourArrival, toLink, shift, breakFacility);
        } else {
            final Task task = schedule.getTasks().get(schedule.getTaskCount() - 1);
            final Link lastLink = ((StayTask) task).getLink();
            if (!lastLink.getId().equals(breakFacility.getLinkId())) {
                double departureTime = task.getBeginTime();
                if (schedule.getCurrentTask() == task) {
                    departureTime = Math.max(task.getBeginTime(), timer.getTimeOfDay());
                }
                VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink, toLink, departureTime, router, travelTime);
                if (path.getArrivalTime() < vehicle.getServiceEndTime()) {
                    if (schedule.getCurrentTask() == task) {
                        task.setEndTime(timer.getTimeOfDay());
                    } else {
                        schedule.removeLastTask();
                    }
                    schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE));
                    double startTime = path.getArrivalTime();
                    double endTime = startTime + shift.getBreak().orElseThrow().getDuration();
                    double latestDetourArrival = path.getDepartureTime() + path.getTravelTime();
                    relocateForBreakImpl(vehicle, startTime, endTime, latestDetourArrival, toLink, shift, breakFacility);
                }
            } else {
                double startTime;
                if (schedule.getCurrentTask() == task) {
                    task.setEndTime(timer.getTimeOfDay());
                    startTime = timer.getTimeOfDay();
                } else {
                    startTime = task.getBeginTime();
                    schedule.removeLastTask();
                }
                double endTime = startTime + shift.getBreak().orElseThrow().getDuration();
                double latestDetourArrival = timer.getTimeOfDay();
                relocateForBreakImpl(vehicle, startTime, endTime, latestDetourArrival, toLink, shift, breakFacility);
            }
        }
    }

    private void relocateForBreakImpl(ShiftDvrpVehicle vehicle, double startTime, double endTime,
                                      double latestDetourArrival, Link link, DrtShift shift,
                                      OperationFacility breakFacility) {
        Schedule schedule = vehicle.getSchedule();
        DrtShiftBreak shiftBreak = shift.getBreak().orElseThrow();
        ShiftBreakTask shiftBreakTask = createShiftBreakTask(vehicle, startTime, endTime, link, shiftBreak, breakFacility);
        schedule.addTask(shiftBreakTask);
        schedule.addTask(taskFactory.createStayTask(vehicle, endTime, shift.getEndTime(), link));
        final double latestTimeConstraintArrival = shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration();
        shiftBreak.schedule(Math.min(latestDetourArrival, latestTimeConstraintArrival));
    }

    protected ShiftBreakTask createShiftBreakTask(ShiftDvrpVehicle vehicle, double startTime, double endTime,
                                                  Link link, DrtShiftBreak shiftBreak, OperationFacility breakFacility) {
        return taskFactory.createShiftBreakTask(vehicle, startTime, endTime, link, shiftBreak, breakFacility);
    }

    @Override
    public void relocateForShiftChange(DvrpVehicle vehicle, Link link, DrtShift shift, OperationFacility breakFacility) {
        final Schedule schedule = vehicle.getSchedule();
        final Task currentTask = schedule.getCurrentTask();
        if (currentTask instanceof DriveTask
                && currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
                && currentTask.equals(schedule.getTasks().get(schedule.getTaskCount() - 2))) {
            // Try to divert/cancel relocation
            LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            VrpPathWithTravelData path;
            if (start != null) {
                path = VrpPaths.calcAndCreatePath(start.link, link, start.time, router, travelTime);
                ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).divertPath(path);
                schedule.removeLastTask();
            } else {
                start = new LinkTimePair(((DriveTask) currentTask).getPath().getToLink(), currentTask.getEndTime());
                path = VrpPaths.calcAndCreatePath(start.link, link, start.time, router, travelTime);
                schedule.removeLastTask();
                schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE));
            }
            final double startTime = Math.max(shift.getEndTime(), path.getArrivalTime());
            final double endTime = startTime + shiftsParams.getChangeoverDuration();
            if (path.getArrivalTime() > shift.getEndTime()) {
                logger.warn("Shift changeover of shift " + shift.getId() + " will probably be delayed by "
                        + (path.getArrivalTime() - shift.getEndTime()) + " seconds.");
            }
            schedule.addTask(taskFactory.createStayTask(vehicle, path.getArrivalTime(), startTime, path.getToLink()));
            appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
        } else {
            final List<? extends Task> tasks = schedule.getTasks();
            final DrtStayTask drtStayTask = (DrtStayTask) tasks.get(tasks.size() - 1);
            Link currentLink = drtStayTask.getLink();
            if (!currentLink.equals(link)) {
                double departureTime = drtStayTask.getBeginTime();
                if (drtStayTask == schedule.getCurrentTask()) {
                    departureTime = Math.max(departureTime, timer.getTimeOfDay());
                }
                VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, link, departureTime, router, travelTime);
                if (schedule.getCurrentTask() == drtStayTask) {
                    drtStayTask.setEndTime(timer.getTimeOfDay());
                } else {
                    schedule.removeLastTask();
                }
                if (path.getArrivalTime() < shift.getEndTime()) {
                    double slack = shift.getEndTime() - path.getArrivalTime();
                    if (shiftsParams.getShiftEndRelocationArrival() == ShiftsParams.ShiftEndRelocationArrival.justInTime) {
                        DrtStayTask waitTask = taskFactory.createStayTask(vehicle, departureTime, departureTime + slack, currentLink);
                        schedule.addTask(waitTask);
                        path = path.withDepartureTime(departureTime + slack);
                    }
                }
                DrtDriveTask driveTask = taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE);
                schedule.addTask(driveTask);
                final double startTime = driveTask.getEndTime();
                final double endTime = Math.min(startTime + shiftsParams.getChangeoverDuration(), vehicle.getServiceEndTime());
                if (path.getArrivalTime() > shift.getEndTime()) {
                    logger.warn("Shift changeover of shift " + shift.getId() + " will probably be delayed by "
                            + (path.getArrivalTime() - shift.getEndTime()) + " seconds.");
                }
                if (shiftsParams.getShiftEndRelocationArrival() != ShiftsParams.ShiftEndRelocationArrival.justInTime) {
                    schedule.addTask(taskFactory.createStayTask(vehicle, path.getArrivalTime(), startTime, path.getToLink()));
                }
                appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
            } else {
                drtStayTask.setEndTime(shift.getEndTime());
                final double startTime = shift.getEndTime();
                final double endTime = Math.min(shift.getEndTime() + shiftsParams.getChangeoverDuration(), vehicle.getServiceEndTime());
                appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
            }
        }
    }

    private void appendShiftChange(DvrpVehicle vehicle, DrtShift shift, OperationFacility breakFacility,
                                   double startTime, double endTime, Link link) {
        Schedule schedule = vehicle.getSchedule();
        ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle, startTime, endTime, link, shift, breakFacility);
        schedule.addTask(changeTask);
        if (endTime < vehicle.getServiceEndTime()) {
            schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(), link, breakFacility));
        }
    }

    @Override
    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof WaitForShiftTask) {
            ((WaitForShiftTask) stayTask).getFacility().deregisterVehicle(vehicle.getId());
            stayTask.setEndTime(now);
            if (Schedules.getLastTask(schedule).equals(stayTask)) {
                // Nothing planned yet
                schedule.addTask(taskFactory.createStayTask(vehicle, now, shift.getEndTime(), stayTask.getLink()));
            } else {
                Schedules.getNextTask(schedule).setBeginTime(now);
            }
        } else {
            throw new IllegalStateException("Vehicle cannot start shift during task:" + stayTask.getTaskType().name());
        }
    }

    @Override
    public boolean updateShiftChange(ShiftDvrpVehicle vehicle, Link link, DrtShift shift,
                                     LinkTimePair start, OperationFacility facility, Task lastTask) {
        if (!start.link.equals(link)) {
            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(start.link, link,
                    Math.max(start.time, timer.getTimeOfDay()), router, travelTime);
            updateShiftChangeImpl(vehicle, path, shift, facility, lastTask);
            return true;
        }
        return false;
    }

    private void updateShiftChangeImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath,
                                       DrtShift shift, OperationFacility facility, Task lastTask) {
        Schedule schedule = vehicle.getSchedule();
        List<Task> copy = new ArrayList<>(schedule.getTasks().subList(lastTask.getTaskIdx() + 1, schedule.getTasks().size()));
        for (Task task : copy) {
            schedule.removeTask(task);
        }
        if (DrtTaskBaseType.getBaseTypeOrElseThrow(lastTask).equals(DRIVE)) {
            ((OnlineDriveTaskTracker) lastTask.getTaskTracker()).divertPath(vrpPath);
        } else {
            lastTask.setEndTime(vrpPath.getDepartureTime());
            schedule.addTask(taskFactory.createDriveTask(vehicle, vrpPath, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE));
        }
        if (vrpPath.getArrivalTime() < shift.getEndTime()) {
            schedule.addTask(taskFactory.createStayTask(vehicle, vrpPath.getArrivalTime(), shift.getEndTime(), vrpPath.getToLink()));
        }
        final double endTime = Math.max(shift.getEndTime(), vrpPath.getArrivalTime()) + shiftsParams.getChangeoverDuration();
        ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle,
                Math.max(shift.getEndTime(), vrpPath.getArrivalTime()), endTime, vrpPath.getToLink(), shift, facility);
        schedule.addTask(changeTask);
        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), facility));
    }

    @Override
    public void planAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof WaitForShiftTask) {
            // set +1 to ensure this update happens after next shift start check
            stayTask.setEndTime(Math.max(timeStep + 1, shift.getStartTime()));
            //append stay task if required
            if (Schedules.getLastTask(schedule).equals(stayTask)) {
                schedule.addTask(taskFactory.createStayTask(vehicle, stayTask.getEndTime(), shift.getEndTime(), stayTask.getLink()));
            }
        }
    }

    @Override
    public void cancelAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof WaitForShiftTask) {
            stayTask.setEndTime(vehicle.getServiceEndTime());
        }
    }
}
