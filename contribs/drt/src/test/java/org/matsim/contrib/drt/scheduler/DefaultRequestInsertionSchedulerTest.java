package org.matsim.contrib.drt.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.dvrp.path.VrpPaths.NODE_TRANSITION_TIME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.stops.MinimumStopDurationAdapter;
import org.matsim.contrib.drt.stops.PrebookingStopTimeCalculator;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTaskUpdater;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultRequestInsertionSchedulerTest {

    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    public static final TravelTime TRAVEL_TIME = (link, time, person, vehicle) -> 10 - NODE_TRANSITION_TIME;
    public static final double STOP_DURATION = 60.;

    private final static double DRIVE_TIME = 10.;

    public static final double CURRENT_TIME = 10;
    public static final double R1_PU_TIME = CURRENT_TIME + DRIVE_TIME + STOP_DURATION;
    public static final double R1_DO_TIME = R1_PU_TIME + DRIVE_TIME;

    //existing on-board request
    public static final double R2_PU_TIME = 0.;
    public static final double R2_DO_TIME = R1_DO_TIME + STOP_DURATION + DRIVE_TIME ;

    public static final int R3_PU_TIME = 20;
    public static final int R3_DO_TIME = 30;

    public static final double ALLOWED_DETOUR = STOP_DURATION + 2 * DRIVE_TIME;

    public static final Id<DvrpVehicle> V_1_ID = Id.create("v1", DvrpVehicle.class);

    private final Link from1 = link("from1");
    private final Link to1 = link("to1");

    private final Link from2 = link("from2");
    private final Link to2 = link("to2");
    private final Link from3 = from1;
    private final Link to3 = link("to3");
    private final DrtRequest existingRequest1 = request("r1", from1, to1, 0., R1_DO_TIME + ALLOWED_DETOUR, R1_PU_TIME, R1_PU_TIME);
    private final DrtRequest existingRequest2 = request("r2", from2, to2, 0., R2_DO_TIME + ALLOWED_DETOUR, R2_PU_TIME, R2_PU_TIME);
    private final DrtRequest newRequest = request("r3", from3, to3, CURRENT_TIME, R3_DO_TIME + ALLOWED_DETOUR, R3_PU_TIME, R3_PU_TIME);
	private final IntegerLoadType integerLoadType = new IntegerLoadType("persons");
    private static final String mode = "DRT_MODE";


    @Test
    public void testInsertion() {
        Link startLink = link("start");
        FleetSpecificationImpl fleetSpecification = new FleetSpecificationImpl();
        fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(V_1_ID)
                .startLinkId(startLink.getId())
                .capacity(6)
                .serviceBeginTime(0)
                .serviceEndTime(1000)
                .build()
        );

        Fleet fleet = Fleets.createDefaultFleet(fleetSpecification, dvrpVehicleSpecification -> startLink);
        MobsimTimer timer = new MobsimTimer(1);
        timer.setTime(CURRENT_TIME);
        DefaultRequestInsertionScheduler insertionScheduler = getDefaultRequestInsertionScheduler(fleet, timer);


        DvrpVehicle vehicle = fleet.getVehicles().get(V_1_ID);

        // vehicle schedule
        Task task = vehicle.getSchedule().nextTask();

        vehicle.getSchedule().getCurrentTask().setEndTime(CURRENT_TIME);
        LeastCostPathCalculator.Path path = new LeastCostPathCalculator.Path(null, List.of(), 0., 0);
        VrpPathWithTravelData vrpPath = VrpPaths.createPath(startLink, existingRequest1.getFromLink(), CURRENT_TIME, path, TRAVEL_TIME);
        vehicle.getSchedule().addTask(new DrtDriveTask(vrpPath, DrtDriveTask.TYPE));

        DefaultDrtStopTask stopTask0 = new DefaultDrtStopTask(R1_PU_TIME - STOP_DURATION, R1_PU_TIME, from1);
        AcceptedDrtRequest acceptedExistingRequest = AcceptedDrtRequest.createFromOriginalRequest(existingRequest1);
        stopTask0.addPickupRequest(acceptedExistingRequest);
        vehicle.getSchedule().addTask(stopTask0);

        VrpPathWithTravelData vrpPath2 = VrpPaths.createPath(existingRequest1.getFromLink(), existingRequest1.getToLink(), stopTask0.getEndTime(), path, TRAVEL_TIME);
        vehicle.getSchedule().addTask(new DrtDriveTask(vrpPath2, DrtDriveTask.TYPE));

        DefaultDrtStopTask stopTask1 = new DefaultDrtStopTask(R1_DO_TIME, R1_DO_TIME + STOP_DURATION, to1);
        stopTask1.addDropoffRequest(acceptedExistingRequest);
        vehicle.getSchedule().addTask(stopTask1);

        LeastCostPathCalculator.Path longPath = new LeastCostPathCalculator.Path(null, List.of(), 200, 0);
        VrpPathWithTravelData vrpPath3 = VrpPaths.createPath(existingRequest1.getToLink(), existingRequest2.getToLink(), stopTask1.getEndTime(), longPath, TRAVEL_TIME);
        vehicle.getSchedule().addTask(new DrtDriveTask(vrpPath3, DrtDriveTask.TYPE));

        DefaultDrtStopTask stopTask2 = new DefaultDrtStopTask(stopTask1.getEndTime() + longPath.travelTime + 10., stopTask1.getEndTime() + longPath.travelTime + STOP_DURATION, to2);
        AcceptedDrtRequest acceptedExistingRequest2 = AcceptedDrtRequest.createFromOriginalRequest(existingRequest2);
        stopTask2.addDropoffRequest(acceptedExistingRequest2);
        vehicle.getSchedule().addTask(stopTask2);


        // vehicle entry
        Waypoint.Start start = start(null, CURRENT_TIME, startLink, integerLoadType.fromInt(1));//not a STOP -> pickup cannot be appended
        Waypoint.Stop stop0 = stop(stopTask0, integerLoadType.fromInt(2));
        Waypoint.Stop stop1 = stop(stopTask1, integerLoadType.fromInt(1));
        Waypoint.Stop stop2 = stop(stopTask2, integerLoadType.getEmptyLoad());
        var vehicleEntry = entry(vehicle, start, stop0, stop1, stop2);

        InsertionWithDetourData.InsertionDetourData detour = detourData(0, 10, 10, 10.);
        InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo = detourTimeInfo();
        InsertionWithDetourData insertion = insertion(vehicleEntry, 1, 2, detour, newRequest,
                detourTimeInfo);

        RequestInsertionScheduler.PickupDropoffTaskPair pickupDropoffTaskPair =
                insertionScheduler.scheduleRequest(AcceptedDrtRequest.createFromOriginalRequest(newRequest), insertion);

        ScheduleInfo actualScheduleInfo = getScheduleInfo(vehicle.getSchedule());
        ScheduleInfo expectedScheduleInfo = ScheduleInfo.newBuilder()
                .addTask(new ScheduleBuilder.TaskInfo(0, 0, 10, Task.TaskStatus.STARTED,
                        DrtStayTask.TYPE, Set.of(), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(1, 10, 20, Task.TaskStatus.PLANNED,
                        DrtDriveTask.TYPE, Set.of(), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(2, 20, 80, Task.TaskStatus.PLANNED,
                        DefaultDrtStopTask.TYPE, Set.of(existingRequest1.getId(), newRequest.getId()), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(3, 80, 90, Task.TaskStatus.PLANNED,
                        DrtDriveTask.TYPE, Set.of(), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(4, 90, 150, Task.TaskStatus.PLANNED,
                        DefaultDrtStopTask.TYPE, Set.of(), Set.of(existingRequest1.getId())))
                .addTask(new ScheduleBuilder.TaskInfo(5, 150, 170, Task.TaskStatus.PLANNED,
                        DrtDriveTask.TYPE, Set.of(), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(6, 170, 230, Task.TaskStatus.PLANNED,
                        DefaultDrtStopTask.TYPE, Set.of(), Set.of(newRequest.getId())))
                .addTask(new ScheduleBuilder.TaskInfo(7, 230, 250, Task.TaskStatus.PLANNED,
                        DrtDriveTask.TYPE, Set.of(), Set.of()))
                .addTask(new ScheduleBuilder.TaskInfo(8, 250, 310, Task.TaskStatus.PLANNED,
                        DefaultDrtStopTask.TYPE, Set.of(), Set.of(existingRequest2.getId())))
                .build();

        compareTwoSchedules(actualScheduleInfo, expectedScheduleInfo);


    }

    private InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo() {
        return new InsertionDetourTimeCalculator.DetourTimeInfo(
                new InsertionDetourTimeCalculator.PickupDetourInfo(R1_PU_TIME + STOP_DURATION, 0.),
                new InsertionDetourTimeCalculator.DropoffDetourInfo(R1_PU_TIME + STOP_DURATION + DRIVE_TIME, 20.)
        );
    }

    private static DefaultRequestInsertionScheduler getDefaultRequestInsertionScheduler(Fleet fleet, MobsimTimer timer) {
        MinimumStopDurationAdapter stopDuration = new MinimumStopDurationAdapter(new PrebookingStopTimeCalculator(StaticPassengerStopDurationProvider.of(STOP_DURATION, 0.0)), 60.);
        ScheduleTimingUpdater scheduleTimingUpdater = new ScheduleTimingUpdater(timer, new DrtStayTaskEndTimeCalculator(stopDuration), DriveTaskUpdater.NOOP);
        DefaultRequestInsertionScheduler insertionScheduler = new DefaultRequestInsertionScheduler(fleet, timer, TRAVEL_TIME, scheduleTimingUpdater,
                new DrtTaskFactoryImpl(), stopDuration, true);
        return insertionScheduler;
    }

    private InsertionWithDetourData.InsertionDetourData detourData(double toPickupTT, double fromPickupTT, double toDropoffTT,
                                                                   double fromDropoffTT) {
        var toPickupDetour = new OneToManyPathSearch.PathData(new LeastCostPathCalculator.Path(null, List.of(), toPickupTT, 0), 0);
        var fromPickupDetour = new OneToManyPathSearch.PathData(new LeastCostPathCalculator.Path(null, List.of(), fromPickupTT, 0), 0);
        var toDropoffDetour = new OneToManyPathSearch.PathData(new LeastCostPathCalculator.Path(null, List.of(), toDropoffTT, 0), 0);
        var fromDropoffDetour = new OneToManyPathSearch.PathData(new LeastCostPathCalculator.Path(null, List.of(), fromDropoffTT, 0), 0);
        return new InsertionWithDetourData.InsertionDetourData(toPickupDetour, fromPickupDetour, toDropoffDetour, fromDropoffDetour);
    }

    private InsertionWithDetourData insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx,
                                              InsertionWithDetourData.InsertionDetourData detour,
                                              DrtRequest drtRequest, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
        return new InsertionWithDetourData(
                new InsertionGenerator.Insertion(drtRequest, entry, pickupIdx, dropoffIdx),
                detour,
                detourTimeInfo
        );
    }


    private VehicleEntry entry(DvrpVehicle vehicle, Waypoint.Start start, Waypoint.Stop... stops) {
        List<Double> precedingStayTimes = Collections.nCopies(stops.length, 0.0);
        return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), null, precedingStayTimes, 0);
    }

    private Waypoint.Start start(Task task, double time, Link link, DvrpLoad occupancy) {
        return new Waypoint.Start(task, link, time, occupancy);
    }

    private Waypoint.Stop stop(DefaultDrtStopTask stopTask, DvrpLoad outgoingOccupancy) {
        return new Waypoint.Stop(stopTask, outgoingOccupancy, integerLoadType);
    }


    private DrtRequest request(String id, Link fromLink, Link toLink, double submissionTime,
                               double latestArrivalTime, double earliestStartTime, double latestStartTime) {
        return DrtRequest.newBuilder()
                .id(Id.create(id, Request.class))
                .passengerIds(List.of(Id.createPersonId(id)))
                .submissionTime(submissionTime)
                .latestArrivalTime(latestArrivalTime)
                .latestStartTime(latestStartTime)
                .earliestStartTime(earliestStartTime)
                .fromLink(fromLink)
                .toLink(toLink)
                .mode(mode)
                .build();
    }

    private Link link(String id) {
        return new FakeLink(Id.createLinkId(id));
    }



    private record ScheduleInfo(List<ScheduleBuilder.TaskInfo> taskInfos) {
        public static ScheduleBuilder newBuilder() {
            return new ScheduleBuilder();
        }
    }

    private static ScheduleInfo getScheduleInfo(Schedule schedule) {
        ScheduleBuilder scheduleBuilder = ScheduleInfo.newBuilder();
        for (Task task : schedule.getTasks()) {
            scheduleBuilder.addTask(new ScheduleBuilder.TaskInfo(task.getTaskIdx(), task.getBeginTime(),
                    task.getEndTime(), task.getStatus(), task.getTaskType(),
                    task instanceof DrtStopTask ? ((DrtStopTask) task).getPickupRequests().keySet(): Set.of(),
                    task instanceof DrtStopTask ? ((DrtStopTask) task).getDropoffRequests().keySet(): Set.of()
                    ));
        }
        return scheduleBuilder.build();
    }


    private static class ScheduleBuilder {

        private record TaskInfo(int taskIdx, double beginTime, double endTime, Task.TaskStatus status,
                                Task.TaskType taskType, Set<Id<Request>> puRequests, Set<Id<Request>> doRequests) {}

        private ScheduleBuilder(){}


        private final List<TaskInfo> taskInfos = new ArrayList<>();

        public ScheduleBuilder addTask(TaskInfo taskInfo) {
            taskInfos.add(taskInfo.taskIdx, taskInfo);
            return this;
        }

        public ScheduleInfo build() {
            return new ScheduleInfo(taskInfos);
        }
    }

    private static void compareTwoSchedules(ScheduleInfo actualScheduleInfo, ScheduleInfo expectedScheduleInfo) {
        assertThat(actualScheduleInfo).usingRecursiveComparison().isEqualTo(expectedScheduleInfo);
    }
}
