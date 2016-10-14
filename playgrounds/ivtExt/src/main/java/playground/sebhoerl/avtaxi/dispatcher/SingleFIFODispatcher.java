package playground.sebhoerl.avtaxi.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class SingleFIFODispatcher implements AVDispatcher {
    @Inject
    private AVConfigGroup config;

    @Inject @Named(AVModule.AV_MODE)
    private LeastCostPathCalculator router;

    @Inject @Named(AVModule.AV_MODE)
    private TravelTime travelTime;

    private Queue<AVVehicle> availableVehicles = new LinkedList<>();
    private Queue<AVRequest> pendingRequests = new LinkedList<>();

    private boolean reoptimize = false;

    @Override
    public void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request);
        reoptimize = true;
    }

    @Override
    public void onNextTaskStarted(AVTask task) {
        if (task.getAVTaskType() == AVTask.AVTaskType.STAY) {
            availableVehicles.add((AVVehicle) task.getSchedule().getVehicle());
        }
    }

    @Override
    public void addVehicle(AVVehicle vehicle) {
        availableVehicles.add(vehicle);
    }

    private void reoptimize(double now) {
        while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
            AVVehicle vehicle = availableVehicles.poll();
            AVRequest request = pendingRequests.poll();
            performAssignment(vehicle, request, now);
        }

        reoptimize = false;
    }

    void performAssignment(AVVehicle vehicle, AVRequest request, double now) {
        @SuppressWarnings("unchecked")
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
        AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);

        double startTime = 0.0;
        double scheduleEndTime = schedule.getEndTime();

        if (stayTask.getStatus() == Task.TaskStatus.STARTED) {
            startTime = now;
        } else {
            startTime = stayTask.getBeginTime();
        }

        VrpPathWithTravelData pickupPath = VrpPaths.calcAndCreatePath(stayTask.getLink(), request.getFromLink(), startTime, router, travelTime);
        VrpPathWithTravelData dropoffPath = VrpPaths.calcAndCreatePath(request.getFromLink(), request.getToLink(), pickupPath.getArrivalTime() + config.getPickupDuration(), router, travelTime);

        AVDriveTask pickupDriveTask = new AVDriveTask(pickupPath);
        AVPickupTask pickupTask = new AVPickupTask(
                pickupPath.getArrivalTime(),
                pickupPath.getArrivalTime() + config.getPickupDuration(),
                request.getFromLink(), Arrays.asList(request));
        AVDriveTask dropoffDriveTask = new AVDriveTask(dropoffPath, Arrays.asList(request));
        AVDropoffTask dropoffTask = new AVDropoffTask(
                dropoffPath.getArrivalTime(),
                dropoffPath.getArrivalTime() + config.getDropoffDuration(),
                request.getToLink(),
                Arrays.asList(request));

        if (stayTask.getStatus() == Task.TaskStatus.STARTED) {
            stayTask.setEndTime(startTime);
        } else {
            schedule.removeLastTask();
        }

        schedule.addTask(pickupDriveTask);
        schedule.addTask(pickupTask);
        schedule.addTask(dropoffDriveTask);
        schedule.addTask(dropoffTask);

        if (dropoffTask.getEndTime() < scheduleEndTime) {
            schedule.addTask(new AVStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
        }
    }

    @Override
    public void onNextTimestep(double now) {
        if (reoptimize) reoptimize(now);
    }
}
