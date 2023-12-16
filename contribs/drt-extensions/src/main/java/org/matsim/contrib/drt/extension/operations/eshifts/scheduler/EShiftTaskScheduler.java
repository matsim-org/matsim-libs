package org.matsim.contrib.drt.extension.operations.eshifts.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtShiftChangeoverTaskImpl;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.ShiftEDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.*;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.evrp.EvDvrpVehicle;
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
public class EShiftTaskScheduler implements ShiftTaskScheduler {

    private final static Logger logger = LogManager.getLogger(EShiftTaskScheduler.class);

    private final TravelTime travelTime;
    private final MobsimTimer timer;
    private final ShiftDrtTaskFactory taskFactory;
    private final LeastCostPathCalculator router;

    private final ShiftsParams shiftsParams;

    private final Network network;
    private final ChargingInfrastructure chargingInfrastructure;

	public EShiftTaskScheduler(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
							   MobsimTimer timer, ShiftDrtTaskFactory taskFactory, ShiftsParams shiftsParams,
							   ChargingInfrastructure chargingInfrastructure, OperationFacilities operationFacilities, Fleet fleet) {
		this.travelTime = travelTime;
		this.timer = timer;
		this.taskFactory = taskFactory;
		this.network = network;
		this.shiftsParams = shiftsParams;
		this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
		this.chargingInfrastructure = chargingInfrastructure;
	}

    public void relocateForBreak(ShiftDvrpVehicle vehicle, OperationFacility breakFacility, DrtShift shift) {
        final Schedule schedule = vehicle.getSchedule();

        final Task currentTask = schedule.getCurrentTask();
        final Link toLink = network.getLinks().get(breakFacility.getLinkId());
        if (currentTask instanceof DriveTask
                && currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
                && currentTask.equals(schedule.getTasks().get(schedule.getTaskCount()-2))) {
            //try to divert/cancel relocation
            LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            VrpPathWithTravelData path;
            if (start != null) {
                path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router,
                        travelTime);
                ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).divertPath(path);

                // remove STAY
                schedule.removeLastTask();
            } else {
                start = new LinkTimePair(((DriveTask) currentTask).getPath().getToLink(), currentTask.getEndTime());
                path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router,
                        travelTime);

                // remove STAY
                schedule.removeLastTask();

