package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.core.gbl.Gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftTaskSchedulerImpl implements ShiftTaskScheduler {

    private final ShiftDrtTaskFactory taskFactory;
    private final OperationFacilities facilities;
    private final OperationFacilityReservationManager facilityReservationManager;

    private final ShiftsParams shiftsParams;

    public ShiftTaskSchedulerImpl(OperationFacilities operationFacilities,
                                  ShiftDrtTaskFactory taskFactory,
                                  OperationFacilityReservationManager facilityReservationManager,
                                  ShiftsParams shiftsParams) {
        this.taskFactory = taskFactory;
        this.facilities = operationFacilities;
        this.facilityReservationManager = facilityReservationManager;
        this.shiftsParams = shiftsParams;
    }


    @Override
    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        Task currentTask = schedule.getCurrentTask();

        if (currentTask instanceof WaitForShiftTask waitForShiftTask) {
            if (Schedules.getLastTask(schedule).equals(currentTask)) {
                waitForShiftTask.setEndTime(Math.max(now, shift.getStartTime()));
                Optional<Id<ReservationManager.Reservation>> reservationId = waitForShiftTask.getReservationId();
                reservationId.ifPresent(id -> facilityReservationManager.updateReservation(waitForShiftTask.getFacilityId(), id, now, shift.getStartTime()));

                double initialStayEndTime = shift.getEndTime();
                Optional<DrtShiftBreak> shiftBreak = shift.getBreak();
                if (shiftBreak.isPresent()) {
                    initialStayEndTime = shiftBreak.get().getEarliestBreakStartTime();
                    Gbl.assertIf(initialStayEndTime > now);
                }

                schedule.addTask(taskFactory.createStayTask(vehicle, now, initialStayEndTime, waitForShiftTask.getLink()));

                OperationFacility operationFacility = facilities.getFacilities().get(waitForShiftTask.getFacilityId());
                if (shiftBreak.isPresent()) {
                    Optional<ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>> reservation =
                            facilityReservationManager.addReservation(operationFacility,
                                    vehicle, shiftBreak.get().getEarliestBreakStartTime(),
                                    shiftBreak.get().getLatestBreakEndTime());
                    double breakEndTime = shiftBreak.get().getEarliestBreakStartTime() + shiftBreak.get().getDuration();
                    if (reservation.isPresent()) {
                        schedule.addTask(
                                taskFactory.createShiftBreakTask(vehicle, initialStayEndTime,
                                        breakEndTime, waitForShiftTask.getLink(), shiftBreak.get(),
                                        operationFacility.getId(), reservation.get().reservationId()));
                    } else {
                        throw new RuntimeException("Could not schedule shift break for " + shift + " at facility " + operationFacility);
                    }

                    schedule.addTask(taskFactory.createStayTask(vehicle, breakEndTime, shift.getEndTime(), waitForShiftTask.getLink()));
                }

                double changeoverEnd = shift.getEndTime() + shiftsParams.getChangeoverDuration();
                Optional<ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>> changeoverReg = facilityReservationManager.addReservation(
                        operationFacility,
                        vehicle,
                        shift.getEndTime(),
                        vehicle.getServiceEndTime()
                );
                if (changeoverReg.isPresent()) {
                    ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle, shift.getEndTime(),
                            changeoverEnd, waitForShiftTask.getLink(), shift, operationFacility.getId(), changeoverReg.get().reservationId());
                    schedule.addTask(changeTask);
                    if (changeTask.getEndTime() < vehicle.getServiceEndTime()) {
                        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, changeTask.getEndTime(),
                                vehicle.getServiceEndTime(), waitForShiftTask.getLink(),
                                operationFacility.getId(), changeoverReg.get().reservationId()));

                    }
                } else {
                    throw new RuntimeException("Could not schedule shift end.");
                }
            } else {
                throw new IllegalStateException("Vehicle cannot start shift due to existing tasks.");
            }
        } else {
            throw new IllegalStateException("Vehicle cannot start shift during task:" + currentTask.getTaskType().name());
        }
    }

    @Override
    public boolean updateShiftChange(ShiftDvrpVehicle vehicle, VrpPathWithTravelData vrpPath, DrtShift shift,
                                     ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle> reservation,
                                     Task lastTask) {
            updateShiftChangeImpl(vehicle, vrpPath, shift, reservation, lastTask);
            return true;
    }

    private void updateShiftChangeImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath,
                                       DrtShift shift, ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle> reservation,
                                       Task lastTask) {
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
                vrpPath.getArrivalTime()), endTime, vrpPath.getToLink(), shift, reservation.resource().getId(), reservation.reservationId());
        schedule.addTask(changeTask);
        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), reservation.resource().getId(), reservation.reservationId()));
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
