package org.matsim.contrib.drt.optimizer.rebalancing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * @author nkuehnel / MOIA
 */
public class EmptyVehicleRelocatorTest {

    @Test
    void relocatesAndReturnsBeforeFixedStopAtDifferentLink_whenSlackIsSufficient() {
        NetworkAndLinks networkAndLinks = getNetworkAndLinks();

        FreespeedTravelTimeAndDisutility freespeedTravelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        // --- Mobsim time "now" = 1000s
        MobsimTimer timer = Mockito.mock(MobsimTimer.class);
        when(timer.getTimeOfDay()).thenReturn(1000.);

        // --- Vehicle & schedule
        double serviceBegin = 0.0;
        double serviceEnd = 10_000.0;

        ImmutableDvrpVehicleSpecification spec = getImmutableDvrpVehicleSpecification(networkAndLinks, serviceBegin, serviceEnd);

        DvrpVehicle veh = new DvrpVehicleImpl(spec, networkAndLinks.lAB());
        Schedule schedule = veh.getSchedule();
        DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();

        // Current STAY at AB
        DrtStayTask currentStay = taskFactory.createStayTask(veh, 1000.0, 2400, networkAndLinks.lAB());
        schedule.addTask(currentStay);

        // DRIVE to existing fixed/prebooked stop at BC
        LeastCostPathCalculator lcpc = new SpeedyALTFactory().createPathCalculator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility);
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(networkAndLinks.lAB(), networkAndLinks.lBC(), currentStay.getEndTime(), lcpc, freespeedTravelTimeAndDisutility);
        DrtDriveTask driveTask = taskFactory.createDriveTask(veh, path, DrtDriveTask.TYPE);
        schedule.addTask(driveTask);

        // Upcoming fixed STOP at BC
        DrtStopTask fixedStop = taskFactory.createStopTask(veh, path.getArrivalTime(), path.getArrivalTime() + 10, networkAndLinks.lBC());
        schedule.addTask(fixedStop);

        // final STAY at BC
        DrtStayTask finalStay = taskFactory.createStayTask(veh, fixedStop.getEndTime(), veh.getServiceEndTime(), networkAndLinks.lBC());
        schedule.addTask(finalStay);

        //trigger start of schedule
        schedule.nextTask();

