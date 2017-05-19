package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * The purpose of VehicleMaintainer is to register {@link AVVehicle}
 * and provide the collection of available vehicles to derived class.
 * <p>
 * manages assignments of {@link AbstractDirective} to {@link AVVehicle}s.
 * path computations attached to assignments are computed in parallel
 * {@link ParallelLeastCostPathCalculator}.
 */
abstract class VehicleMaintainer implements AVDispatcher {
    protected final EventsManager eventsManager;

    private final List<AVVehicle> vehicles = new ArrayList<>(); // access via function getFunctioningVehicles()
    private Double private_now = null;
    private Map<AVVehicle, AbstractDirective> private_vehicleDirectives = new LinkedHashMap<>();
    private long routingTimeNano = 0; // <- total cpu time required to compute paths and update schedules
    private int infoLinePeriod = 0;

    VehicleMaintainer(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    /**
     * @param infoLinePeriod
     *            positive values determine the period,
     *            negative values or 0 will disable the printout
     */
    public final void setInfoLinePeriod(int infoLinePeriod) {
        this.infoLinePeriod = infoLinePeriod;
    }

    /**
     * maps given {@link AVVehicle} to given {@link AbstractDirective}
     *
     * @param avVehicle
     * @param abstractDirective
     */
    /* package */
    synchronized void assignDirective(AVVehicle avVehicle, AbstractDirective abstractDirective) {
        GlobalAssert.that(isWithoutDirective(avVehicle));
        private_vehicleDirectives.put(avVehicle, abstractDirective);
    }

    /**
     * @param avVehicle
     * @return true if given {@link AVVehicle} doesn't have a {@link AbstractDirective} assigned to it
     */
    private synchronized boolean isWithoutDirective(AVVehicle avVehicle) {
        return !private_vehicleDirectives.containsKey(avVehicle);
    }

    /**
     * @return collection of AVVehicles that have started their schedule
     */
    /* package */ final Collection<AVVehicle> getFunctioningVehicles() {
        if (vehicles.isEmpty() || !vehicles.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(vehicles);
    }

    /**
     * @return collection of all vehicles available to {@link VehicleMaintainer}
     */
    protected final Collection<AVVehicle> getMaintainedVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     *
     * @return collection of all vehicles that currently are in the last task, which is of type STAY
     */
    public final Map<Link, Queue<AVVehicle>> getStayVehicles() {
        Map<Link, Queue<AVVehicle>> map = new HashMap<>();
        for (AVVehicle avVehicle : getFunctioningVehicles())
            if (isWithoutDirective(avVehicle)) {
                Task task = Schedules.getLastTask(avVehicle.getSchedule()); // <- last task
                if (task.getStatus().equals(Task.TaskStatus.STARTED)) // <- task is STARTED
                    new AVTaskAdapter(task) {
                        @Override
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
     * @return collection of AVVehicles that are in stay mode
     */
    private final Collection<AVVehicle> getStayVehiclesCollection() {
        return getFunctioningVehicles().stream() //
                .filter(this::isWithoutDirective) //
                .filter(v -> Schedules.getLastTask(v.getSchedule()).getStatus().equals(Task.TaskStatus.STARTED)) //
                .collect(Collectors.toList());
    }

    /**
     * @return collection of cars that are in the driving state without customer, or stay task.
     *         if a vehicle is given a directive for instance by setVehicleDiversion(...) or setAcceptRequest(...)
     *         that invoke assignDirective(...), the vehicle is not included in the successive call to
     *         getDivertableVehicles() until it becomes <i>divertable</i> again.
     */
    protected final Collection<VehicleLinkPair> getDivertableVehicles() {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (AVVehicle avVehicle : getFunctioningVehicles())
            if (isWithoutDirective(avVehicle)) {
                Schedule schedule = avVehicle.getSchedule();
                new AVTaskAdapter(schedule.getCurrentTask()) {
                    @Override
                    public void handle(AVDriveTask avDriveTask) {
                        // for empty cars the drive task is second to last task
                        if (Schedules.isNextToLastTask(avDriveTask)) {
                            TaskTracker taskTracker = avDriveTask.getTaskTracker();
                            OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                            LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint(); // there is a slim chance that function returns null
                            if (linkTimePair != null)
                                collection.add(new VehicleLinkPair(avVehicle, linkTimePair, avDriveTask.getPath().getToLink()));
                        }
                    }

                    @Override
                    public void handle(AVStayTask avStayTask) {
                        // for empty vehicles the current task has to be the last task
                        if (Schedules.isLastTask(avStayTask)) {
                            GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow()); // <- self evident?
                            LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), getTimeNow());
                            collection.add(new VehicleLinkPair(avVehicle, linkTimePair, null));
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

    private String previousInfoMarker = "";

    abstract void notifySimulationSubscribers(long round_now);

    /**
     * invoked at the beginning of every iteration
     * dispatchers can update their data structures based on the stay vehicle set
     * function is not meant for issuing directives
     */
    abstract void updateDatastructures(Collection<AVVehicle> stayVehicles);

    @Override
    public final void onNextTimestep(double now) {
        private_now = now; // <- time available to derived class via getTimeNow()

        updateDatastructures(Collections.unmodifiableCollection(getStayVehiclesCollection()));

        if (0 < infoLinePeriod && Math.round(now) % infoLinePeriod == 0) {
            String infoLine = getInfoLine();
            String marker = infoLine.substring(16);
            if (!marker.equals(previousInfoMarker)) {
                previousInfoMarker = marker;
                System.out.println(infoLine);
            }
        }

        notifySimulationSubscribers(Math.round(now));

        redispatch(now);
        private_now = null; // <- time unavailable
        long tic = System.nanoTime();
        private_vehicleDirectives.values().stream() //
                .parallel() //
                .forEach(AbstractDirective::execute);
        routingTimeNano += System.nanoTime() - tic;
        private_vehicleDirectives.clear();
    }

    /**
     * derived classes should override this function to add details
     * 
     * @return
     */
    public String getInfoLine() {
        final String string = getClass().getSimpleName() + "        ";
        return String.format("%s@%6d V=(%4ds,%4dd)", //
                string.substring(0, 6), //
                (long) getTimeNow(), //
                getStayVehicles().values().stream().mapToInt(Queue::size).sum(), //
                getDivertableVehicles().size() //
        );
    }

    /**
     * @return time of current re-dispatching iteration step
     * @throws NullPointerException
     *             if dispatching has not started yet
     */
    protected final double getTimeNow() {
        return private_now;
    }

    /**
     * derived classes should override this function
     * 
     * @param now
     */
    protected abstract void redispatch(double now);

    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }
}
