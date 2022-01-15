package org.matsim.contrib.drt.extension.shifts.scheduler;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskType;
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
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftTaskScheduler {

    private final static Logger logger = Logger.getLogger(ShiftTaskScheduler.class);

    public static final DrtTaskType RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_BREAK", DRIVE);
    public static final DrtTaskType RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_CHANGEOVER", DRIVE);

    private final TravelTime travelTime;
    private final MobsimTimer timer;
    private final ShiftDrtTaskFactory taskFactory;
    private final LeastCostPathCalculator router;

    private final ShiftDrtConfigGroup shiftConfig;
	private final OperationFacilities operationFacilities;
	private final Fleet fleet;

	private final Network network;

	public ShiftTaskScheduler(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
							  MobsimTimer timer, ShiftDrtTaskFactory taskFactory, ShiftDrtConfigGroup shiftConfig,
							  OperationFacilities operationFacilities, Fleet fleet) {
		this.travelTime = travelTime;
		this.timer = timer;
		this.taskFactory = taskFactory;
		this.network = network;
		this.shiftConfig = shiftConfig;
		this.operationFacilities = operationFacilities;
		this.fleet = fleet;
		this.router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	public void initSchedules() {
		final Map<Id<Link>, List<OperationFacility>> facilitiesByLink = operationFacilities.getDrtOperationFacilities().values().stream().collect(Collectors.groupingBy(Facility::getLinkId));
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			try {
				final OperationFacility operationFacility = facilitiesByLink.get(veh.getStartLink().getId()).stream().findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
				veh.getSchedule()
						.addTask(taskFactory.createWaitForShiftStayTask(veh, veh.getServiceBeginTime(), veh.getServiceEndTime(),
								veh.getStartLink(), operationFacility));
				operationFacility.register(veh.getId());
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
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
            double relocationDuration = path.getDepartureTime() + path.getTravelTime();
            relocateForBreakImpl(vehicle, startTime, endTime, relocationDuration, toLink, shift, breakFacility);

        } else {
            final Task task = schedule.getTasks().get(schedule.getTaskCount() - 1);
            final Link lastLink = ((StayTask) task).getLink();
            if (lastLink.getId() != breakFacility.getLinkId()) {
                double departureTime = task.getBeginTime();

                // @Nico Did I change something here?
                if (schedule.getCurrentTask() == task) {
                    departureTime = Math.max(task.getBeginTime(), timer.getTimeOfDay());
                }

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
                    double relocationDuration = path.getDepartureTime() + path.getTravelTime();

                    relocateForBreakImpl(vehicle, startTime, endTime, relocationDuration, toLink, shift, breakFacility);
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
                double relocationDuration = 0;
                relocateForBreakImpl(vehicle, startTime, endTime, relocationDuration, toLink, shift, breakFacility);
            }
        }
    }

    private void relocateForBreakImpl(ShiftDvrpVehicle vehicle, double startTime, double endTime,
                                      double relocationDuration, Link link, DrtShift shift,
                                      OperationFacility breakFacility) {
        Schedule schedule = vehicle.getSchedule();

        // append SHIFT_BREAK task

		DrtShiftBreak shiftBreak = shift.getBreak().orElseThrow();
		ShiftBreakTask dropoffStopTask = taskFactory.createShiftBreakTask(vehicle, startTime,
                    endTime, link, shiftBreak, breakFacility);

        schedule.addTask(dropoffStopTask);

        schedule.addTask(taskFactory.createStayTask(vehicle, endTime, shift.getEndTime(),
                link));

        double latestDetourArrival = relocationDuration * 1.5 + 10;
        final double latestTimeConstraintArrival = shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration();

		shiftBreak.schedule(Math.min(latestDetourArrival, latestTimeConstraintArrival));
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
            final double endTime = startTime + shiftConfig.getChangeoverDuration();
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
                final double endTime = startTime + shiftConfig.getChangeoverDuration();
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
                final double endTime = shift.getEndTime() + shiftConfig.getChangeoverDuration();
                appendShiftChange(vehicle, shift, breakFacility, startTime, endTime, link);
            }
        }
    }

    private void appendShiftChange(DvrpVehicle vehicle, DrtShift shift, OperationFacility breakFacility,
                                   double startTime, double endTime, Link link) {
        Schedule schedule = vehicle.getSchedule();
        ShiftChangeOverTask dropoffStopTask = taskFactory.createShiftChangeoverTask(vehicle, startTime,
                    endTime, link, shift, breakFacility);
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
        final double endTime = Math.max(shift.getEndTime(), vrpPath.getArrivalTime()) + shiftConfig.getChangeoverDuration();
        ShiftChangeOverTask dropoffStopTask = taskFactory.createShiftChangeoverTask(vehicle, Math.max(shift.getEndTime(), vrpPath.getArrivalTime()),
                endTime, vrpPath.getToLink(), shift, facility);
        schedule.addTask(dropoffStopTask);

        schedule.addTask(taskFactory.createWaitForShiftStayTask(vehicle, endTime, vehicle.getServiceEndTime(),
                vrpPath.getToLink(), facility));
    }
}