        // Sanity preconditions
        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(schedule.getCurrentTask()).isSameAs(currentStay);
        Assertions.assertThat(schedule.getTasks().get(2)).isSameAs(fixedStop);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        EmptyVehicleRelocator relocator = new EmptyVehicleRelocator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility, timer, taskFactory);

        // Try to relocate to CD
        relocator.relocateVehicle(veh, networkAndLinks.lCD(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

        // --- Verify schedule shape:
        // After relocation we expect:
        // 0: STAY(AB) shortened to depart at 1000 (relocate departure)
        // 1: DRIVE(RELOCATE)
        // 2: STAY(CD)
        // 3: DRIVE(CD-> BC)
        // 4: STOP(BC)
        // 5: STAY(BC)

        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(6);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        // 0: current STAY truncated
        Task t0 = schedule.getTasks().get(0);
        Assertions.assertThat(t0).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t0.getBeginTime()).isEqualTo(1000.0);
        Assertions.assertThat(t0.getEndTime()).isEqualTo(1000.0);

        // 1: relocate DRIVE to CD
        Task t1 = schedule.getTasks().get(1);
        Assertions.assertThat(t1).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getFromLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getToLink()).isSameAs(networkAndLinks.lCD());
        Assertions.assertThat(t1.getBeginTime()).isEqualTo(1000);
        Assertions.assertThat(t1.getEndTime()).isEqualTo(1201);

        // 2: STAY at CD for slack
        Task t2 = schedule.getTasks().get(2);
        Assertions.assertThat(t2).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t2.getBeginTime()).isEqualTo(1201);
        Assertions.assertThat(t2.getEndTime()).isEqualTo(2200);
        Assertions.assertThat(((DrtStayTask)t2).getLink()).isSameAs(networkAndLinks.lCD());

        // 3: follow-up DRIVE B->C
        Task t3 = schedule.getTasks().get(3);
        Assertions.assertThat(t3).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t3).getPath().getFromLink()).isSameAs(networkAndLinks.lCD());
        Assertions.assertThat(((DrtDriveTask) t3).getPath().getToLink()).isSameAs(networkAndLinks.lBC());
        Assertions.assertThat(t3.getBeginTime()).isEqualTo(2200);
        Assertions.assertThat(t3.getEndTime()).isEqualTo(2501);

        // 4: original STOP at BC
        Task t4 = schedule.getTasks().get(4);
        Assertions.assertThat(t4).isSameAs(fixedStop);
        Assertions.assertThat(t4.getBeginTime()).isEqualTo(2501);
        Assertions.assertThat(t4.getEndTime()).isEqualTo(2511);
        Assertions.assertThat(((DrtStopTask)t4).getLink()).isSameAs(networkAndLinks.lBC());


        // 5: final STAY at BC
        Task t5 = schedule.getTasks().get(5);
        Assertions.assertThat(t5).isSameAs(finalStay);
        Assertions.assertThat(t5.getBeginTime()).isEqualTo(2511);
        Assertions.assertThat(t5.getEndTime()).isEqualTo(veh.getServiceEndTime());
        Assertions.assertThat(((DrtStayTask)t5).getLink()).isSameAs(networkAndLinks.lBC());
    }


    @Test
    void relocatesAndReturnsBeforeFixedStopAtSameLink_whenSlackIsSufficient() {
        NetworkAndLinks networkAndLinks = getNetworkAndLinks();

        FreespeedTravelTimeAndDisutility freespeedTravelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        // --- Mobsim time "now" = 1000s
        MobsimTimer timer = Mockito.mock(MobsimTimer.class);
        when(timer.getTimeOfDay()).thenReturn(1000.);

        double serviceBegin = 0.0;
        double serviceEnd = 10_000.0;

        ImmutableDvrpVehicleSpecification spec = getImmutableDvrpVehicleSpecification(networkAndLinks, serviceBegin, serviceEnd);

        DvrpVehicle veh = new DvrpVehicleImpl(spec, networkAndLinks.lAB());
        Schedule schedule = veh.getSchedule();
        DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();

        // Current STAY at AB
        DrtStayTask currentStay = taskFactory.createStayTask(veh, 1000.0, 2400, networkAndLinks.lAB());
        schedule.addTask(currentStay);

        // Upcoming fixed STOP at AB
        DrtStopTask fixedStop = taskFactory.createStopTask(veh, 2400, 2410, networkAndLinks.lAB());
        schedule.addTask(fixedStop);

        // final STAY at AB
        DrtStayTask finalStay = taskFactory.createStayTask(veh, fixedStop.getEndTime(), veh.getServiceEndTime(), networkAndLinks.lAB());
        schedule.addTask(finalStay);

        //trigger start of schedule
        schedule.nextTask();

        // Sanity preconditions
        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(3);
        Assertions.assertThat(schedule.getCurrentTask()).isSameAs(currentStay);
        Assertions.assertThat(schedule.getTasks().get(1)).isSameAs(fixedStop);
        Assertions.assertThat(verifyTaskContinuity(schedule));

        EmptyVehicleRelocator relocator = new EmptyVehicleRelocator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility, timer, taskFactory);

        // Try to relocate to CD
        relocator.relocateVehicle(veh, networkAndLinks.lCD(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

        // --- Verify schedule shape:
        // After relocation we expect:
        // 0: STAY(AB) shortened to depart at 1000
        // 1: DRIVE(RELOCATE) AB-> CD
        // 2: STAY(CD)
        // 3: DRIVE(CD->AB)
        // 4: STOP(AB) (unchanged begin)

        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(6);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        // 0: current STAY truncated
        Task t0 = schedule.getTasks().get(0);
        Assertions.assertThat(t0).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t0.getBeginTime()).isEqualTo(1000.0);
        Assertions.assertThat(t0.getEndTime()).isEqualTo(1000.0);

        // 1: relocate DRIVE to CD
        Task t1 = schedule.getTasks().get(1);
        Assertions.assertThat(t1).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getFromLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getToLink()).isSameAs(networkAndLinks.lCD());
        Assertions.assertThat(t1.getBeginTime()).isEqualTo(1000);
        Assertions.assertThat(t1.getEndTime()).isEqualTo(1201);

        // 2: STAY at CD for slack
        Task t2 = schedule.getTasks().get(2);
        Assertions.assertThat(t2).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t2.getBeginTime()).isEqualTo(1201);
        Assertions.assertThat(t2.getEndTime()).isEqualTo(1999);
        Assertions.assertThat(((DrtStayTask)t2).getLink()).isSameAs(networkAndLinks.lCD());


        // 3: follow-up DRIVE CD->AB
        Task t3 = schedule.getTasks().get(3);
        Assertions.assertThat(t3).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t3).getPath().getFromLink()).isSameAs(networkAndLinks.lCD());
        Assertions.assertThat(((DrtDriveTask) t3).getPath().getToLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(t3.getBeginTime()).isEqualTo(1999);
        Assertions.assertThat(t3.getEndTime()).isEqualTo(2400);

        // 4: original STOP at AB
        Task t4 = schedule.getTasks().get(4);
        Assertions.assertThat(t4).isSameAs(fixedStop);
        Assertions.assertThat(t4.getBeginTime()).isEqualTo(2400);
        Assertions.assertThat(t4.getEndTime()).isEqualTo(2410);
        Assertions.assertThat(((DrtStopTask)t4).getLink()).isSameAs(networkAndLinks.lAB());


        // 5: final STAY at AB
        Task t5 = schedule.getTasks().get(5);
        Assertions.assertThat(t5).isSameAs(finalStay);
        Assertions.assertThat(t5.getBeginTime()).isEqualTo(2410);
        Assertions.assertThat(t5.getEndTime()).isEqualTo(veh.getServiceEndTime());
        Assertions.assertThat(((DrtStayTask)t5).getLink()).isSameAs(networkAndLinks.lAB());

    }

    @Test
    void relocatesToFixedStopAtSameLink_whenSlackIsSufficient() {
        NetworkAndLinks networkAndLinks = getNetworkAndLinks();

        FreespeedTravelTimeAndDisutility freespeedTravelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        // --- Mobsim time "now" = 1000s
        MobsimTimer timer = Mockito.mock(MobsimTimer.class);
        when(timer.getTimeOfDay()).thenReturn(1000.);

        // --- Vehicle & schedule
        double serviceBegin = 0.0;
        double serviceEnd = 10_000.0;

        ImmutableDvrpVehicleSpecification spec = getImmutableDvrpVehicleSpecification(networkAndLinks, serviceBegin, serviceEnd);

        DvrpVehicle veh = new DvrpVehicleImpl(spec, networkAndLinks.lAB());
        Schedule schedule = veh.getSchedule();
        DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();

        // Current STAY at AB
        DrtStayTask currentStay = taskFactory.createStayTask(veh, 1000.0, 2400, networkAndLinks.lAB());
        schedule.addTask(currentStay);

        // DRIVE to existing fixed/prebooked stop at CD
        LeastCostPathCalculator lcpc = new SpeedyALTFactory().createPathCalculator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility);
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(networkAndLinks.lAB(), networkAndLinks.lCD(), currentStay.getEndTime(), lcpc, freespeedTravelTimeAndDisutility);
        DrtDriveTask driveTask = taskFactory.createDriveTask(veh, path, DrtDriveTask.TYPE);
        schedule.addTask(driveTask);

        // Upcoming fixed STOP at CD
        DrtStopTask fixedStop = taskFactory.createStopTask(veh, path.getArrivalTime(), path.getArrivalTime() + 10, networkAndLinks.lCD());
        schedule.addTask(fixedStop);

        // final STAY at CD
        DrtStayTask finalStay = taskFactory.createStayTask(veh, fixedStop.getEndTime(), veh.getServiceEndTime(), networkAndLinks.lCD());
        schedule.addTask(finalStay);

        //trigger start of schedule
        schedule.nextTask();

        // Sanity preconditions
        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(schedule.getCurrentTask()).isSameAs(currentStay);
        Assertions.assertThat(schedule.getTasks().get(2)).isSameAs(fixedStop);
        Assertions.assertThat(verifyTaskContinuity(schedule));

        EmptyVehicleRelocator relocator = new EmptyVehicleRelocator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility, timer, taskFactory);

        // Try to relocate to CD
        relocator.relocateVehicle(veh, networkAndLinks.lCD(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

        // --- Verify schedule shape:
        // After relocation we expect:
        // 0: STAY(AB) shortened to depart at 1000 (relocate departure)
        // 1: DRIVE(RELOCATE) AB->CD
        // 2: STAY(CD)
        // 3: STOP(CD)
        // 4: STAY(CD)

        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(5);
        Assertions.assertThat(verifyTaskContinuity(schedule));

        // 0: current STAY truncated
        Task t0 = schedule.getTasks().get(0);
        Assertions.assertThat(t0).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t0.getBeginTime()).isEqualTo(1000.0);
        Assertions.assertThat(t0.getEndTime()).isEqualTo(1000.0);

        // 1: relocate DRIVE to CD
        Task t1 = schedule.getTasks().get(1);
        Assertions.assertThat(t1).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getFromLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getToLink()).isSameAs(networkAndLinks.lCD());
        Assertions.assertThat(t1.getBeginTime()).isEqualTo(1000);
        Assertions.assertThat(t1.getEndTime()).isEqualTo(1201);

        // 2: STAY at CD for slack
        Task t2 = schedule.getTasks().get(2);
        Assertions.assertThat(t2).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t2.getBeginTime()).isEqualTo(1201);
        Assertions.assertThat(t2.getEndTime()).isEqualTo(2601);
        Assertions.assertThat(((DrtStayTask)t2).getLink()).isSameAs(networkAndLinks.lCD());


        // 3: original STOP at CD
        Task t3 = schedule.getTasks().get(3);
        Assertions.assertThat(t3).isSameAs(fixedStop);
        Assertions.assertThat(t3.getBeginTime()).isEqualTo(2601);
        Assertions.assertThat(t3.getEndTime()).isEqualTo(2611);
        Assertions.assertThat(((DrtStopTask)t3).getLink()).isSameAs(networkAndLinks.lCD());


        // 4: final STAY at CD
        Task t4 = schedule.getTasks().get(4);
        Assertions.assertThat(t4).isSameAs(finalStay);
        Assertions.assertThat(t4.getBeginTime()).isEqualTo(2611);
        Assertions.assertThat(t4.getEndTime()).isEqualTo(veh.getServiceEndTime());
        Assertions.assertThat(((DrtStayTask)t4).getLink()).isSameAs(networkAndLinks.lCD());
    }

    @Test
    void relocatesAndReturnsBeforeFixedStopAtDifferentLink_whenSlackIsInSufficientForReturn() {
        NetworkAndLinks networkAndLinks = getNetworkAndLinks();

        FreespeedTravelTimeAndDisutility freespeedTravelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        // --- Mobsim time "now" = 1000s
        MobsimTimer timer = Mockito.mock(MobsimTimer.class);
        when(timer.getTimeOfDay()).thenReturn(1000.);

        // --- Vehicle & schedule
        double serviceBegin = 0.0;
        double serviceEnd = 10_000.0;

        ImmutableDvrpVehicleSpecification spec = getImmutableDvrpVehicleSpecification(networkAndLinks, serviceBegin, serviceEnd);

        DvrpVehicle veh = new DvrpVehicleImpl(spec, networkAndLinks.lAB());
        Schedule schedule = veh.getSchedule();
        DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();

        // Current STAY at AB, short slack
        DrtStayTask currentStay = taskFactory.createStayTask(veh, 1000.0, 1200, networkAndLinks.lAB());
        schedule.addTask(currentStay);

        // DRIVE to existing fixed/prebooked stop at BC
        LeastCostPathCalculator lcpc = new SpeedyALTFactory().createPathCalculator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility);
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(networkAndLinks.lAB(), networkAndLinks.lBC(), currentStay.getEndTime(), lcpc, freespeedTravelTimeAndDisutility);
        DrtDriveTask driveTask = taskFactory.createDriveTask(veh, path, DrtDriveTask.TYPE);
        schedule.addTask(driveTask);

        // Upcoming fixed STOP at BC
        DrtStopTask fixedStop = taskFactory.createStopTask(veh, path.getArrivalTime(), path.getArrivalTime() + 10, networkAndLinks.lBC());
        schedule.addTask(fixedStop);

        // final STAY at BC
        DrtStayTask finalStay = taskFactory.createStayTask(veh, fixedStop.getEndTime(), veh.getServiceEndTime(), networkAndLinks.lBC());
        schedule.addTask(finalStay);

        //trigger start of schedule
        schedule.nextTask();

        // Sanity preconditions
        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(schedule.getCurrentTask()).isSameAs(currentStay);
        Assertions.assertThat(schedule.getTasks().get(2)).isSameAs(fixedStop);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        EmptyVehicleRelocator relocator = new EmptyVehicleRelocator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility, timer, taskFactory);

        // Try to relocate to CD
        relocator.relocateVehicle(veh, networkAndLinks.lCD(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

        // --- Verify schedule shape:
        // After failed relocation we expect (unchanged):
        // 0: STAY(AB)
        // 1: DRIVE AB->BC
        // 2: STOP(BC)
        // 3: STAY(BC)

        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        // 0: current STAY left as before
        Task t0 = schedule.getTasks().get(0);
        Assertions.assertThat(t0).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t0.getBeginTime()).isEqualTo(1000.0);
        Assertions.assertThat(t0.getEndTime()).isEqualTo(1200);

        // 1: DRIVE AB->BC
        Task t1 = schedule.getTasks().get(1);
        Assertions.assertThat(t1).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getFromLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getToLink()).isSameAs(networkAndLinks.lBC());
        Assertions.assertThat(t1.getBeginTime()).isEqualTo(1200);
        Assertions.assertThat(t1.getEndTime()).isEqualTo(1301);

        // 2: original STOP at BC
        Task t2 = schedule.getTasks().get(2);
        Assertions.assertThat(t2).isSameAs(fixedStop);
        Assertions.assertThat(t2.getBeginTime()).isEqualTo(1301);
        Assertions.assertThat(t2.getEndTime()).isEqualTo(1311);
        Assertions.assertThat(((DrtStopTask)t2).getLink()).isSameAs(networkAndLinks.lBC());


        // 3: final STAY at BC
        Task t3 = schedule.getTasks().get(3);
        Assertions.assertThat(t3).isSameAs(finalStay);
        Assertions.assertThat(t3.getBeginTime()).isEqualTo(1311);
        Assertions.assertThat(t3.getEndTime()).isEqualTo(veh.getServiceEndTime());
        Assertions.assertThat(((DrtStayTask)t3).getLink()).isSameAs(networkAndLinks.lBC());
    }

    @Test
    void relocatesAndReturnsBeforeFixedStopAtDifferentLink_whenSlackIsInSufficientForRelocation() {
        NetworkAndLinks networkAndLinks = getNetworkAndLinks();

        FreespeedTravelTimeAndDisutility freespeedTravelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        // --- Mobsim time "now" = 1000s
        MobsimTimer timer = Mockito.mock(MobsimTimer.class);
        when(timer.getTimeOfDay()).thenReturn(1000.);

        // --- Vehicle & schedule
        double serviceBegin = 0.0;
        double serviceEnd = 10_000.0;

        ImmutableDvrpVehicleSpecification spec = getImmutableDvrpVehicleSpecification(networkAndLinks, serviceBegin, serviceEnd);

        DvrpVehicle veh = new DvrpVehicleImpl(spec, networkAndLinks.lAB());
        Schedule schedule = veh.getSchedule();
        DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();

        // Current STAY at AB, short slack
        DrtStayTask currentStay = taskFactory.createStayTask(veh, 1000.0, 1100, networkAndLinks.lAB());
        schedule.addTask(currentStay);

        // DRIVE to existing fixed/prebooked stop at BC
        LeastCostPathCalculator lcpc = new SpeedyALTFactory().createPathCalculator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility);
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(networkAndLinks.lAB(), networkAndLinks.lBC(), currentStay.getEndTime(), lcpc, freespeedTravelTimeAndDisutility);
        DrtDriveTask driveTask = taskFactory.createDriveTask(veh, path, DrtDriveTask.TYPE);
        schedule.addTask(driveTask);

        // Upcoming fixed STOP at BC
        DrtStopTask fixedStop = taskFactory.createStopTask(veh, path.getArrivalTime(), path.getArrivalTime() + 10, networkAndLinks.lBC());
        schedule.addTask(fixedStop);

        // final STAY at BC
        DrtStayTask finalStay = taskFactory.createStayTask(veh, fixedStop.getEndTime(), veh.getServiceEndTime(), networkAndLinks.lBC());
        schedule.addTask(finalStay);

        //trigger start of schedule
        schedule.nextTask();

        // Sanity preconditions
        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(schedule.getCurrentTask()).isSameAs(currentStay);
        Assertions.assertThat(schedule.getTasks().get(2)).isSameAs(fixedStop);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        EmptyVehicleRelocator relocator = new EmptyVehicleRelocator(networkAndLinks.net(), freespeedTravelTimeAndDisutility, freespeedTravelTimeAndDisutility, timer, taskFactory);

        // Try to relocate to CD
        relocator.relocateVehicle(veh, networkAndLinks.lCD(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

        // --- Verify schedule shape:
        // After failed relocation we expect (unchanged):
        // 0: STAY(AB)
        // 1: DRIVE AB->BC
        // 2: STOP(BC)
        // 3: STAY(BC)

        Assertions.assertThat(schedule.getTaskCount()).isEqualTo(4);
        Assertions.assertThat(verifyTaskContinuity(schedule));


        // 0: current STAY left as before
        Task t0 = schedule.getTasks().get(0);
        Assertions.assertThat(t0).isInstanceOf(DrtStayTask.class);
        Assertions.assertThat(t0.getBeginTime()).isEqualTo(1000.0);
        Assertions.assertThat(t0.getEndTime()).isEqualTo(1100);

        // 1: DRIVE AB->BC
        Task t1 = schedule.getTasks().get(1);
        Assertions.assertThat(t1).isInstanceOf(DrtDriveTask.class);
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getFromLink()).isSameAs(networkAndLinks.lAB());
        Assertions.assertThat(((DrtDriveTask) t1).getPath().getToLink()).isSameAs(networkAndLinks.lBC());
        Assertions.assertThat(t1.getBeginTime()).isEqualTo(1100);
        Assertions.assertThat(t1.getEndTime()).isEqualTo(1201);

        // 2: original STOP at BC
        Task t2 = schedule.getTasks().get(2);
        Assertions.assertThat(t2).isSameAs(fixedStop);
        Assertions.assertThat(t2.getBeginTime()).isEqualTo(1201);
        Assertions.assertThat(t2.getEndTime()).isEqualTo(1211);
        Assertions.assertThat(((DrtStopTask)t2).getLink()).isSameAs(networkAndLinks.lBC());


        // 3: final STAY at BC
        Task t3 = schedule.getTasks().get(3);
        Assertions.assertThat(t3).isSameAs(finalStay);
        Assertions.assertThat(t3.getBeginTime()).isEqualTo(1211);
        Assertions.assertThat(t3.getEndTime()).isEqualTo(veh.getServiceEndTime());
        Assertions.assertThat(((DrtStayTask)t3).getLink()).isSameAs(networkAndLinks.lBC());
    }


    private static ImmutableDvrpVehicleSpecification getImmutableDvrpVehicleSpecification(NetworkAndLinks networkAndLinks, double serviceBegin, double serviceEnd) {
        ImmutableDvrpVehicleSpecification spec = ImmutableDvrpVehicleSpecification.newBuilder()
                .id(Id.create(1, DvrpVehicle.class))
                .startLinkId(networkAndLinks.lAB().getId())
                .serviceBeginTime(serviceBegin)
                .serviceEndTime(serviceEnd)
                .capacity(4)
                .build();
        return spec;
    }

    private static NetworkAndLinks getNetworkAndLinks() {
        Network net = NetworkUtils.createNetwork();
        Node nA = NetworkUtils.createAndAddNode(net, Id.createNodeId("A"), new Coord(0, 0));
        Node nB = NetworkUtils.createAndAddNode(net, Id.createNodeId("B"), new Coord(1000, 0));
        Node nC = NetworkUtils.createAndAddNode(net, Id.createNodeId("C"), new Coord(2000, 0));
        Node nD = NetworkUtils.createAndAddNode(net, Id.createNodeId("D"), new Coord(3000, 0));
        Link lAB = NetworkUtils.createAndAddLink(net, Id.createLinkId("A-B"), nA, nB, 1000, 10.0, 9999, 1);
        Link lBA = NetworkUtils.createAndAddLink(net, Id.createLinkId("B-A"), nB, nA, 1000, 10.0, 9999, 1);
        Link lBC = NetworkUtils.createAndAddLink(net, Id.createLinkId("B-C"), nB, nC, 1000, 10.0, 9999, 1);
        Link lCB = NetworkUtils.createAndAddLink(net, Id.createLinkId("C-B"), nC, nB, 1000, 10.0, 9999, 1);
        Link lCD = NetworkUtils.createAndAddLink(net, Id.createLinkId("C-D"), nC, nD, 1000, 10.0, 9999, 1);
        Link lDC = NetworkUtils.createAndAddLink(net, Id.createLinkId("D-C"), nD, nC, 1000, 10.0, 9999, 1);
        NetworkAndLinks networkAndLinks = new NetworkAndLinks(net, lAB, lBC, lCD);
        return networkAndLinks;
    }

    private record NetworkAndLinks(Network net, Link lAB, Link lBC, Link lCD) {
    }

    private boolean verifyTaskContinuity(Schedule schedule) {
        for (int i = 1; i < schedule.getTaskCount(); i++) {
            Task first = schedule.getTasks().get(i - 1);
            Task second = schedule.getTasks().get(i);
            if(first.getEndTime() != second.getBeginTime()) {
                return false;
            }
        }
        return true;
    }
}
