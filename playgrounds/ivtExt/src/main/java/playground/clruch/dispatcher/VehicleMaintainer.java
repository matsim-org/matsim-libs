package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

/**
 * purpose of VehicleMaintainer is to register vehicles and provide list of available vehicles to derived class
 */
public abstract class VehicleMaintainer implements AVDispatcher {
    protected final EventsManager eventsManager;

    private final List<AVVehicle> vehicles = new ArrayList<>(); // access via function getFunctioningVehicles()
    private Double private_now = null;

    public VehicleMaintainer(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    protected final Collection<AVVehicle> getFunctioningVehicles() {
        if (vehicles.isEmpty() || !vehicles.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(vehicles);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return collection of all vehicles that currently are in the last task, which is of type STAY
     */
    protected final Map<Link, Queue<AVVehicle>> getStayVehicles() {
        Map<Link, Queue<AVVehicle>> map = new HashMap<>();
        for (AVVehicle avVehicle : getFunctioningVehicles()) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule(); // TODO insert directly into next line
            AbstractTask abstractTask = Schedules.getLastTask(schedule); // <- last task
            if (abstractTask.getStatus().equals(Task.TaskStatus.STARTED)) // <- task is STARTED
                new AVTaskAdapter(abstractTask) {
                    public void handle(AVStayTask avStayTask) { // <- type of task is STAY
                        final Link link = avStayTask.getLink();
                        if (!map.containsKey(link))
                            map.put(link, new LinkedList<>());
                        map.get(link).add(avVehicle); // <- append vehicle to list of vehicles at link
                    }
                };
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return collection of cars that are in the driving state without customer, or stay task
     */
    protected final Collection<VehicleLinkPair> getDivertableVehicles() {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (AVVehicle avVehicle : getFunctioningVehicles()) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            AbstractTask abstractTask = schedule.getCurrentTask();
            new AVTaskAdapter(abstractTask) {
                @Override
                public void handle(AVDriveTask avDriveTask) {
                    // for empty cars the drive task is second to last task
                    if (Schedules.isNextToLastTask(abstractTask)) {
                        TaskTracker taskTracker = avDriveTask.getTaskTracker();
                        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                        LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint(); // there is a slim chance that function returns null 
                        if (linkTimePair != null) // TODO treat null case ? 
                            collection.add(new VehicleLinkPair(avVehicle, linkTimePair));
                    }
                }

                @Override
                public void handle(AVStayTask avStayTask) {
                    // for empty vehicles the current task has to be the last task
                    if (Schedules.isLastTask(abstractTask))
                        if (avStayTask.getBeginTime() + 5 < getTimeNow()) { // TODO magic const
                            LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), getTimeNow());
                            collection.add(new VehicleLinkPair(avVehicle, linkTimePair));
                        }
                }
            };
        }
        return collection;
    }

    @Override
    public final void registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    @Override
    public final void onNextTimestep(double now) {
        private_now = now; // time available
        redispatch(now);
        private_now = null; // time unavailable
    }

    /**
     * @return time of current re-dispatching iteration step
     * 
     * @throws NullPointerException
     *             if dispatching has not started yet
     */
    protected final double getTimeNow() {
        return private_now;
    }

    public abstract void redispatch(double now);

}
