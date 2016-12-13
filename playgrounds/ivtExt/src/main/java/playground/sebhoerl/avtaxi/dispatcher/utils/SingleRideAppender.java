package playground.sebhoerl.avtaxi.dispatcher.utils;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SingleRideAppender {
    final private ParallelLeastCostPathCalculator router;
    final private AVDispatcherConfig config;
    final private TravelTime travelTime;

    private List<AppendTask> tasks = new LinkedList<>();

    public SingleRideAppender(AVDispatcherConfig config, ParallelLeastCostPathCalculator router, TravelTime travelTime) {
        this.router = router;
        this.config = config;
        this.travelTime = travelTime;
    }

    private class AppendTask {
        final public AVRequest request;
        final public AVVehicle vehicle;

        final public LeastCostPathFuture pickup;
        final public LeastCostPathFuture dropoff;

        final public double time;

        public AppendTask(AVRequest request, AVVehicle vehicle, double time, LeastCostPathFuture pickup, LeastCostPathFuture dropoff) {
            this.request = request;
            this.vehicle = vehicle;
            this.pickup = pickup;
            this.dropoff = dropoff;
            this.time = time;
        }
    }

    public void schedule(AVRequest request, AVVehicle vehicle, double now) {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
        AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);

        LeastCostPathFuture pickup = router.calcLeastCostPath(stayTask.getLink().getToNode(), request.getFromLink().getFromNode(), now, null, null);
        LeastCostPathFuture dropoff = router.calcLeastCostPath(request.getFromLink().getToNode(), request.getToLink().getFromNode(), now, null, null);

        tasks.add(new AppendTask(request, vehicle, now, pickup, dropoff));
    }

    public void schedule(AppendTask task) {
        AVRequest request = task.request;
        AVVehicle vehicle = task.vehicle;
        double now = task.time;

        AVTimingParameters timing = config.getParent().getTimingParameters();

        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
        AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);

        double startTime = 0.0;
        double scheduleEndTime = schedule.getEndTime();

        if (stayTask.getStatus() == Task.TaskStatus.STARTED) {
            startTime = now;
        } else {
            startTime = stayTask.getBeginTime();
        }

        VrpPathWithTravelData pickupPath = VrpPaths.createPath(stayTask.getLink(), request.getFromLink(), startTime, task.pickup.get(), travelTime);
        VrpPathWithTravelData dropoffPath = VrpPaths.createPath(request.getFromLink(), request.getToLink(), pickupPath.getArrivalTime() + timing.getPickupDurationPerStop(), task.dropoff.get(), travelTime);

        AVDriveTask pickupDriveTask = new AVDriveTask(pickupPath);
        AVPickupTask pickupTask = new AVPickupTask(
                pickupPath.getArrivalTime(),
                pickupPath.getArrivalTime() + timing.getPickupDurationPerStop(),
                request.getFromLink(), Arrays.asList(request));
        AVDriveTask dropoffDriveTask = new AVDriveTask(dropoffPath, Arrays.asList(request));
        AVDropoffTask dropoffTask = new AVDropoffTask(
                dropoffPath.getArrivalTime(),
                dropoffPath.getArrivalTime() + timing.getDropoffDurationPerStop(),
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

        double distance = 0.0;
        for (int i = 0; i < dropoffPath.getLinkCount(); i++) {
            distance += dropoffPath.getLink(i).getLength();
        }
        request.getRoute().setDistance(distance);

        if (dropoffTask.getEndTime() < scheduleEndTime) {
            schedule.addTask(new AVStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
        }
    }

    public void update() {
        // TODO: This can be made more efficient if one knows which ones have just been added and which ones are still
        // to be processed. Depends mainly on if "update" is called before new tasks are submitted or after ...

        Iterator<AppendTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            AppendTask task = iterator.next();

            if (task.pickup.isDone() && task.dropoff.isDone()) {
                schedule(task);
                iterator.remove();
            }
        }
    }
}
