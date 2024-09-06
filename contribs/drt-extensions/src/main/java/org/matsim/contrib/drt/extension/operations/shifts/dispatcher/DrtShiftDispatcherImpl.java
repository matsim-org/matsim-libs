package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import com.google.common.base.Verify;
import org.apache.commons.collections4.iterators.IteratorChain;
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

    public DrtShiftDispatcherImpl(String mode, Fleet fleet, MobsimTimer timer, OperationFacilities operationFacilities,
                                  OperationFacilityFinder breakFacilityFinder, ShiftTaskScheduler shiftTaskScheduler,
                                  Network network, EventsManager eventsManager, ShiftsParams drtShiftParams,
                                  ShiftStartLogic shiftStartLogic, AssignShiftToVehicleLogic assignShiftToVehicleLogic,
                                  ShiftScheduler shiftScheduler) {
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
    public void dispatch(double timeStep) {
        if(timeStep % drtShiftParams.loggingInterval == 0) {
            logger.info(String.format("Active shifts: %s | Assigned shifts: %s | Unscheduled shifts: %s",
                    activeShifts.size(), assignedShifts.size(), unAssignedShifts.size()));
            StringJoiner print = new StringJoiner(" | ");
            for (Map.Entry<Id<OperationFacility>, Queue<ShiftDvrpVehicle>> queueEntry : idleVehiclesQueues.entrySet()) {
                print.add(String.format("Idle vehicles at facility %s: %d", queueEntry.getKey().toString(), queueEntry.getValue().size()));
            }
            logger.info(print.toString());
        }
        endShifts(timeStep);
        if (timeStep % (drtShiftParams.updateShiftEndInterval) == 0) {
            updateShiftEnds(timeStep);
        }
        scheduleShifts(timeStep, this.fleet);
        assignShifts(timeStep);
        startShifts(timeStep);
        checkBreaks();
    }

    private void checkBreaks() {
        for (ShiftEntry activeShift : activeShifts) {
            final DrtShift shift = activeShift.shift();
            if (shift != null && shift.isStarted()) {
                if (hasSchedulableBreak(shift, timer.getTimeOfDay())) {
                    Optional<OperationFacility> breakFacility = findBreakFacility(activeShift);
                    if (breakFacility.isPresent()) {
                        OperationFacility facility = breakFacility.get();
                        if (facility.register(activeShift.vehicle().getId())) {
                            eventsManager.processEvent(new ShiftFacilityRegistrationEvent(
                                    timer.getTimeOfDay(), mode, activeShift.vehicle().getId(), facility.getId()));
                            shiftTaskScheduler.relocateForBreak(activeShift.vehicle(), facility, shift);
                            eventsManager.processEvent(new DrtShiftBreakScheduledEvent(timer.getTimeOfDay(), mode, shift.getId(),
                                    activeShift.vehicle().getId(), facility.getLinkId(),
                                    shift.getBreak().orElseThrow().getScheduledLatestArrival()));
                            return;
                        }
                    }
                    throw new RuntimeException("Could not schedule break!");
                }
            }
        }
    }


    private void scheduleShifts(double timeStep, Fleet fleet) {
        List<DrtShift> scheduled = shiftScheduler.schedule(timeStep, fleet);
        unAssignedShifts.addAll(scheduled);
    }

    private void startShifts(double timeStep) {
        final Iterator<ShiftEntry> iterator = this.assignedShifts.iterator();
        while (iterator.hasNext()) {
            final ShiftEntry assignedShiftEntry = iterator.next();
            if (assignedShiftEntry.shift().getStartTime() > timeStep) {
                shiftTaskScheduler.planAssignedShift(assignedShiftEntry.vehicle(), timeStep, assignedShiftEntry.shift());
                continue;
            } else if (assignedShiftEntry.shift().getEndTime() < timeStep) {
                logger.warn("Too late to start shift " + assignedShiftEntry.shift().getId());
                shiftTaskScheduler.cancelAssignedShift(assignedShiftEntry.vehicle(), timeStep, assignedShiftEntry.shift());
                assignedShiftEntry.vehicle().getShifts().remove(assignedShiftEntry.shift());
                iterator.remove();
                continue;
            }

            if (shiftStartLogic.shiftStarts(assignedShiftEntry)) {
                assignedShiftEntry.shift().start();
                shiftTaskScheduler.startShift(assignedShiftEntry.vehicle(), timeStep, assignedShiftEntry.shift());
                activeShifts.add(assignedShiftEntry);
                iterator.remove();
                logger.debug("Started shift " + assignedShiftEntry.shift());
                StayTask currentTask = (StayTask) assignedShiftEntry.vehicle().getSchedule().getCurrentTask();
                eventsManager.processEvent(new DrtShiftStartedEvent(timeStep, mode, assignedShiftEntry.shift().getId(), assignedShiftEntry.vehicle().getId(),
                        currentTask.getLink().getId()));
            } else {
                shiftTaskScheduler.planAssignedShift(assignedShiftEntry.vehicle(), timeStep, assignedShiftEntry.shift());
            }
        }
    }

    private void assignShifts(double timeStep) {
        // Remove elapsed shifts
        unAssignedShifts.removeIf(shift -> {
            if (shift.getStartTime() + drtShiftParams.maxUnscheduledShiftDelay < timeStep ) {
                logger.warn("Shift with ID " + shift.getId() + " could not be assigned and is being removed as start time is longer in the past than defined by maxUnscheduledShiftDelay.");
                return true;
            }
            return false;
        });

        // Assign shifts
        Set<DrtShift> assignableShifts = new LinkedHashSet<>();
        Iterator<DrtShift> unscheduledShiftsIterator = unAssignedShifts.iterator();
        while(unscheduledShiftsIterator.hasNext()) {
            DrtShift unscheduledShift = unscheduledShiftsIterator.next();
            if(isSchedulable(unscheduledShift, timeStep)) {
                assignableShifts.add(unscheduledShift);
                unscheduledShiftsIterator.remove();
            } else {
                break;
            }
        }

        for (DrtShift shift : assignableShifts) {
            ShiftDvrpVehicle vehicle = null;

            if(shift.getDesignatedVehicleId().isPresent()) {
                DvrpVehicle designatedVehicle = fleet.getVehicles().get(shift.getDesignatedVehicleId().get());
                Verify.verify(designatedVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED);
                Verify.verify(designatedVehicle instanceof ShiftDvrpVehicle);
                if(!((ShiftDvrpVehicle) designatedVehicle).getShifts().isEmpty()) {
                    continue;
                }
                if(shift.getOperationFacilityId().isPresent()) {
                    Verify.verify(idleVehiclesQueues.get(shift.getOperationFacilityId().get()).contains(designatedVehicle));
                }
                vehicle = (ShiftDvrpVehicle) designatedVehicle;
            }

            if(vehicle == null) {
                for (ShiftEntry active : activeShifts) {
                    if (active.shift().getEndTime() > shift.getStartTime()) {
                        break;
                    }
                    if (shift.getOperationFacilityId().isPresent()) {
                        //we have to check that the vehicle ends the previous shift at the same facility where
                        //the new shift is to start.
                        if (active.shift().getOperationFacilityId().isPresent()) {
                            if (!active.shift().getOperationFacilityId().get().equals(shift.getOperationFacilityId().get())) {
                                continue;
                            }
                        } else {
                            Optional<ShiftChangeOverTask> nextShiftChangeover = ShiftSchedules.getNextShiftChangeover(active.vehicle().getSchedule());
                            if (nextShiftChangeover.isPresent()) {
                                Verify.verify(nextShiftChangeover.get().getShift().equals(active.shift()));
                                if (!nextShiftChangeover.get().getFacility().getId().equals(shift.getOperationFacilityId().get())) {
                                    // there is already a shift changeover scheduled elsewhere
                                    continue;
                                }
                            }
                        }
                    }
                    if (assignShiftToVehicleLogic.canAssignVehicleToShift(active.vehicle(), shift)) {
                        vehicle = active.vehicle();
                        break;
                    }
                }
            }

            if (vehicle == null) {
                final Iterator<ShiftDvrpVehicle> iterator;

                if(shift.getOperationFacilityId().isPresent()) {
                    //shift has to start at specific hub/facility
                    iterator = idleVehiclesQueues.get(shift.getOperationFacilityId().get()).iterator();
                } else {
                    //shift can start at random location
                    IteratorChain<ShiftDvrpVehicle> iteratorChain = new IteratorChain<>();
                    for (Queue<ShiftDvrpVehicle> value : idleVehiclesQueues.values()) {
                        iteratorChain.addIterator(value.iterator());
                    }
                    iterator = iteratorChain;
                }

                while (iterator.hasNext()) {
                    final ShiftDvrpVehicle next = iterator.next();
                    if (assignShiftToVehicleLogic.canAssignVehicleToShift(next, shift)) {
                        vehicle = next;
                        iterator.remove();
                        break;
                    }
                }
            }

            if (vehicle != null) {
                logger.debug("Shift assigned");
                assignShiftToVehicle(shift, vehicle);
            } else {
                // logger.warn("Could not assign shift " + shift.getId().toString() + " to a
                // vehicle. Will retry next time step.");
                this.unAssignedShifts.add(shift);
            }
        }
    }

    private void assignShiftToVehicle(DrtShift shift, ShiftDvrpVehicle vehicle) {
        Gbl.assertNotNull(vehicle);
        vehicle.addShift(shift);
        shiftTaskScheduler.planAssignedShift(vehicle, timer.getTimeOfDay(), shift);
        assignedShifts.add(new ShiftEntry(shift, vehicle));
        eventsManager.processEvent(new DrtShiftAssignedEvent(timer.getTimeOfDay(), mode, shift.getId(), vehicle.getId()));
    }

    private void endShifts(double timeStep) {
        // End shifts
        final Iterator<ShiftEntry> iterator = this.activeShifts.iterator();
        while (iterator.hasNext()) {
            final ShiftEntry next = iterator.next();

            if (timeStep + drtShiftParams.shiftEndLookAhead < next.shift().getEndTime()) {
                continue;
            }

            final DrtShift active = next.shift();

            if (active != next.vehicle().getShifts().peek()) {
                throw new IllegalStateException("Shifts don't match!");
            }

            logger.debug("Scheduling shift end for shift " + next.shift().getId() + " of vehicle " + next.vehicle().getId());
            scheduleShiftEnd(next);
            endingShifts.add(next);
            iterator.remove();
        }
    }

    private void updateShiftEnds(double timeStep) {
        final Iterator<ShiftEntry> endingShiftsIterator = this.endingShifts.iterator();
        while (endingShiftsIterator.hasNext()) {
            final ShiftEntry next = endingShiftsIterator.next();
            if (next.shift().isEnded()) {
                endingShiftsIterator.remove();
                continue;
            }
            if (timeStep + drtShiftParams.shiftEndRescheduleLookAhead > next.shift().getEndTime()) {
                if (next.vehicle().getShifts().size() > 1) {
                    updateShiftEnd(next);
                }
            } else {
                break;
            }
        }
    }

    private void updateShiftEnd(ShiftEntry next) {

        if(next.shift().getOperationFacilityId().isPresent()) {
            //start and end facility are fixed
            return;
        }

        final List<? extends Task> tasks = next.vehicle().getSchedule().getTasks();

        Task lastTask = null;
        LinkTimePair start = null;
        ShiftChangeOverTask changeOverTask = null;
        final Task currentTask = next.vehicle().getSchedule().getCurrentTask();
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
        if (drtShiftParams.allowInFieldChangeover) {
            maybeFacility = breakFacilityFinder.findFacility(start.link.getCoord());
        } else {
            maybeFacility = breakFacilityFinder.findFacilityOfType(start.link.getCoord(),
                    OperationFacilityType.hub);
        }
        if (maybeFacility.isPresent()) {
            OperationFacility shiftChangeFacility = maybeFacility.get();
            if(changeOverTask != null && !(shiftChangeFacility.getId().equals(changeOverTask.getFacility().getId()))) {
                if (shiftChangeFacility.hasCapacity()) {
                    if (shiftTaskScheduler.updateShiftChange(next.vehicle(),
                            network.getLinks().get(shiftChangeFacility.getLinkId()), next.shift(), start,
                            shiftChangeFacility, lastTask)) {
                        shiftChangeFacility.register(next.vehicle().getId());
                        changeOverTask.getFacility().deregisterVehicle(next.vehicle().getId());
                        eventsManager.processEvent(new ShiftFacilityRegistrationEvent(timer.getTimeOfDay(),
                                mode, next.vehicle().getId(), shiftChangeFacility.getId()));
                    }
                }
            }
        }
    }

    private void scheduleShiftEnd(ShiftEntry endingShift) {
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

        if(shiftChangeoverFacility == null) {
            shiftChangeoverFacility = breakFacilityFinder.findFacilityOfType(coord,
                    OperationFacilityType.hub).orElseThrow(() -> new RuntimeException("Could not find shift end location!"));
        }

        Verify.verify(shiftChangeoverFacility.register(endingShift.vehicle().getId()), "Could not register vehicle at facility.");

        shiftTaskScheduler.relocateForShiftChange(endingShift.vehicle(),
                network.getLinks().get(shiftChangeoverFacility.getLinkId()), endingShift.shift(), shiftChangeoverFacility);
        eventsManager.processEvent(new ShiftFacilityRegistrationEvent(timer.getTimeOfDay(), mode, endingShift.vehicle().getId(),
                shiftChangeoverFacility.getId()));
    }

    private Optional<OperationFacility> findBreakFacility(ShiftEntry activeShift) {
        final Schedule schedule = activeShift.vehicle().getSchedule();
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
        return breakFacilityFinder.findFacility(lastLink.getCoord());
    }

    @Override
    public void endShift(ShiftDvrpVehicle vehicle, Id<Link> id, Id<OperationFacility> operationFacilityId) {
        final DrtShift shift = vehicle.getShifts().poll();
        logger.debug("Ended shift " + shift.getId());
        shift.end();
        eventsManager.processEvent(new DrtShiftEndedEvent(timer.getTimeOfDay(),mode,  shift.getId(), vehicle.getId(), id, operationFacilityId));
        if (vehicle.getShifts().isEmpty()) {
            idleVehiclesQueues.get(operationFacilityId).add(vehicle);
        }
    }

    @Override
    public void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask previousTask) {
        final OperationFacility facility = previousTask.getFacility();
        facility.deregisterVehicle(vehicle.getId());
        eventsManager.processEvent(
                new VehicleLeftShiftFacilityEvent(timer.getTimeOfDay(), mode, vehicle.getId(), facility.getId()));
        eventsManager.processEvent(
                new DrtShiftBreakEndedEvent(timer.getTimeOfDay(), mode, vehicle.getShifts().peek().getId(),
                        vehicle.getId(), previousTask.getFacility().getLinkId())
        );
    }

    public void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId) {
        eventsManager.processEvent(
                new DrtShiftBreakStartedEvent(timer.getTimeOfDay(), mode,
                        vehicle.getShifts().peek().getId(), vehicle.getId(), linkId)
        );
    }

    private boolean isSchedulable(DrtShift shift, double timeStep) {
        return shift.getStartTime() <= timeStep + drtShiftParams.shiftScheduleLookAhead;
    }

    private boolean hasSchedulableBreak(DrtShift shift, double timeStep) {
        return shift.getBreak().isPresent() && shift.getBreak().get().getEarliestBreakStartTime() == timeStep
                && !shift.getBreak().get().isScheduled();
    }
}