                //add drive to break location
                schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)); // add RELOCATE
            }

            double startTime = path.getArrivalTime();
            double endTime = startTime + shift.getBreak().orElseThrow().getDuration();
            double latestDetourArrival = path.getDepartureTime() + path.getTravelTime() + 1.5;
            relocateForBreakImpl(vehicle, startTime, endTime, latestDetourArrival, toLink, shift, breakFacility);

        } else {
            final Task task = schedule.getTasks().get(schedule.getTaskCount() - 1);
            final Link lastLink = ((StayTask) task).getLink();
			double departureTime = task.getBeginTime();

			// @Nico Did I change something here?
			if (schedule.getCurrentTask() == task) {
				departureTime = Math.max(task.getBeginTime(), timer.getTimeOfDay());
			}
			if (lastLink.getId() != breakFacility.getLinkId()) {

                VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink, toLink,
                        departureTime, router,
                        travelTime);
                if (path.getArrivalTime() < vehicle.getServiceEndTime()) {

                    if (schedule.getCurrentTask() == task) {
                        task.setEndTime(timer.getTimeOfDay());
                    } else {
                        // remove STAY
                        schedule.removeLastTask();
                    }


                    //add drive to break location
                    schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE)); // add RELOCATE
                    double startTime = path.getArrivalTime();
                    double endTime = startTime + shift.getBreak().orElseThrow().getDuration();
                    double latestDetourArrival = path.getDepartureTime() + path.getTravelTime() * 1.5;

                    relocateForBreakImpl(vehicle, startTime, endTime, latestDetourArrival, toLink, shift, breakFacility);
                }
            } else {
                double startTime;
                if (schedule.getCurrentTask() == task) {
                    task.setEndTime(timer.getTimeOfDay());
                    startTime = timer.getTimeOfDay();
                } else {
                    // remove STAY
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

        // append SHIFT_BREAK task
		DrtShiftBreak shiftBreak = shift.getBreak().orElseThrow();

		ShiftBreakTask dropoffStopTask;
		ElectricVehicle ev = ((EvDvrpVehicle) vehicle).getElectricVehicle();
		Optional<Charger> charger = charge(breakFacility, ev);
		if (charger.isPresent()) {
            final Charger chargerImpl = charger.get();

			final double waitTime = ChargingEstimations
					.estimateMaxWaitTimeForNextVehicle(chargerImpl);

			if (ev.getBattery().getCharge() / ev.getBattery().getCapacity() > shiftsParams.chargeDuringBreakThreshold ||
			waitTime > 0) {
                dropoffStopTask = taskFactory.createShiftBreakTask(vehicle, startTime,
                        endTime, link, shiftBreak, breakFacility);
            } else {
				double energyCharge = ((BatteryCharging) ev.getChargingPower()).calcEnergyCharged(chargerImpl.getSpecification(), endTime - startTime);
				double totalEnergy = -energyCharge;
                ((ChargingWithAssignmentLogic) chargerImpl.getLogic()).assignVehicle(ev);
                dropoffStopTask = ((ShiftEDrtTaskFactoryImpl) taskFactory).createChargingShiftBreakTask(vehicle,
                        startTime, endTime, link, shiftBreak, chargerImpl, totalEnergy, breakFacility);
            }
        } else {
            dropoffStopTask = taskFactory.createShiftBreakTask(vehicle, startTime,
                    endTime, link, shiftBreak, breakFacility);
        }

        schedule.addTask(dropoffStopTask);

        schedule.addTask(taskFactory.createStayTask(vehicle, endTime, shift.getEndTime(),
                link));

        final double latestTimeConstraintArrival = shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration();

		shiftBreak.schedule(Math.min(latestDetourArrival, latestTimeConstraintArrival));
    }

    private Optional<Charger> charge(OperationFacility breakFacility, ElectricVehicle electricVehicle) {
        if (chargingInfrastructure != null) {
			List<Id<Charger>> chargerIds = breakFacility.getChargers();
			if(!chargerIds.isEmpty()) {
				Optional<Charger> selectedCharger = chargerIds
						.stream()
						.map(id -> chargingInfrastructure.getChargers().get(id))
						.filter(charger -> shiftsParams.breakChargerType.equals(charger.getChargerType()))
						.min((c1, c2) -> {
							final double waitTime = ChargingEstimations
									.estimateMaxWaitTimeForNextVehicle(c1);
							final double waitTime2 = ChargingEstimations
									.estimateMaxWaitTimeForNextVehicle(c2);
							return Double.compare(waitTime, waitTime2);
						});
				if(selectedCharger.isPresent()) {
					if (selectedCharger.get().getLogic().getChargingStrategy().isChargingCompleted(electricVehicle)) {
						return Optional.empty();
					}
				}
				return selectedCharger;
			}
		}
        return Optional.empty();
    }

    public void relocateForShiftChange(DvrpVehicle vehicle, Link link, DrtShift shift, OperationFacility breakFacility) {
        final Schedule schedule = vehicle.getSchedule();

        final Task currentTask = schedule.getCurrentTask();
        if (currentTask instanceof DriveTask
                && currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
                && currentTask.equals(schedule.getTasks().get(schedule.getTaskCount()-2))) {
            //try to divert/cancel relocation
            LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
            VrpPathWithTravelData path;
            if (start != null) {
                path = VrpPaths.calcAndCreatePath(start.link, link, start.time, router,
                        travelTime);
                ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).divertPath(path);

                // remove STAY
                schedule.removeLastTask();
            } else {
                start = new LinkTimePair(((DriveTask) currentTask).getPath().getToLink(), currentTask.getEndTime());
                path = VrpPaths.calcAndCreatePath(start.link, link, start.time, router,
                        travelTime);

                // remove STAY
                schedule.removeLastTask();

                //add drive to break location
                schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)); // add RELOCATE
            }

            final double startTime = Math.max(shift.getEndTime(), path.getArrivalTime());
            final double endTime = startTime + shiftsParams.changeoverDuration;
            if (path.getArrivalTime() > shift.getEndTime()) {
                logger.warn("Shift changeover of shift " + shift.getId() + " will probably be delayed by "
                        + (path.getArrivalTime() - shift.getEndTime()) + " seconds.");
            }
            schedule.addTask(taskFactory.createStayTask(vehicle, path.getArrivalTime(), startTime,
                    path.getToLink()));
            appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);

        } else {

            final List<? extends Task> tasks = schedule.getTasks();
            final DrtStayTask drtStayTask = (DrtStayTask) tasks.get(tasks.size() - 1);
            Link currentLink = drtStayTask.getLink();
            if (currentLink != link) {
                double departureTime = drtStayTask.getBeginTime();

                // @ Nico: Is this ok?
                if (drtStayTask == schedule.getCurrentTask()) {
                    departureTime = Math.max(departureTime, timer.getTimeOfDay());
                }

                VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, link, departureTime, router,
                        travelTime);

                if (schedule.getCurrentTask() == drtStayTask) {
                    drtStayTask.setEndTime(timer.getTimeOfDay());
                } else {
                    // remove STAY
                    schedule.removeLastTask();
                }
                //add drive to break location
                schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)); // add RELOCATE

                final double startTime = Math.max(shift.getEndTime(), path.getArrivalTime());
                final double endTime = startTime + shiftsParams.changeoverDuration;
                if (path.getArrivalTime() > shift.getEndTime()) {
                    logger.warn("Shift changeover of shift " + shift.getId() + " will probably be delayed by "
                            + (path.getArrivalTime() - shift.getEndTime()) + " seconds.");
                }
                schedule.addTask(taskFactory.createStayTask(vehicle, path.getArrivalTime(), startTime,
                        path.getToLink()));
                appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
            } else {
                drtStayTask.setEndTime(shift.getEndTime());
                final double startTime = shift.getEndTime();
                final double endTime = shift.getEndTime() + shiftsParams.changeoverDuration;
                appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
            }
        }
    }

    private void appendShiftChange(DvrpVehicle vehicle, DrtShift shift, OperationFacility breakFacility,
                                   double startTime, double endTime, Link link) {
        Schedule schedule = vehicle.getSchedule();
        ShiftChangeOverTask dropoffStopTask;

        // append SHIFT_CHANGEOVER task

		ElectricVehicle ev = ((EvDvrpVehicle) vehicle).getElectricVehicle();
		Optional<Charger> charger = charge(breakFacility, ev);
		if (charger.isPresent()) {
			Charger chargingImpl = charger.get();

			final double waitTime = ChargingEstimations
					.estimateMaxWaitTimeForNextVehicle(chargingImpl);

			if (ev.getBattery().getCharge() / ev.getBattery().getCapacity() < shiftsParams.chargeDuringBreakThreshold
                    || ((ChargingWithAssignmentLogic) chargingImpl.getLogic()).getAssignedVehicles().contains(ev)
			|| waitTime >0) {
                dropoffStopTask = taskFactory.createShiftChangeoverTask(vehicle, startTime,
                        endTime, link, shift, breakFacility);
            } else {
				double energyCharge = ((BatteryCharging) ev.getChargingPower()).calcEnergyCharged(chargingImpl.getSpecification(), endTime - startTime);
				double totalEnergy = -energyCharge;
                ((ChargingWithAssignmentLogic) chargingImpl.getLogic()).assignVehicle(ev);
                dropoffStopTask = ((ShiftEDrtTaskFactoryImpl) taskFactory).createChargingShiftChangeoverTask(vehicle,
                        startTime, endTime, link, chargingImpl, totalEnergy, shift, breakFacility);
            }
        } else {
            dropoffStopTask = taskFactory.createShiftChangeoverTask(vehicle, startTime,
                    endTime, link, shift, breakFacility);
        }
        schedule.addTask(dropoffStopTask);
        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                link, breakFacility));
    }

    public void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift) {
		Schedule schedule = vehicle.getSchedule();
		StayTask stayTask = (StayTask) schedule.getCurrentTask();
		if (stayTask instanceof WaitForShiftStayTask) {
			((WaitForShiftStayTask) stayTask).getFacility().deregisterVehicle(vehicle.getId());
			stayTask.setEndTime(now);
			schedule.addTask(taskFactory.createStayTask(vehicle, now, shift.getEndTime(), stayTask.getLink()));
		} else {
			throw new IllegalStateException("Vehicle cannot start shift during task:" + stayTask.getTaskType().name());
		}
    }

    public boolean updateShiftChange(ShiftDvrpVehicle vehicle, Link link, DrtShift shift,
                                     LinkTimePair start, OperationFacility facility, Task lastTask) {

        if (start.link != link) {

            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(start.link, link, Math.max(start.time, timer.getTimeOfDay()), router,
                    travelTime);
            //if (path.getArrivalTime() <= shift.getEndTime()) {
                updateShiftChangeImpl(vehicle, path, shift, facility, lastTask);
                return true;
          //  }
        }
        return false;
    }

    private void updateShiftChangeImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath,
                                       DrtShift shift, OperationFacility facility, Task lastTask) {
        Schedule schedule = vehicle.getSchedule();

		Optional<ShiftChangeOverTask> oldChangeOver = ShiftSchedules.getNextShiftChangeover(schedule);
		if(oldChangeOver.isPresent() && oldChangeOver.get() instanceof EDrtShiftChangeoverTaskImpl) {
			if(((EDrtShiftChangeoverTaskImpl) oldChangeOver.get()).getChargingTask() != null) {
				ElectricVehicle ev = ((EvDvrpVehicle) vehicle).getElectricVehicle();
				((EDrtShiftChangeoverTaskImpl) oldChangeOver.get()).getChargingTask().getChargingLogic().unassignVehicle(ev);
			}
		}

		List<Task> copy = new ArrayList<>(schedule.getTasks().subList(lastTask.getTaskIdx() + 1, schedule.getTasks().size()));
        for (Task task : copy) {
            schedule.removeTask(task);
        }
        if (DrtTaskBaseType.getBaseTypeOrElseThrow(lastTask).equals(DRIVE)) {
            ((OnlineDriveTaskTracker) lastTask.getTaskTracker()).divertPath(vrpPath);
        } else {
            //add drive to break location
        	lastTask.setEndTime(vrpPath.getDepartureTime());
            schedule.addTask(taskFactory.createDriveTask(vehicle, vrpPath, RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)); // add RELOCATE
        }

        if (vrpPath.getArrivalTime() < shift.getEndTime()) {
            schedule.addTask(taskFactory.createStayTask(vehicle, vrpPath.getArrivalTime(), shift.getEndTime(),
                    vrpPath.getToLink()));
        }
        // append SHIFT_CHANGEOVER task
        final double endTime = Math.max(shift.getEndTime(), vrpPath.getArrivalTime()) + shiftsParams.changeoverDuration;
        ShiftChangeOverTask dropoffStopTask = taskFactory.createShiftChangeoverTask(vehicle, Math.max(shift.getEndTime(), vrpPath.getArrivalTime()),
                endTime, vrpPath.getToLink(), shift, facility);
        schedule.addTask(dropoffStopTask);

        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), facility));
    }

    public void chargeAtHub(WaitForShiftStayTask currentTask, ShiftDvrpVehicle vehicle,
                            ElectricVehicle electricVehicle, Charger charger, double beginTime,
                            double endTime, double energy) {
        final double initialEndTime = currentTask.getEndTime();
        currentTask.setEndTime(beginTime);
        ((ChargingWithAssignmentLogic) charger.getLogic()).assignVehicle(electricVehicle);
        final WaitForShiftStayTask chargingWaitForShiftStayTask = ((ShiftEDrtTaskFactoryImpl) taskFactory).createChargingWaitForShiftStayTask(vehicle,
                beginTime, endTime, currentTask.getLink(), currentTask.getFacility(), energy, charger);

        final WaitForShiftStayTask waitForShiftStayTask = taskFactory.createWaitForShiftStayTask(vehicle, endTime,
                initialEndTime, currentTask.getLink(), currentTask.getFacility());

        vehicle.getSchedule().addTask(currentTask.getTaskIdx() + 1, chargingWaitForShiftStayTask);
        vehicle.getSchedule().addTask(currentTask.getTaskIdx() + 2, waitForShiftStayTask);
    }
}
