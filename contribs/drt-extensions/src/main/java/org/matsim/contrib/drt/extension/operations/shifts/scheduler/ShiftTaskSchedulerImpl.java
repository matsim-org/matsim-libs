package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.*;
import org.matsim.contrib.drt.extension.operations.shifts.charging.ShiftChargingLogic;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftBreakStopWaypoint;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftChangeoverStopWaypoint;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.evrp.ChargingTaskImpl;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.matsim.contrib.drt.schedule.DrtDriveTask.TYPE;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * Unified implementation of ShiftTaskScheduler that works with all vehicle types.
 * This implementation handles both standard and electric vehicles using the unified task implementations.
 * 
 * @author nkuehnel / MOIA
 */
public class ShiftTaskSchedulerImpl implements ShiftTaskScheduler {

    private final static Logger logger = LogManager.getLogger(ShiftTaskSchedulerImpl.class);

    private final ShiftDrtTaskFactory taskFactory;
    private final OperationFacilities facilities;
    private final Network network;
    private final OperationFacilityReservationManager facilityReservationManager;
    private final ShiftsParams shiftsParams;
    private final LeastCostPathCalculator router;
    private final TravelTime travelTime;
    private final OperationFacilityFinder operationFacilityFinder;
    private final VehicleEntry.EntryFactory vEntryFactory;
    private final ScheduleTimingUpdater timingUpdater;
    
    // Optional charging-related components - null for standard vehicles
    private final ShiftChargingLogic shiftChargingLogic;
    private final ChargingInfrastructure chargingInfrastructure;

    /**
     * Constructor for standard vehicles (no charging capability)
     */
    public ShiftTaskSchedulerImpl(
            OperationFacilities operationFacilities,
            ShiftDrtTaskFactory taskFactory, 
            Network network,
            OperationFacilityReservationManager facilityReservationManager,
            ShiftsParams shiftsParams, 
            TravelDisutility travelDisutility,
            TravelTime travelTime, 
            OperationFacilityFinder operationFacilityFinder,
            VehicleEntry.EntryFactory vEntryFactory, 
            ScheduleTimingUpdater timingUpdater) {
        this(operationFacilities, taskFactory, network, facilityReservationManager, shiftsParams,
                travelDisutility, travelTime, operationFacilityFinder, vEntryFactory, timingUpdater,
                null, null);
    }

    /**
     * Constructor for electric vehicles (with charging capability)
     */
    public ShiftTaskSchedulerImpl(
            OperationFacilities operationFacilities,
            ShiftDrtTaskFactory taskFactory, 
            Network network,
            OperationFacilityReservationManager facilityReservationManager,
            ShiftsParams shiftsParams, 
            TravelDisutility travelDisutility,
            TravelTime travelTime, 
            OperationFacilityFinder operationFacilityFinder,
            VehicleEntry.EntryFactory vEntryFactory, 
            ScheduleTimingUpdater timingUpdater,
            ChargingStrategy.Factory chargingStrategyFactory,
            ChargingInfrastructure chargingInfrastructure) {
        this.taskFactory = taskFactory;
        this.facilities = operationFacilities;
        this.network = network;
        this.facilityReservationManager = facilityReservationManager;
        this.shiftsParams = shiftsParams;
        this.timingUpdater = timingUpdater;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
        this.travelTime = travelTime;
        this.operationFacilityFinder = operationFacilityFinder;
        this.vEntryFactory = vEntryFactory;
        
        // Initialize charging-related components if provided
        this.chargingInfrastructure = chargingInfrastructure;
        this.shiftChargingLogic = chargingStrategyFactory != null && chargingInfrastructure != null ? 
                new ShiftChargingLogic(shiftsParams, chargingInfrastructure, chargingStrategyFactory) : null;
    }

    @Override
    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
        Schedule schedule = vehicle.getSchedule();
        Task currentTask = schedule.getCurrentTask();

