package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftTaskSchedulerImpl implements ShiftTaskScheduler {

    private final TravelTime travelTime;
    private final MobsimTimer timer;
    private final ShiftDrtTaskFactory taskFactory;
    private final LeastCostPathCalculator router;
    private final OperationFacilities facilities;

    private final ShiftsParams shiftsParams;

    public ShiftTaskSchedulerImpl(Network network, OperationFacilities operationFacilities, TravelTime travelTime,
                                  TravelDisutility travelDisutility, MobsimTimer timer,
                                  ShiftDrtTaskFactory taskFactory, ShiftsParams shiftsParams) {
        this.travelTime = travelTime;
        this.timer = timer;
        this.taskFactory = taskFactory;
        this.facilities = operationFacilities;
        this.shiftsParams = shiftsParams;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }


    @Override
    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        Task currentTask = schedule.getCurrentTask();

        if (currentTask instanceof WaitForShiftTask waitForShiftTask) {
            if (Schedules.getLastTask(schedule).equals(currentTask)) {

                OperationFacility.Registration facilityRegistration = waitForShiftTask.getFacilityRegistration();
                OperationFacility operationFacility = facilities.getFacilities().get(facilityRegistration.operationFacilityId());
                boolean deregistered = operationFacility.deregisterVehicle(facilityRegistration.registrationId());
                waitForShiftTask.setEndTime(Math.max(now, shift.getStartTime()));

                double initialStayEndTime = shift.getEndTime();
                Optional<DrtShiftBreak> shiftBreak = shift.getBreak();
                if(shiftBreak.isPresent()) {
                    initialStayEndTime = shiftBreak.get().getEarliestBreakStartTime();
                    Gbl.assertIf(initialStayEndTime > now);
                }

                schedule.addTask(taskFactory.createStayTask(vehicle, now, initialStayEndTime, waitForShiftTask.getLink()));

                if(shiftBreak.isPresent()) {
                    Optional<OperationFacility.Registration> breakRegistration = operationFacility.registerVehicle(vehicle.getId(),
                            shiftBreak.get().getEarliestBreakStartTime(), shiftBreak.get().getLatestBreakEndTime());
                    double breakEndTime = shiftBreak.get().getEarliestBreakStartTime() + shiftBreak.get().getDuration();
                    if(breakRegistration.isPresent()) {
                        schedule.addTask(taskFactory.createShiftBreakTask(vehicle, initialStayEndTime,
                                breakEndTime, waitForShiftTask.getLink(), shiftBreak.get(), breakRegistration.get()));
                    } else {
                        throw new RuntimeException("Could not schedule shift break for " + shift + " at facility " + operationFacility);
                    }

                    schedule.addTask(taskFactory.createStayTask(vehicle, breakEndTime, shift.getEndTime(), waitForShiftTask.getLink()));
                }

                double changeoverEnd = shift.getEndTime() + shiftsParams.getChangeoverDuration();
                Optional<OperationFacility.Registration> changeoverReg = operationFacility.registerVehicle(
                        vehicle.getId(),
                        shift.getEndTime(),
                        changeoverEnd
                );
                Optional<OperationFacility.Registration> waitForShiftReg = operationFacility.registerVehicle(vehicle.getId(), changeoverEnd);
                if(changeoverReg.isPresent() && waitForShiftReg.isPresent()) {
                    ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle, shift.getEndTime(),
                            changeoverEnd, waitForShiftTask.getLink(), shift, changeoverReg.get());
                    schedule.addTask(changeTask);
                    if (changeTask.getEndTime() < vehicle.getServiceEndTime()) {
                        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, changeTask.getEndTime(),
                                vehicle.getServiceEndTime(), waitForShiftTask.getLink(), waitForShiftReg.get()));
                    }
                } else {
                    throw new RuntimeException("Could not schedule shift end.");
                }
                    //eventsManager.processEvent(new OperationFacilityRegistrationEvent(timer.getTimeOfDay(), mode, vehicle.getId(), facility.getId()));

            } else {
                throw new IllegalStateException("Vehicle cannot start shift due to existing tasks.");
            }
        } else {
            throw new IllegalStateException("Vehicle cannot start shift during task:" + currentTask.getTaskType().name());
        }
    }

    @Override
    public boolean updateShiftChange(ShiftDvrpVehicle vehicle, Link link, DrtShift shift,
                                     LinkTimePair start, OperationFacility.Registration facilityRegistration, Task lastTask) {
        if (!start.link.equals(link)) {
            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(start.link, link,
                    Math.max(start.time, timer.getTimeOfDay()), router, travelTime);
            updateShiftChangeImpl(vehicle, path, shift, facilityRegistration, lastTask);
            return true;
        }
        return false;
    }

    private void updateShiftChangeImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath,
                                       DrtShift shift, OperationFacility.Registration facilityRegistration, Task lastTask) {
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
        ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle, Math.max(shift.getEndTime(),
                vrpPath.getArrivalTime()), endTime, vrpPath.getToLink(), shift, facilityRegistration);
        schedule.addTask(changeTask);
        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), facilityRegistration));
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
