package org.matsim.contrib.drt.extension.operations.eshifts.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtShiftChangeoverTaskImpl;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtWaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.ShiftEDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftSchedules;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskSchedulerImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author nkuehnel / MOIA
 */
public final class EShiftTaskScheduler implements ShiftTaskScheduler {

    private final static Logger logger = LogManager.getLogger(EShiftTaskScheduler.class);

    private final ShiftTaskSchedulerImpl delegate;

    private final ShiftDrtTaskFactory taskFactory;


    public EShiftTaskScheduler(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
                               MobsimTimer timer, ShiftDrtTaskFactory taskFactory, ShiftsParams shiftsParams) {
        this.delegate = new ShiftTaskSchedulerImpl(network, travelTime, travelDisutility, timer, taskFactory, shiftsParams);
        this.taskFactory = taskFactory;
    }

    @Override
    public boolean updateShiftChange(ShiftDvrpVehicle vehicle, Link link, DrtShift shift,
                                     LinkTimePair start, OperationFacility facility, Task lastTask) {
        // Check if a previous EV-specific changeover task exists and unassign the vehicle.
        Optional<ShiftChangeOverTask> oldChangeOver = ShiftSchedules.getNextShiftChangeover(vehicle.getSchedule());
        if (oldChangeOver.isPresent() && oldChangeOver.get() instanceof EDrtShiftChangeoverTaskImpl evTask) {
            if (evTask.getChargingTask() != null) {
                EvDvrpVehicle evVehicle = (EvDvrpVehicle) vehicle;
                evTask.getChargingTask().getChargingLogic().unassignVehicle(evVehicle.getElectricVehicle());
            }
        }
        return delegate.updateShiftChange(vehicle, link, shift, start, facility, lastTask);
    }


    @Override
    public void planAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof EDrtWaitForShiftTask eTask) {
            if (eTask.getChargingTask() != null) {
                Task nextTask = Schedules.getNextTask(schedule);
                if (nextTask instanceof WaitForShiftTask) {
                    nextTask.setEndTime(Math.max(timeStep + 1, shift.getStartTime()));
                    if (Schedules.getLastTask(schedule).equals(nextTask)) {
                        schedule.addTask(taskFactory.createStayTask(vehicle, nextTask.getEndTime(), shift.getEndTime(),
                                ((WaitForShiftTask) nextTask).getLink()));
                    }
                } else {
                    throw new RuntimeException("Unexpected task type after charging task.");
                }
                return;
            }
        }
        // Fallback to default behavior if not an EV-specific wait task.
        delegate.planAssignedShift(vehicle, timeStep, shift);
    }


    @Override
    public void cancelAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof WaitForShiftTask) {
            if (stayTask instanceof EDrtWaitForShiftTask eTask) {
                if (eTask.getChargingTask() != null) {
                    Task nextTask = Schedules.getNextTask(schedule);
                    if (nextTask instanceof WaitForShiftTask) {
                        nextTask.setEndTime(vehicle.getServiceEndTime());
                    } else {
                        throw new RuntimeException("Unexpected task type after charging task.");
                    }
                } else {
                    stayTask.setEndTime(vehicle.getServiceEndTime());
                }
            }
        } else {
            throw new IllegalStateException("Vehicle should be in WaitForShiftTask");
        }
    }

    @Override
    public void relocateForBreak(ShiftDvrpVehicle vehicle, OperationFacility breakFacility, DrtShift shift) {
        delegate.relocateForBreak(vehicle, breakFacility, shift);
    }

    @Override
    public void relocateForShiftChange(DvrpVehicle vehicle, Link link, DrtShift shift, OperationFacility breakFacility) {
        delegate.relocateForShiftChange(vehicle, link, shift, breakFacility);
    }

    @Override
    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        StayTask stayTask = (StayTask) schedule.getCurrentTask();
        if (stayTask instanceof WaitForShiftTask) {
            ((WaitForShiftTask) stayTask).getFacility().deregisterVehicle(vehicle.getId());
            stayTask.setEndTime(now);
            if (stayTask instanceof EDrtWaitForShiftTask eTask) {
                if (eTask.getChargingTask() != null) {
                    ChargingWithAssignmentLogic chargingLogic = eTask.getChargingTask().getChargingLogic();
                    ElectricVehicle ev = ((EvShiftDvrpVehicle) vehicle).getElectricVehicle();
                    if(Stream.concat(chargingLogic.getPluggedVehicles().stream(), chargingLogic.getQueuedVehicles().stream())
                            .map(ChargingLogic.ChargingVehicle::ev)
                            .anyMatch(chargerEv -> chargerEv.getId().equals(ev.getId()))) {
                        chargingLogic.removeVehicle(ev, now);
                        eTask.setEndTime(now);
                    } else {
                        logger.warn("Vehicle {} had active charging task when trying to start shift {}. " +
                                "However, it was neither queued nor plugged in the logic (anymore). This may happen if the logic " +
                                "decided to end charging in the second before the shift start. ", vehicle.getId(), shift
                                .getId());
                    }
                }
            }
            if (Schedules.getLastTask(schedule).equals(stayTask)) {
                //nothing planned yet.
                schedule.addTask(taskFactory.createStayTask(vehicle, now, shift.getEndTime(), stayTask.getLink()));
            } else {
                Schedules.getNextTask(schedule).setBeginTime(now);
            }
        } else {
            throw new IllegalStateException("Vehicle cannot start shift during task:" + stayTask.getTaskType().name());
        }
    }

    public void chargeAtHub(WaitForShiftTask currentTask, ShiftDvrpVehicle vehicle,
                            ElectricVehicle electricVehicle, Charger charger, double beginTime,
                            double endTime, double energy, ChargingStrategy strategy) {
        final double initialEndTime = currentTask.getEndTime();
        currentTask.setEndTime(beginTime);
        ((ChargingWithAssignmentLogic) charger.getLogic()).assignVehicle(electricVehicle, strategy);
        final WaitForShiftTask chargingWaitForShiftTask = ((ShiftEDrtTaskFactoryImpl) taskFactory)
                .createChargingWaitForShiftStayTask(vehicle, beginTime, endTime, currentTask.getLink(),
                        currentTask.getFacility(), energy, charger, strategy);
        final WaitForShiftTask waitForShiftTask = taskFactory.createWaitForShiftStayTask(vehicle, endTime,
                initialEndTime, currentTask.getLink(), currentTask.getFacility());
        vehicle.getSchedule().addTask(currentTask.getTaskIdx() + 1, chargingWaitForShiftTask);
        vehicle.getSchedule().addTask(currentTask.getTaskIdx() + 2, waitForShiftTask);
    }
}