        if (currentTask instanceof WaitForShiftTask waitForShiftTask) {
            // Handle charging if present
            waitForShiftTask.getChargingTask().ifPresent(chargingTask -> {
                if (vehicle instanceof EvDvrpVehicle evVehicle) {
                    ChargingWithAssignmentLogic chargingLogic = chargingTask.getChargingLogic();
                    ElectricVehicle ev = evVehicle.getElectricVehicle();
                    if (Stream.concat(chargingLogic.getPluggedVehicles().stream(), chargingLogic.getQueuedVehicles().stream())
                            .map(ChargingLogic.ChargingVehicle::ev)
                            .anyMatch(chargerEv -> chargerEv.getId().equals(ev.getId()))) {
                        chargingLogic.removeVehicle(ev, now);
                        chargingTask.setEndTime(now);
                    }
                }
            });

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
                        ShiftBreakTask breakTask = taskFactory.createShiftBreakTask(vehicle, initialStayEndTime,
                                breakEndTime, waitForShiftTask.getLink(), shiftBreak.get(),
                                operationFacility.getId(), reservation.get().reservationId());
                        
                        // Add charging if appropriate (for electric vehicles)
                        if (shiftChargingLogic != null && vehicle instanceof EvDvrpVehicle) {
                            addChargingToBreakIfNeeded((EvDvrpVehicle) vehicle, breakTask, operationFacility);
                        }
                        
                        schedule.addTask(breakTask);
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
                        WaitForShiftTask waitTask = taskFactory.createWaitForShiftStayTask(vehicle, changeTask.getEndTime(),
                                vehicle.getServiceEndTime(), waitForShiftTask.getLink(),
                                operationFacility.getId(), changeoverReg.get().reservationId());
                        schedule.addTask(waitTask);

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

    /**
     * Adds charging to a break task if needed and possible
     */
    private void addChargingToBreakIfNeeded(EvDvrpVehicle vehicle, ShiftBreakTask breakTask, OperationFacility facility) {
        if (shiftChargingLogic == null) {
            return;
        }

        ElectricVehicle ev = vehicle.getElectricVehicle();

        // Check if charging should be added
        boolean shouldCharge = shouldAddChargingToBreak(vehicle, breakTask, facility);
        
        if (shouldCharge) {
            // Find available charger with strategy
            Optional<ShiftChargingLogic.ChargerWithStrategy> chargerWithStrategy =
                    shiftChargingLogic.findAvailableCharger(facility, ev);

            if (chargerWithStrategy.isPresent()) {
                Charger charger = chargerWithStrategy.get().charger();
                ChargingStrategy strategy = chargerWithStrategy.get().strategy();

                // Calculate energy to charge during break
                double breakDuration = breakTask.getEndTime() - breakTask.getBeginTime();
                double energyCharge = ((BatteryCharging) ev.getChargingPower())
                        .calcEnergyCharged(charger.getSpecification(), breakDuration);

                // Create charging task
                ChargingTaskImpl chargingTask = new ChargingTaskImpl(
                        EDrtChargingTask.TYPE,
                        breakTask.getBeginTime(),
                        breakTask.getEndTime(),
                        charger,
                        vehicle.getElectricVehicle(),
                        -energyCharge, // Negative value means charging
                        strategy);

                // Add charging to the break task
                boolean added = breakTask.addCharging(chargingTask);

                if(added) {
                    // Assign vehicle to charger
                    ((ChargingWithAssignmentLogic) charger.getLogic()).assignVehicle(ev, strategy);
                } else {
                    logger.warn("Could not add charging to break task for " + charger + " for " + ev);
                }
            }
        }
    }
    
    /**
     * Determines if charging should be added to a break task
     */
    private boolean shouldAddChargingToBreak(EvDvrpVehicle vehicle, ShiftBreakTask breakTask, OperationFacility facility) {
        if (shiftChargingLogic == null) {
            return false;
        }
        
        // Get current SOC
        ElectricVehicle ev = vehicle.getElectricVehicle();
        double soc = ev.getBattery().getCharge() / ev.getBattery().getCapacity();
        
        // Check if SOC is below threshold and facility has chargers
        return soc <= shiftsParams.getChargeDuringBreakThreshold() && 
                facility.getChargers() != null && !facility.getChargers().isEmpty();
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
            schedule.addTask(taskFactory.createDriveTask(vehicle, vrpPath, TYPE));
        }
        if (vrpPath.getArrivalTime() < shift.getEndTime()) {
            schedule.addTask(taskFactory.createStayTask(vehicle, vrpPath.getArrivalTime(), shift.getEndTime(), vrpPath.getToLink()));
        }
        final double endTime = Math.max(shift.getEndTime(), vrpPath.getArrivalTime()) + shiftsParams.getChangeoverDuration();
        ShiftChangeOverTask changeTask = taskFactory.createShiftChangeoverTask(vehicle, Math.max(shift.getEndTime(),
                vrpPath.getArrivalTime()), endTime, vrpPath.getToLink(), shift, reservation.resource().getId(), reservation.reservationId());
        schedule.addTask(changeTask);
        
        // Create the wait task
        WaitForShiftTask waitTask = taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), reservation.resource().getId(), reservation.reservationId());
        
        schedule.addTask(waitTask);
    }

    /**
     * Context object for shift break operations to avoid excessive parameter passing
     */
    private record ShiftBreakContext(
            VehicleEntry vehicleEntry,
            Waypoint startWaypoint,
            ShiftBreakStopWaypoint shiftBreakStop,
            StopWaypoint end,
            ShiftBreakTask shiftBreakTask,
            DrtShiftBreak shiftBreak,
            DvrpVehicle vehicle,
            Schedule schedule,
            Optional<OperationFacilityFinder.FacilityWithPath> facility) {
    }

    /**
     * Record to store break timing information
     */
    private record BreakTimingInfo(double breakStart , double reservationStart, double reservationEnd) {
    }

    /**
     * Prepares the context object for shift break operations
     */
    private ShiftBreakContext prepareShiftBreakContext(DrtShiftDispatcher.ShiftEntry activeShift, double now) {
        // Create vehicle entry
        VehicleEntry vehicleEntry = vEntryFactory.create(activeShift.vehicle(), now);
        if (vehicleEntry == null) {
            return null;
        }

        // Find waypoints
        Waypoint startWaypoint = vehicleEntry.start;
        ShiftBreakStopWaypoint shiftBreakStop = null;
        StopWaypoint end = null;

        // Find existing shift break and follow up stop
        for (int i = 0; i < vehicleEntry.stops.size(); i++) {
            StopWaypoint stop = vehicleEntry.stops.get(i);
            if (stop instanceof ShiftBreakStopWaypoint) {
                shiftBreakStop = (ShiftBreakStopWaypoint) stop;
                if (i + 1 < vehicleEntry.stops.size()) {
                    end = vehicleEntry.stops.get(i + 1);
                }
                break;
            } else {
                startWaypoint = stop;
            }
        }

        // No break to update
        if (shiftBreakStop == null || end == null) {
            return null;
        }

        ShiftBreakTask shiftBreakTask = (ShiftBreakTask) shiftBreakStop.getTask();
        DrtShiftBreak shiftBreak = shiftBreakTask.getShiftBreak();

        final Optional<OperationFacilityFinder.FacilityWithPath> facility;
        if(shiftBreakTask.getStatus() != Task.TaskStatus.PLANNED) {
            facility = Optional.of(new OperationFacilityFinder.FacilityWithPath(facilities.getFacilities().get(shiftBreakTask.getFacilityId()), null));
        } else {
            // Find potential new facility
            Set<OperationFacilityType> facilityTypes = Set.of(OperationFacilityType.hub, OperationFacilityType.inField);
            facility = operationFacilityFinder.findFacilityForTime(
                    startWaypoint.getLink(),
                    activeShift.vehicle(),
                    startWaypoint.getDepartureTime(),
                    shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration(),
                    shiftBreak.getLatestBreakEndTime(),
                    facilityTypes);
        }

        return new ShiftBreakContext(
                vehicleEntry,
                startWaypoint,
                shiftBreakStop,
                end,
                shiftBreakTask,
                shiftBreak,
                activeShift.vehicle(),
                activeShift.vehicle().getSchedule(),
                facility
        );
    }

    @Override
    public boolean updateShiftBreak(DrtShiftDispatcher.ShiftEntry activeShift, double now) {
        // Prepare context and validate
        ShiftBreakContext context = prepareShiftBreakContext(activeShift, now);
        if (context == null || context.facility().isEmpty()) {
            return false;
        }

        OperationFacility newFacility = context.facility().get().operationFacility();
        Id<OperationFacility> existingFacilityId = context.shiftBreakTask().getFacilityId();

        // Update due to facility change
        boolean facilityChanged = !newFacility.getId().equals(existingFacilityId);
        
        // Update due to charging need (for electric vehicles)
        boolean chargingNeeded = false;
        if (!facilityChanged && shiftChargingLogic != null && context.vehicle() instanceof EvDvrpVehicle) {
            EvDvrpVehicle evVehicle = (EvDvrpVehicle) context.vehicle();
            chargingNeeded = shouldAddChargingToBreak(evVehicle, context.shiftBreakTask(), newFacility) && 
                    !context.shiftBreakTask().getChargingTask().isPresent();
        }

        // Only update if facility has changed or charging is needed
        if (facilityChanged) {
            return tryUpdateShiftBreak(context, newFacility, existingFacilityId, now);
        } else if (chargingNeeded) {
            return tryAddChargingToExistingBreak(context, (EvDvrpVehicle) context.vehicle(), now);
        } else {
            return false;
        }
    }
    
    /**
     * Tries to add charging to an existing break task
     */
    private boolean tryAddChargingToExistingBreak(ShiftBreakContext context, EvDvrpVehicle vehicle, double now) {
        if (shiftChargingLogic == null) {
            return false;
        }
        
        OperationFacility facility = facilities.getFacilities().get(context.shiftBreakTask().getFacilityId());
        ElectricVehicle ev = vehicle.getElectricVehicle();
        
        // Find available charger with strategy
        Optional<ShiftChargingLogic.ChargerWithStrategy> chargerWithStrategy =
                shiftChargingLogic.findAvailableCharger(facility, ev);
        
        if (chargerWithStrategy.isPresent()) {
            Charger charger = chargerWithStrategy.get().charger();
            ChargingStrategy strategy = chargerWithStrategy.get().strategy();
            
            // Calculate energy to charge during break
            double breakDuration = context.shiftBreakTask().getEndTime() - context.shiftBreakTask().getBeginTime();
            double energyCharge = ((BatteryCharging) ev.getChargingPower())
                    .calcEnergyCharged(charger.getSpecification(), breakDuration);
            
            // Create charging task
            ChargingTaskImpl chargingTask = new ChargingTaskImpl(
                    EDrtChargingTask.TYPE,
                    context.shiftBreakTask().getBeginTime(),
                    context.shiftBreakTask().getEndTime(),
                    charger,
                    vehicle.getElectricVehicle(),
                    -energyCharge, // Negative value means charging
                    strategy);
            
            // Assign vehicle to charger
            ((ChargingWithAssignmentLogic) charger.getLogic()).assignVehicle(ev, strategy);
            
            // Add charging to the break task
            return context.shiftBreakTask().addCharging(chargingTask);
        }
        
        return false;
    }

    /**
     * Attempts to update the shift break with a new facility
     *
     * @param context the shift break context
     * @param newFacility the new facility
     * @param existingFacilityId the existing facility ID
     * @param now current time
     * @return true if update was successful
     */
    private boolean tryUpdateShiftBreak(
            ShiftBreakContext context,
            OperationFacility newFacility,
            Id<OperationFacility> existingFacilityId,
            double now) {

        // 1. First validate all timing constraints without modifying the schedule
        if (!isTimingValid(context)) {
            return false;
        }

        // 2. Calculate break timing
        BreakTimingInfo breakTimingInfo = calculateBreakTiming(context);
        if (breakTimingInfo == null) {
            return false;
        }

        // 3. make new reservation and Clean up existing reservation
        Optional<ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>> reservation =
                makeBreakReservation(newFacility, context.vehicle(), breakTimingInfo);

        if (reservation.isEmpty()) {
            return false;
        }

        cleanupExistingReservation(existingFacilityId, context.shiftBreakTask(), context.vehicle, now);

        // 4. All validations passed, NOW we can modify the schedule
        Task arrivalTask = modifyTasksForBreak(context, now);
        if (arrivalTask == null) {
            return false;
        }

        // 5. Add the break and continuation tasks
        Link facilityLink = network.getLinks().get(newFacility.getLinkId());
        ShiftBreakTask newShiftBreakTask = taskFactory.createShiftBreakTask(
                context.vehicleEntry().vehicle,
                arrivalTask.getEndTime(),
                calculateBreakEndTime(arrivalTask.getEndTime(), context.shiftBreak()),
                facilityLink,
                context.shiftBreak(),
                newFacility.getId(),
                reservation.get().reservationId());

        context.schedule().addTask(arrivalTask.getTaskIdx() + 1, newShiftBreakTask);

        // Add charging if appropriate (for electric vehicles)
        if (shiftChargingLogic != null && context.vehicle() instanceof EvDvrpVehicle) {
            addChargingToBreakIfNeeded((EvDvrpVehicle) context.vehicle(), newShiftBreakTask, newFacility);
        }


        // 6. Calculate continuation path
        VrpPathWithTravelData continuationPath = calculateContinuationPath(context, newShiftBreakTask.getEndTime());
        if (continuationPath == null) {
            return false;
        }

        // 7. Add continuation tasks
        boolean success = addContinuationTasks(context, newShiftBreakTask, continuationPath, facilityLink);
        if (success) {
            Task startTask = context.startWaypoint instanceof StopWaypoint ? ((StopWaypoint) context.startWaypoint).getTask(): context.schedule.getCurrentTask();
            timingUpdater.updateTimingsStartingFromTaskIdx(context.vehicle, startTask.getTaskIdx() +1, startTask.getEndTime());
            return true;
        }
        return false;
    }

    /**
     * Adds tasks for continuing after the break
     */
    private boolean addContinuationTasks(
            ShiftBreakContext context,
            ShiftBreakTask breakTask,
            VrpPathWithTravelData continuationPath,
            Link facilityLink) {

        Task leadingTask = breakTask;

        // Add wait task if needed
        if (context.end().scheduleWaitBeforeDrive()) {
            double driveDepartureTime = context.end().getTask().getBeginTime() - continuationPath.getTravelTime();

            if (driveDepartureTime > breakTask.getEndTime()) {
                DrtStayTask stayTask = taskFactory.createStayTask(
                        context.vehicleEntry().vehicle,
                        breakTask.getEndTime(),
                        driveDepartureTime,
                        facilityLink
                );
                context.schedule().addTask(breakTask.getTaskIdx() + 1, stayTask);

                continuationPath = continuationPath.withDepartureTime(driveDepartureTime);
                leadingTask = stayTask;
            }
        }

        // Add drive task to end
        DrtDriveTask driveTask = taskFactory.createDriveTask(
                context.vehicleEntry().vehicle,
                continuationPath,
                context.end instanceof  ShiftChangeoverStopWaypoint ? RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE: TYPE
        );
        context.schedule().addTask(leadingTask.getTaskIdx() + 1, driveTask);

        // Add stay task if needed
        if (driveTask.getEndTime() < context.end().getTask().getBeginTime()) {
            DrtStayTask stayTask = taskFactory.createStayTask(
                    context.vehicleEntry().vehicle,
                    driveTask.getEndTime(),
                    context.end().getTask().getBeginTime(),
                    context.end().getLink()
            );
            context.schedule().addTask(driveTask.getTaskIdx() + 1, stayTask);
        }

        return true;
    }

    /**
     * Calculates the end time of a break
     */
    private double calculateBreakEndTime(double startTime, DrtShiftBreak shiftBreak) {
        return Math.min(
                shiftBreak.getLatestBreakEndTime(),
                startTime + shiftBreak.getDuration()
        );
    }

    /**
     * Checks if facility timing constraints are satisfied
     */
    private boolean isTimingValid(ShiftBreakContext context) {
        if (context.facility().isEmpty()) {
            return false;
        }

        double facilityArrivalTime = context.facility().get().path().getArrivalTime();
        double latestBreakStartTime = context.shiftBreak().getLatestBreakEndTime() - context.shiftBreak().getDuration();

        return facilityArrivalTime < latestBreakStartTime;
    }

    /**
     * Calculates break timing based on facility arrival and break constraints
     */
    private BreakTimingInfo calculateBreakTiming(ShiftBreakContext context) {
        if (context.facility().isEmpty()) {
            return null;
        }

        double facilityArrivalTime = context.facility().get().path().getArrivalTime();
        double earliestBreakStartTime = context.shiftBreak().getEarliestBreakStartTime();

        // Start break at the later of facility arrival or earliest break time
        double breakStart = Math.max(facilityArrivalTime, earliestBreakStartTime);

        return new BreakTimingInfo(breakStart, breakStart, context.shiftBreak().getLatestBreakEndTime());
    }

    /**
     * Calculates the path from facility to end waypoint
     */
    private VrpPathWithTravelData calculateContinuationPath(ShiftBreakContext context, double breakEndTime) {
        if (context.facility().isEmpty()) {
            return null;
        }

        VrpPathWithTravelData continuationPath = VrpPaths.calcAndCreatePath(
                context.facility().get().path().getToLink(),
                context.end().getLink(),
                breakEndTime,
                router,
                travelTime
        );

        // Check if arrival at end waypoint is in time
        return continuationPath.getArrivalTime() <= context.end().getLatestArrivalTime() ?
                continuationPath : null;
    }

    /**
     * Creates a reservation for the break at the facility
     */
    private Optional<ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>> makeBreakReservation(
            OperationFacility facility,
            DvrpVehicle vehicle,
            BreakTimingInfo breakTimingInfo) {

        return facilityReservationManager.addReservation(
                facility,
                vehicle,
                breakTimingInfo.reservationStart,
                breakTimingInfo.reservationEnd
        );
    }

    /**
     * Cleans up an existing facility reservation and charging if present
     */
    private void cleanupExistingReservation(Id<OperationFacility> facilityId, ShiftBreakTask shiftBreakTask,
                                            DvrpVehicle vehicle, double now) {
        // Clean up reservation
        shiftBreakTask.getReservationId().ifPresent(id ->
                facilityReservationManager.removeReservation(facilityId, id)
        );

        // Clean up charging if present
        shiftBreakTask.getChargingTask().ifPresent(chargingTask -> {
            if (vehicle instanceof EvDvrpVehicle evVehicle) {
                ChargingWithAssignmentLogic chargingLogic = chargingTask.getChargingLogic();
                ElectricVehicle ev = evVehicle.getElectricVehicle();
                if(chargingLogic.isAssigned(ev)) {
                    chargingLogic.unassignVehicle(ev);
                } else if(Stream.concat(chargingLogic.getPluggedVehicles().stream(), chargingLogic.getQueuedVehicles().stream())
                        .map(ChargingLogic.ChargingVehicle::ev)
                        .anyMatch(chargerEv -> chargerEv.getId().equals(ev.getId()))) {
                    chargingLogic.removeVehicle(ev, now);
                }
            }
        });
    }

    /**
     * Modifies the tasks leading to the break depending on the current task type
     * Only called after all validation steps have passed successfully.
     *
     * @return The task that arrives at the facility (typically a drive task)
     */
    private Task modifyTasksForBreak(ShiftBreakContext context, double now) {
        if (context.facility().isEmpty()) {
            return null;
        }

        Task currentTask = context.vehicle().getSchedule().getCurrentTask();

        Task startTask = context.startWaypoint instanceof StopWaypoint ? ((StopWaypoint) context.startWaypoint).getTask(): currentTask;
        // remove tasks between current and break
        removeTasksBetween(startTask, context.end.getTask(), context.schedule());

        if (context.startWaypoint() instanceof StopWaypoint) {
            return handleStopWaypoint(context);
        } else if (DRIVE.isBaseTypeOf(currentTask)) {
            LinkTimePair diversion = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            return diversion != null ?
                    handleDriveWithDiversion(context, currentTask, diversion) :
                    handleDriveWithoutDiversion(context, currentTask, now);
        } else {
            return handleStayTask(context, currentTask);
        }
    }

    /**
     * Handles the case where the current task is a stay task
     */
    private Task handleStayTask(ShiftBreakContext context, Task currentTask) {
        if (context.facility().isEmpty()) {
            return null;
        }

        StayTask stayTask = (StayTask) currentTask;
        VrpPathWithTravelData path = context.facility().get().path();

        // Calculate departure time
        double driveDepartureTime = calculateDriveDepartureTime(context);

        // Modify stay task end time
        stayTask.setEndTime(driveDepartureTime);

        if(path.getFromLink().equals(path.getToLink())) {
            return stayTask;
        }

        // Add drive task to facility
        path = path.withDepartureTime(driveDepartureTime);
        Task driveTask = taskFactory.createDriveTask(context.vehicle(), path, RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE);
        context.schedule().addTask(stayTask.getTaskIdx() + 1, driveTask);

        return driveTask;
    }

    /**
     * Handles the case where the vehicle is currently performing a drive task that can't be diverted
     */
    private Task handleDriveWithoutDiversion(ShiftBreakContext context, Task currentTask, double now) {
        if (context.facility().isEmpty()) {
            return null;
        }

        VrpPathWithTravelData path = context.facility().get().path();
        Task leadingTask = currentTask;

        // Check if we need to wait before driving to the facility
        if (path.getArrivalTime() < context.shiftBreak().getEarliestBreakStartTime()) {
            double driveDepartureTime = calculateDriveDepartureTime(context);

            if (driveDepartureTime > now) {
                // Add wait task
                DrtStayTask waitTask = taskFactory.createStayTask(
                        context.vehicleEntry().vehicle,
                        context.vehicleEntry().start.time,
                        driveDepartureTime,
                        context.startWaypoint().getLink()
                );
                context.schedule().addTask(currentTask.getTaskIdx() + 1, waitTask);

                path = path.withDepartureTime(driveDepartureTime);
                leadingTask = waitTask;
            }
        }

        if(path.getFromLink().equals(path.getToLink())) {
            return leadingTask;
        }

        // Add drive task
        Task driveTask = taskFactory.createDriveTask(context.vehicle(), path, RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE);
        context.schedule().addTask(leadingTask.getTaskIdx() + 1, driveTask);

        return driveTask;
    }

    /**
     * Handles the case where the vehicle is currently performing a drive task that can be diverted
     */
    private Task handleDriveWithDiversion(ShiftBreakContext context, Task currentTask, LinkTimePair diversion) {
        if (!context.facility().isPresent()) {
            return null;
        }

        VrpPathWithTravelData path = context.facility().get().path();
        OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) currentTask.getTaskTracker();

        if (path.getArrivalTime() < context.shiftBreak().getEarliestBreakStartTime()) {
            // Divert to stop at current location (will be followed by wait then drive)
            tracker.divertPath(VrpPaths.createZeroLengthPathForDiversion(diversion));

            double driveDepartureTime = calculateDriveDepartureTime(context);
            Task leadingTask = currentTask;

            // Add wait task if needed
            if (driveDepartureTime > diversion.time) {
                DrtStayTask waitTask = taskFactory.createStayTask(
                        context.vehicle(),
                        currentTask.getEndTime(),
                        driveDepartureTime,
                        context.startWaypoint().getLink()
                );
                context.schedule().addTask(currentTask.getTaskIdx() + 1, waitTask);

                path = path.withDepartureTime(driveDepartureTime);
                leadingTask = waitTask;
            }

            if(path.getFromLink().equals(path.getToLink())) {
                return leadingTask;
            }

            // Add drive task to facility
            Task driveTask = taskFactory.createDriveTask(context.vehicle(), path, RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE);
            context.schedule().addTask(leadingTask.getTaskIdx() + 1, driveTask);
            return driveTask;
        } else {
            // Divert directly to facility (arrival time is after earliest break start)
            tracker.divertPath(path);
            return currentTask;
        }
    }

    /**
     * Handles the case where the break follows a stop waypoint
     */
    private Task handleStopWaypoint(ShiftBreakContext context) {
        if (context.facility().isEmpty()) {
            return null;
        }

        DrtStopTask stopTask = ((StopWaypoint) context.startWaypoint()).getTask();

        // Calculate departure time and prepare path
        double driveDepartureTime = calculateDriveDepartureTime(context);
        Task leadingTask = stopTask;
        VrpPathWithTravelData path = context.facility().get().path();

        // Add wait task if needed
        if (driveDepartureTime > stopTask.getEndTime()) {
            DrtStayTask waitTask = taskFactory.createStayTask(
                    context.vehicleEntry().vehicle,
                    stopTask.getEndTime(),
                    driveDepartureTime,
                    context.startWaypoint().getLink()
            );
            context.schedule().addTask(stopTask.getTaskIdx() + 1, waitTask);

            path = path.withDepartureTime(driveDepartureTime);
            leadingTask = waitTask;

        }

        if(path.getFromLink().equals(path.getToLink())) {
            // no drive necessary
            return leadingTask;
        }

        // Add drive task to facility
        Task driveTask = taskFactory.createDriveTask(
                context.vehicleEntry().vehicle,
                path,
                DrtDriveTask.TYPE
        );
        context.schedule().addTask(leadingTask.getTaskIdx() + 1, driveTask);

        return driveTask;
    }

    /**
     * Calculates when the vehicle should start driving to arrive at the facility at the right time
     */
    private double calculateDriveDepartureTime(ShiftBreakContext context) {
        if (context.facility().isEmpty()) {
            return 0;
        }

        return context.shiftBreak().getEarliestBreakStartTime() -
                context.facility().get().path().getTravelTime();
    }

    /**
     * Helper method to remove tasks between two tasks
     */
    private void removeTasksBetween(Task fromTask, Task toTask, Schedule schedule) {
        List<? extends Task> tasksBetween = new ArrayList<>(Schedules.getTasksBetween(
                fromTask.getTaskIdx() + 1,
                toTask.getTaskIdx(), schedule));
        for (Task task : tasksBetween) {
            schedule.removeTask(task);
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
    
    /**
     * Updates a waiting vehicle with charging capability.
     * This can be called periodically to evaluate if waiting vehicles should start charging.
     * This method handles all charging logic including threshold checks, charger selection,
     * and validation that charging can be completed within the available time.
     * 
     * @param vehicle The shift vehicle
     * @param now Current simulation time
     * @return true if charging was added, false otherwise
     */
    @Override
    public boolean updateWaitingVehicleWithCharging(ShiftDvrpVehicle vehicle, double now) {
        // Check if vehicle is an electric vehicle and charging is enabled
        if (!(vehicle instanceof EvDvrpVehicle) || shiftChargingLogic == null || chargingInfrastructure == null) {
            return false;
        }
        
        // Check schedule status
        Schedule schedule = vehicle.getSchedule();
        if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
            return false;
        }
        
        // Check if current task is a wait task
        Task currentTask = schedule.getCurrentTask();
        if (!(currentTask instanceof WaitForShiftTask waitTask) || 
            currentTask.getStatus() != Task.TaskStatus.STARTED) {
            return false;
        }
        
        // If task already has charging, nothing to do
        if (waitTask.getChargingTask().isPresent()) {
            return false;
        }
        
        // Get vehicle SOC
        EvDvrpVehicle evVehicle = (EvDvrpVehicle) vehicle;
        ElectricVehicle ev = evVehicle.getElectricVehicle();
        double soc = ev.getBattery().getCharge() / ev.getBattery().getCapacity();
        
        // Only charge if SOC is below threshold
        // Use the chargeAtHubThreshold since this is for vehicles waiting at hub
        if (soc > shiftsParams.getChargeAtHubThreshold()) {
            return false;
        }
        
        // Get facility and check for available charger
        OperationFacility facility = facilities.getFacilities().get(waitTask.getFacilityId());
        if (facility == null || facility.getChargers().isEmpty()) {
            return false; // Facility doesn't exist or has no chargers
        }
        
        // Find the best charger with minimum wait time
        String chargerType = shiftsParams.getOutOfShiftChargerType();
        Optional<Charger> selectedCharger = facility.getChargers().stream()
                .map(id -> chargingInfrastructure.getChargers().get(id))
                .filter(charger -> charger != null && chargerType.equals(charger.getChargerType()))
                .min((c1, c2) -> {
                    double waitTime1 = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(c1);
                    double waitTime2 = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(c2);
                    return Double.compare(waitTime1, waitTime2);
                });
        
        if (selectedCharger.isEmpty()) {
            return false; // No suitable charger available
        }
        
        Charger charger = selectedCharger.get();
        Optional<ShiftChargingLogic.ChargerWithStrategy> chargerWithStrategy =
                shiftChargingLogic.findAvailableCharger(facility, ev);
                
        if (chargerWithStrategy.isEmpty()) {
            return false; // No charger with strategy available
        }
        
        ChargingStrategy strategy = chargerWithStrategy.get().strategy();
        if (strategy.isChargingCompleted()) {
            return false; // Vehicle already fully charged
        }
        
        // Calculate time requirements
        double waitTime = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger);
        double chargingTime = strategy.calcRemainingTimeToCharge();
        double endTime = now + waitTime + chargingTime;
        
        // Ensure charging can be completed within the task's time window
        if (endTime >= currentTask.getEndTime()) {
            return false; // Not enough time to complete charging
        }
        
        // Calculate energy to be charged
        double energy = -strategy.calcRemainingEnergyToCharge();
        
        // Use chargeAtHub method to add charging to the vehicle
        return chargeAtHub(waitTask, vehicle, ev, charger, now, energy, strategy);
    }
    
    /**
     * Adds charging capability to a waiting vehicle at a hub facility.
     * This method adds charging to the existing task instead of replacing it.
     * 
     * @param currentTask The current wait task
     * @param vehicle The vehicle
     * @param electricVehicle The electric vehicle
     * @param charger The charger to use
     * @param beginTime The begin time of charging
     * @param energy The energy to charge (negative value means charging)
     * @param strategy The charging strategy
     * @return true if charging was added successfully, false otherwise
     */
    public boolean chargeAtHub(WaitForShiftTask currentTask, ShiftDvrpVehicle vehicle, ElectricVehicle electricVehicle,
                          Charger charger, double beginTime, double energy, ChargingStrategy strategy) {
        // If task already has charging, nothing to do
        if (currentTask.getChargingTask().isPresent()) {
            return false;
        }
        
        // Assign vehicle to charger
        ((ChargingWithAssignmentLogic) charger.getLogic()).assignVehicle(electricVehicle, strategy);
        
        // Create charging task
        ChargingTaskImpl chargingTask = new ChargingTaskImpl(
                EDrtChargingTask.TYPE,
                beginTime,
                currentTask.getEndTime(),
                charger,
                electricVehicle,
                energy,
                strategy);
        
        // Add charging to the existing wait task
        boolean addedCharging = currentTask.addCharging(chargingTask);
        if (!addedCharging) {
            // If charging couldn't be added, remove vehicle from charger
            try {
                charger.getLogic().removeVehicle(electricVehicle, beginTime);
            } catch (IllegalStateException e) {
                // Ignore if vehicle wasn't properly added yet
                logger.warn("Failed to remove vehicle from charger after failed charging addition: {}", e.getMessage());
            }
            return false;
        }
        
        return true;
    }
}