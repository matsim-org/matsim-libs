/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import com.google.common.base.Verify;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityType;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.events.*;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftSchedules;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.*;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DrtShiftDispatcherImpl implements DrtShiftDispatcher {

    private final static Logger logger = LogManager.getLogger(DrtShiftDispatcherImpl.class);

    private final String mode;

    private SortedSet<DrtShift> unAssignedShifts;
    private SortedSet<ShiftEntry> assignedShifts;
    private SortedSet<ShiftEntry> activeShifts;
    private SortedSet<ShiftEntry> endingShifts;

    private Map<Id<OperationFacility>, Queue<ShiftDvrpVehicle>> idleVehiclesQueues;

    private final Fleet fleet;

    private final MobsimTimer timer;

    private final OperationFacilities operationFacilities;
    private final OperationFacilityFinder breakFacilityFinder;
    private final ShiftTaskScheduler shiftTaskScheduler;

    private final Network network;

    private final EventsManager eventsManager;

    private final ShiftsParams drtShiftParams;

    private final ShiftStartLogic shiftStartLogic;
    private final AssignShiftToVehicleLogic assignShiftToVehicleLogic;

    private final ShiftScheduler shiftScheduler;
    private final ShiftBreakManager breakManager;


    public DrtShiftDispatcherImpl(String mode, Fleet fleet, MobsimTimer timer, OperationFacilities operationFacilities,
                                  OperationFacilityFinder breakFacilityFinder, ShiftTaskScheduler shiftTaskScheduler,
                                  Network network, EventsManager eventsManager, ShiftsParams drtShiftParams,
                                  ShiftStartLogic shiftStartLogic, AssignShiftToVehicleLogic assignShiftToVehicleLogic,
                                  ShiftScheduler shiftScheduler, ShiftBreakManager breakManager) {
        this.mode = mode;
        this.fleet = fleet;
        this.timer = timer;
        this.operationFacilities = operationFacilities;
        this.breakFacilityFinder = breakFacilityFinder;
        this.shiftTaskScheduler = shiftTaskScheduler;
        this.network = network;
        this.eventsManager = eventsManager;
        this.drtShiftParams = drtShiftParams;
        this.shiftStartLogic = shiftStartLogic;
        this.assignShiftToVehicleLogic = assignShiftToVehicleLogic;
        this.shiftScheduler = shiftScheduler;
        this.breakManager = breakManager;
    }

    @Override
    public void initialize() {

        unAssignedShifts = new TreeSet<>(Comparator.comparingDouble(DrtShift::getStartTime).thenComparing(Comparator.naturalOrder()));
        unAssignedShifts.addAll(shiftScheduler.initialSchedule().values());

        assignedShifts = new TreeSet<>(Comparator
                .comparingDouble((ShiftEntry entry) -> entry.shift().getStartTime())
                .thenComparing(ShiftEntry::shift));

        activeShifts = new TreeSet<>(Comparator
                .comparingDouble((ShiftEntry entry) -> entry.shift().getEndTime())
                .thenComparing(ShiftEntry::shift));
        endingShifts = new TreeSet<>(Comparator
                .comparingDouble((ShiftEntry entry) -> entry.shift().getEndTime())
                .thenComparing(ShiftEntry::shift));

        idleVehiclesQueues = new LinkedHashMap<>();
        for(OperationFacility facility: operationFacilities.getDrtOperationFacilities().values()) {
            PriorityQueue<ShiftDvrpVehicle> queue = new PriorityQueue<>((v1, v2) -> String.CASE_INSENSITIVE_ORDER.compare(v1.getId().toString(), v2.getId().toString()));
            Set<Id<DvrpVehicle>> registeredVehicles = facility.getRegisteredVehicles();
            for (Id<DvrpVehicle> registeredVehicle : registeredVehicles) {
                queue.add((ShiftDvrpVehicle) fleet.getVehicles().get(registeredVehicle));
            }
            idleVehiclesQueues.put(
                    facility.getId(),
                    queue
            );
        }
    }

    @Override
    public void dispatch(double now) {
        if(now % drtShiftParams.getLoggingInterval() == 0) {
            logger.info(String.format("Active shifts: %s | Assigned shifts: %s | Unscheduled shifts: %s",
                    activeShifts.size(), assignedShifts.size(), unAssignedShifts.size()));
            StringJoiner print = new StringJoiner(" | ");
            for (Map.Entry<Id<OperationFacility>, Queue<ShiftDvrpVehicle>> queueEntry : idleVehiclesQueues.entrySet()) {
                print.add(String.format("Idle vehicles at facility %s: %d", queueEntry.getKey().toString(), queueEntry.getValue().size()));
            }
            logger.info(print.toString());
        }
        endShifts(now);
        if (now % (drtShiftParams.getUpdateShiftEndInterval()) == 0) {
            updateShiftEnds(now);
        }
        scheduleShifts(now, this.fleet);
        assignShifts(now);
        startShifts(now);
        breakManager.checkAndScheduleBreaks(activeShifts);
    }


    private void scheduleShifts(double now, Fleet fleet) {
        List<DrtShift> scheduled = shiftScheduler.schedule(now, fleet);
        unAssignedShifts.addAll(scheduled);
    }

    private void startShifts(double now) {
        final Iterator<ShiftEntry> iterator = this.assignedShifts.iterator();
        while (iterator.hasNext()) {
            final ShiftEntry assignedShiftEntry = iterator.next();
            if (assignedShiftEntry.shift().getStartTime() > now) {
                shiftTaskScheduler.planAssignedShift(assignedShiftEntry.vehicle(), now, assignedShiftEntry.shift());
                continue;
            } else if (assignedShiftEntry.shift().getEndTime() < now) {
                logger.warn("Too late to start shift " + assignedShiftEntry.shift().getId());
                shiftTaskScheduler.cancelAssignedShift(assignedShiftEntry.vehicle(), now, assignedShiftEntry.shift());
                assignedShiftEntry.vehicle().getShifts().remove(assignedShiftEntry.shift());
                iterator.remove();
                continue;
            }

            if (shiftStartLogic.shiftStarts(assignedShiftEntry)) {
                assignedShiftEntry.shift().start();
                shiftTaskScheduler.startShift(assignedShiftEntry.vehicle(), now, assignedShiftEntry.shift());
                activeShifts.add(assignedShiftEntry);
                iterator.remove();
                logger.debug("Started shift " + assignedShiftEntry.shift());
                StayTask currentTask = (StayTask) assignedShiftEntry.vehicle().getSchedule().getCurrentTask();
                eventsManager.processEvent(new DrtShiftStartedEvent(now, mode,
                        assignedShiftEntry.shift().getId(), assignedShiftEntry.vehicle().getId(),
                        currentTask.getLink().getId(), assignedShiftEntry.shift().getShiftType().orElse(null)));
            } else {
                shiftTaskScheduler.planAssignedShift(assignedShiftEntry.vehicle(), now, assignedShiftEntry.shift());
            }
        }
    }

    private void assignShifts(double now) {
        removeExpiredShifts(now);

        List<DrtShift> assignableShifts = collectAssignableShifts(now);
        for (DrtShift shift : assignableShifts) {
            if (shift.getDesignatedVehicleId().isPresent()) {
                assignDesignatedOrRequeue(shift);
            } else {
                assignFallbackOrRequeue(shift);
            }
        }
    }

    private void removeExpiredShifts(double timeStep) {
        unAssignedShifts.removeIf(shift -> {
            boolean expired = shift.getStartTime() + drtShiftParams.getMaxUnscheduledShiftDelay() < timeStep;
            if (expired) {
                logger.warn("Shift with ID {} could not be assigned and is being removed as start time is longer in the past than defined by maxUnscheduledShiftDelay.",
                        shift.getId());
            }
            return expired;
        });
    }

    private List<DrtShift> collectAssignableShifts(double timeStep) {
        List<DrtShift> assignable = new ArrayList<>();
        Iterator<DrtShift> it = unAssignedShifts.iterator();
        while (it.hasNext()) {
            DrtShift shift = it.next();
            if (isSchedulable(shift, timeStep)) {
                assignable.add(shift);
                it.remove();
            } else {
                break;
            }
        }
        return assignable;
    }

    private void assignDesignatedOrRequeue(DrtShift shift) {
        Optional<ShiftDvrpVehicle> vehicleOpt = findDesignatedVehicle(shift);
        if (vehicleOpt.isPresent()) {
            assignShiftToVehicle(shift, vehicleOpt.get());
        } else {
            unAssignedShifts.add(shift);
        }
    }

    private void assignFallbackOrRequeue(DrtShift shift) {
        Optional<ShiftDvrpVehicle> vehicleOpt = findReuseVehicle(shift)
                .or(() -> findIdleVehicle(shift));

        if (vehicleOpt.isPresent()) {
            assignShiftToVehicle(shift, vehicleOpt.get());
        } else {
            unAssignedShifts.add(shift);
        }
    }

    private Optional<ShiftDvrpVehicle> findDesignatedVehicle(DrtShift shift) {
        Id<DvrpVehicle> vid = shift.getDesignatedVehicleId().get();
        DvrpVehicle dv = fleet.getVehicles().get(vid);
        Verify.verify(dv.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED);
        Verify.verify(dv instanceof ShiftDvrpVehicle);
        ShiftDvrpVehicle vehicle = (ShiftDvrpVehicle) dv;
        if (!vehicle.getShifts().isEmpty()) {
            return Optional.empty();
        }
        shift.getOperationFacilityId().ifPresent(fid ->
                Verify.verify(idleVehiclesQueues.get(fid).contains(vehicle))
        );
        return Optional.of(vehicle);
    }

    private Optional<ShiftDvrpVehicle> findReuseVehicle(DrtShift shift) {
        for (ShiftEntry active : activeShifts) {
            if (active.shift().getEndTime() > shift.getStartTime()) {
                break;
            }
            if (shift.getOperationFacilityId().isPresent()) {
                Id<OperationFacility> target = shift.getOperationFacilityId().get();
                if (active.shift().getOperationFacilityId().isPresent()) {
                    if (!active.shift().getOperationFacilityId().get().equals(target)) {
                        continue;
                    }
                } else {
                    Optional<ShiftChangeOverTask> co = ShiftSchedules.getNextShiftChangeover(
                            active.vehicle().getSchedule());
                    if (co.isPresent() && !co.get().getFacility().getId().equals(target)) {
                        continue;
                    }
                }
            }
            if (assignShiftToVehicleLogic.canAssignVehicleToShift(active.vehicle(), shift)) {
                return Optional.of(active.vehicle());
            }
        }
        return Optional.empty();
    }

    private Optional<ShiftDvrpVehicle> findIdleVehicle(DrtShift shift) {
        Iterator<ShiftDvrpVehicle> it;
        if (shift.getOperationFacilityId().isPresent()) {
            it = idleVehiclesQueues.get(shift.getOperationFacilityId().get()).iterator();
        } else {
            IteratorChain<ShiftDvrpVehicle> chain = new IteratorChain<>();
            for (Queue<ShiftDvrpVehicle> q : idleVehiclesQueues.values()) {
                chain.addIterator(q.iterator());
            }
            it = chain;
        }
        while (it.hasNext()) {
            ShiftDvrpVehicle v = it.next();
            if (assignShiftToVehicleLogic.canAssignVehicleToShift(v, shift)) {
                it.remove();
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    private void assignShiftToVehicle(DrtShift shift, ShiftDvrpVehicle vehicle) {
        Gbl.assertNotNull(vehicle);
        vehicle.addShift(shift);
        shiftTaskScheduler.planAssignedShift(vehicle, timer.getTimeOfDay(), shift);
        assignedShifts.add(new ShiftEntry(shift, vehicle));
        eventsManager.processEvent(new DrtShiftAssignedEvent(timer.getTimeOfDay(), mode, shift.getId(),
                vehicle.getId(), shift.getShiftType().orElse(null)));
    }

    private void endShifts(double now) {
        // End shifts
        final Iterator<ShiftEntry> iterator = this.activeShifts.iterator();
        while (iterator.hasNext()) {
            final ShiftEntry next = iterator.next();

            if (now + drtShiftParams.getShiftEndLookAhead() < next.shift().getEndTime()) {
                continue;
            }

            final DrtShift active = next.shift();
            if(active.getEndTime() < now) {
                throw new RuntimeException(String.format("Could not end shift %s (end time: %f now: %f)", active.getId().toString(), active.getEndTime(), now));
            }

            if (active != next.vehicle().getShifts().peek()) {
                throw new IllegalStateException("Shifts don't match!");
            }

            logger.debug("Scheduling shift end for shift " + next.shift().getId() + " of vehicle " + next.vehicle().getId());
            if(scheduleShiftEnd(next)) {
                endingShifts.add(next);
                iterator.remove();
            }
        }
    }

    private void updateShiftEnds(double now) {
        final Iterator<ShiftEntry> endingShiftsIterator = this.endingShifts.iterator();
        while (endingShiftsIterator.hasNext()) {
            final ShiftEntry endingShift = endingShiftsIterator.next();
            if (endingShift.shift().isEnded()) {
                endingShiftsIterator.remove();
                continue;
            }
            if (now + drtShiftParams.getShiftEndRescheduleLookAhead() > endingShift.shift().getEndTime()) {
                if (endingShift.vehicle().getShifts().size() > 1) {
                    updateShiftEnd(endingShift);
                }
            } else {
                break;
            }
        }
    }

    private void updateShiftEnd(ShiftEntry endingShift) {

        if(endingShift.shift().getOperationFacilityId().isPresent()) {
            //start and end facility are fixed
            return;
        }

        final List<? extends Task> tasks = endingShift.vehicle().getSchedule().getTasks();

        Task lastTask = null;
        LinkTimePair start = null;
        ShiftChangeOverTask changeOverTask = null;
        final Task currentTask = endingShift.vehicle().getSchedule().getCurrentTask();
        for (Task task : tasks.subList(currentTask.getTaskIdx(), tasks.size())) {
            if (task instanceof ShiftChangeOverTask) {
                changeOverTask = (ShiftChangeOverTask) task;
            } else if (STOP.isBaseTypeOf(task)) {
                start = new LinkTimePair(((DrtStopTask) task).getLink(), task.getEndTime());
                lastTask = task;
            }
        }

        if (start == null) {
            lastTask = currentTask;
            switch (getBaseTypeOrElseThrow(currentTask)) {
                case DRIVE:
                    DrtDriveTask driveTask = (DrtDriveTask)currentTask;
                    LinkTimePair diversionPoint = ((OnlineDriveTaskTracker)driveTask.getTaskTracker()).getDiversionPoint();
                    //diversion possible
                    start = diversionPoint != null ? diversionPoint : //diversion possible
                            new LinkTimePair(driveTask.getPath().getToLink(), // too late for diversion
                                    driveTask.getEndTime());
                    break;
                case STOP:
                    DrtStopTask stopTask = (DrtStopTask)currentTask;
                    start = new LinkTimePair(stopTask.getLink(), stopTask.getEndTime());
                    break;

                case STAY:
                    DrtStayTask stayTask = (DrtStayTask)currentTask;
                    start = new LinkTimePair(stayTask.getLink(), timer.getTimeOfDay());
                    break;

                default:
                    throw new RuntimeException();
            }
        }

        final Optional<OperationFacility> maybeFacility;
        IntRange timeRange = new IntRange(endingShift.shift().getEndTime(), Integer.MAX_VALUE);
        if (drtShiftParams.isAllowInFieldChangeover()) {
            maybeFacility = breakFacilityFinder.findFacility(start.link.getCoord(), timeRange);
        } else {
            maybeFacility = breakFacilityFinder.findFacilityOfType(start.link.getCoord(),
                    OperationFacilityType.hub, timeRange);
        }
        if (maybeFacility.isPresent()) {
            OperationFacility shiftChangeFacility = maybeFacility.get();
            if(changeOverTask != null && !(shiftChangeFacility.getId().equals(changeOverTask.getFacility().getId()))) {
                if (shiftChangeFacility.hasCapacity(timeRange)) {
                    if (shiftTaskScheduler.updateShiftChange(endingShift.vehicle(),
                            network.getLinks().get(shiftChangeFacility.getLinkId()), endingShift.shift(), start,
                            shiftChangeFacility, lastTask)) {
                        shiftChangeFacility.register(endingShift.vehicle().getId(), timeRange);
                        changeOverTask.getFacility().deregisterVehicle(endingShift.vehicle().getId());
                        eventsManager.processEvent(new OperationFacilityRegistrationEvent(timer.getTimeOfDay(),
                                mode, endingShift.vehicle().getId(), shiftChangeFacility.getId()));
                    }
                }
            }
        }
    }

    private boolean scheduleShiftEnd(ShiftEntry endingShift) {
        // hub return
        final Schedule schedule = endingShift.vehicle().getSchedule();

        Task currentTask = schedule.getCurrentTask();
        Link lastLink;
        if (currentTask instanceof DriveTask
                && currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
                && currentTask.equals(schedule.getTasks().get(schedule.getTaskCount()-2))) {
            LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            if(start != null) {
                lastLink = start.link;
            } else {
                lastLink = ((DriveTask) currentTask).getPath().getToLink();
            }
        }  else {
            lastLink = ((DrtStayTask) schedule.getTasks()
                    .get(schedule.getTaskCount() - 1)).getLink();
        }
        final Coord coord = lastLink.getCoord();

        OperationFacility shiftChangeoverFacility = null;

        //check whether current shift has to end at specific facility
        if(endingShift.shift().getOperationFacilityId().isPresent()) {
            shiftChangeoverFacility = operationFacilities
                    .getDrtOperationFacilities()
                    .get(endingShift.shift().getOperationFacilityId().get());
        } else {
            //check whether next shift has to start at specific facility
            for (DrtShift shift : endingShift.vehicle().getShifts()) {
                if (shift != endingShift.shift()) {
                    if (shift.getOperationFacilityId().isPresent()) {
                        shiftChangeoverFacility = operationFacilities
                                .getDrtOperationFacilities()
                                .get(shift.getOperationFacilityId().get());
                    }
                    break;
                }
            }
        }

        IntRange timeRange = new IntRange(endingShift.shift().getEndTime(), Integer.MAX_VALUE);
        if(shiftChangeoverFacility == null) {
            shiftChangeoverFacility = breakFacilityFinder.findFacilityOfType(coord,
                    OperationFacilityType.hub, timeRange).orElse(null);
        }

        if(shiftChangeoverFacility == null) {
            return false;
        }

        if (!shiftChangeoverFacility.register(endingShift.vehicle().getId(), timeRange)) {
            return false;
        }

        shiftTaskScheduler.relocateForShiftChange(endingShift.vehicle(),
                network.getLinks().get(shiftChangeoverFacility.getLinkId()), endingShift.shift(), shiftChangeoverFacility);
        eventsManager.processEvent(new OperationFacilityRegistrationEvent(timer.getTimeOfDay(), mode, endingShift.vehicle().getId(),
                shiftChangeoverFacility.getId()));
        return true;
    }

    @Override
    public void endShift(ShiftDvrpVehicle vehicle, Id<Link> id, Id<OperationFacility> operationFacilityId) {
        final DrtShift shift = vehicle.getShifts().poll();
        logger.debug("Ended shift " + shift.getId());
        shift.end();
        eventsManager.processEvent(new DrtShiftEndedEvent(timer.getTimeOfDay(),mode,  shift.getId(), vehicle.getId(), id, operationFacilityId, null));
        if (vehicle.getShifts().isEmpty()) {
            idleVehiclesQueues.get(operationFacilityId).add(vehicle);
        }
    }

    @Override
    public void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask previousTask) {
        final OperationFacility facility = previousTask.getFacility();
        facility.deregisterVehicle(vehicle.getId());
        eventsManager.processEvent(
                new VehicleLeftOperationFacilityEvent(timer.getTimeOfDay(), mode, vehicle.getId(), facility.getId()));
        DrtShift shift = vehicle.getShifts().peek();
        Gbl.assertNotNull(shift, "Shift may not be null!");
        eventsManager.processEvent(
                new DrtShiftBreakEndedEvent(timer.getTimeOfDay(), mode, shift.getId(),
                        vehicle.getId(), previousTask.getFacility().getLinkId(), shift.getShiftType().orElse(null))
        );
    }

    public void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId) {
        DrtShift shift = vehicle.getShifts().peek();
        Gbl.assertNotNull(shift, "Shift must not be null!");
        eventsManager.processEvent(
                new DrtShiftBreakStartedEvent(timer.getTimeOfDay(), mode,
                        shift.getId(), vehicle.getId(), linkId, shift.getShiftType().orElse(null))
        );
    }

    private boolean isSchedulable(DrtShift shift, double now) {
        return shift.getStartTime() <= now + drtShiftParams.getShiftScheduleLookAhead();
    }
}
