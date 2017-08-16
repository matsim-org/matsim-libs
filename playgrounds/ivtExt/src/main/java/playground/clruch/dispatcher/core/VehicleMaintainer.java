// code by jph
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

/** The purpose of VehicleMaintainer is to register {@link AVVehicle} and provide the collection of
 * available vehicles to derived class.
 * <p>
 * manages assignments of {@link AbstractDirective} to {@link AVVehicle}s. path computations
 * attached to assignments are computed in parallel
 * {@link ParallelLeastCostPathCalculator}. */
abstract class VehicleMaintainer implements AVDispatcher {
    protected final EventsManager eventsManager;

    private final List<AVVehicle> vehicles = new ArrayList<>(); // access via function
                                                                // getFunctioningVehicles()
    private final List<RoboTaxi> roboTaxis = new ArrayList<>();
    private Double private_now = null;
    private Map<AVVehicle, AbstractDirective> private_vehicleDirectives = new LinkedHashMap<>();
    private int infoLinePeriod = 0;
    private Map<AVVehicle, RoboTaxi> avVehicleVehicleLinkPairMap = new HashMap<>();
    private String previousInfoMarker = "";

    VehicleMaintainer(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    /** @param infoLinePeriod
     *            positive values determine the period, negative values or 0 will disable the
     *            printout */
    public final void setInfoLinePeriod(int infoLinePeriod) {
        this.infoLinePeriod = infoLinePeriod;
    }

    /** @return time of current re-dispatching iteration step
     * @throws NullPointerException
     *             if dispatching has not started yet */
    protected final double getTimeNow() {
        return private_now;
    }

    // TODO put this inside RoboTaxi
    synchronized void assignDirective(RoboTaxi robotaxi, AbstractDirective abstractDirective) {
        GlobalAssert.that(isWithoutDirective(robotaxi));
        robotaxi.setDirective(abstractDirective);
    }

    // TODO put this inside RoboTaxi
    private synchronized boolean isWithoutDirective(RoboTaxi robotaxi) {
        if (robotaxi.getDirective() == null) {
            return true;
        }
        return false;
    }

    /** @return collection of AVVehicles that have started their schedule */
    protected final List<RoboTaxi> getRoboTaxis() {
        if (roboTaxis.isEmpty() || !roboTaxis.get(0).getAVVehicle().getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(roboTaxis);
    }

    // TODO make a function for rt.getDirective() == null
    protected final Collection<RoboTaxi> getDivertableRoboTaxis() {
        return roboTaxis.stream().filter(rt-> (rt.getDirective() == null))
                .filter(RoboTaxi::isWithoutCustomer)
        .collect(Collectors.toList());
    }


    // TODO what happens with the other AVTasks? 
    // TODO this function is important, check thoroughly
    private final void updateDivertableLocations() {
        for (RoboTaxi robotaxi : getRoboTaxis()) {
            if (isWithoutDirective(robotaxi)) {
                Schedule schedule = robotaxi.getAVVehicle().getSchedule();
                new AVTaskAdapter(schedule.getCurrentTask()) {
                    @Override
                    public void handle(AVDriveTask avDriveTask) {
                        // for empty cars the drive task is second to last task
                        if (Schedules.isNextToLastTask(avDriveTask)) {
                            TaskTracker taskTracker = avDriveTask.getTaskTracker();
                            OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                            LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint();
                            // there is a slim chance that function returns null
                            // update divertibleLocation and currentDriveDestination
                            // TODO put this as an info into RoboTaxi and make sure it cannot be used in the iteration
                            if (linkTimePair != null)
                                robotaxi.setLinkTimePair(linkTimePair);
                            robotaxi.setCurrentDriveDestination(avDriveTask.getPath().getToLink());
                        } else {
                            // TODO is this actually necessary?
                            robotaxi.setAVStatus(AVStatus.DRIVEWITHCUSTOMER);
                        }
                    }

                    @Override
                    public void handle(AVStayTask avStayTask) {
                        // for empty vehicles the current task has to be the last task
                        if (Schedules.isLastTask(avStayTask)) {
                            GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow()); // <- self
                                                                                          // evident?
                            LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), getTimeNow());
                            // collection.add(new VehicleLinkPair(avVehicle, linkTimePair, null));
                            robotaxi.setLinkTimePair(linkTimePair);
                            robotaxi.setCurrentDriveDestination(avStayTask.getLink());
                            robotaxi.setAVStatus(AVStatus.STAY);
                        }
                    }
                };
            }
        }
    }

    @Override
    public final void registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
        GlobalAssert.that(vehicle.getStartLink() != null);
        roboTaxis.add(new RoboTaxi(vehicle, new LinkTimePair(vehicle.getStartLink(), -1.0), vehicle.getStartLink()));
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    abstract void notifySimulationSubscribers(long round_now);

    /** invoked at the beginning of every iteration dispatchers can update their data structures
     * based on the stay vehicle set function is not meant
     * for issuing directives */
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

        consistencyCheck();
        beforeStepTasks();
        redispatch(now);
        afterStepTasks();
        consistencyCheck();

        for (RoboTaxi robotaxi : roboTaxis) {
            if (robotaxi.getDirective() != null) {
                robotaxi.getDirective().execute();
                robotaxi.setDirective(null);
            }
        }

    }

    private void beforeStepTasks() {
        // update divertable locations of RoboTaxis
        updateDivertableLocations();
        executePickups();
    }

    private void afterStepTasks() {
        stopUnusedVehicles();
    }

    
    private void consistencyCheck(){
        consistencySubCheck();
        
    }
    
    
    /* package */ abstract void executePickups();

    /* package */ abstract void stopUnusedVehicles();
    
    /* package */ abstract void consistencySubCheck();

    /** derived classes should override this function to add details
     * 
     * @return */
    protected String getInfoLine() {
        final String string = getClass().getSimpleName() + "        ";
        return String.format("%s@%6d V=(%4ds,%4dd)", //
                string.substring(0, 6), //
                (long) getTimeNow(), //
                getStayVehicles().values().stream().mapToInt(Queue::size).sum(), //
                getDivertableVehicles().size() //
        );
    }

    /** derived classes should override this function
     * 
     * @param now */
    protected abstract void redispatch(double now);

    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }

    // ===========================================================================================================
    // ===========================================================================================================
    // ===========================================================================================================
    // OLD FUNCTIONS TO DELETE
    // ===========================================================================================================
    // ===========================================================================================================
    // ===========================================================================================================

    @Deprecated
    protected final Collection<RoboTaxi> getDivertableVehicleLinkPairs() {
        getDivertableVehicles();
        return avVehicleVehicleLinkPairMap.values();
    }

    /** @param avVehicle
     * @return true if given {@link AVVehicle} doesn't have a {@link AbstractDirective} assigned
     *         to
     *         it */
    @Deprecated
    private synchronized boolean isWithoutDirective(AVVehicle avVehicle) {
        return !private_vehicleDirectives.containsKey(avVehicle);
    }

    /** maps given {@link AVVehicle} to given {@link AbstractDirective}
     *
     * @param avVehicle
     * @param abstractDirective */
    /* package */
    @Deprecated
    synchronized void assignDirective(AVVehicle avVehicle, AbstractDirective abstractDirective) {
        GlobalAssert.that(isWithoutDirective(avVehicle));
        private_vehicleDirectives.put(avVehicle, abstractDirective);
    }

    /** @return collection of cars that are in the driving state without customer, or stay task. if
     *         a vehicle is given a directive for instance by
     *         setVehicleDiversion(...) or setAcceptRequest(...) that invoke assignDirective(...),
     *         the vehicle is not included in the successive call
     *         to getDivertableVehicles() until it becomes <i>divertable</i> again. */
    @Deprecated
    protected final Collection<AVVehicle> getDivertableVehicles() {
        avVehicleVehicleLinkPairMap.clear();
        // Collection<VehicleLinkPair> collection = new LinkedList<>();
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
                            LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint(); // there
                                                                                                    // is
                                                                                                    // a
                                                                                                    // slim
                                                                                                    // chance
                                                                                                    // that
                                                                                                    // function
                                                                                                    // returns
                                                                                                    // null
                            if (linkTimePair != null)
                                // collection.add(new VehicleLinkPair(avVehicle, linkTimePair,
                                // avDriveTask.getPath().getToLink()));
                                avVehicleVehicleLinkPairMap.put(avVehicle, new RoboTaxi(avVehicle, linkTimePair, avDriveTask.getPath().getToLink()));
                        }
                    }

                    @Override
                    public void handle(AVStayTask avStayTask) {
                        // for empty vehicles the current task has to be the last task
                        if (Schedules.isLastTask(avStayTask)) {
                            GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow()); // <- self
                                                                                          // evident?
                            LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), getTimeNow());
                            // collection.add(new VehicleLinkPair(avVehicle, linkTimePair, null));
                            avVehicleVehicleLinkPairMap.put(avVehicle, new RoboTaxi(avVehicle, linkTimePair, avStayTask.getLink()));
                        }
                    }
                };
            }

        return avVehicleVehicleLinkPairMap.keySet();
        // return collection;

    }

    /** @return collection of AVVehicles that have started their schedule */
    @Deprecated
    /* package */ final Collection<AVVehicle> getFunctioningVehicles() {
        if (vehicles.isEmpty() || !vehicles.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(vehicles);
    }

    /** function call leaves the state of the {@link UniversalDispatcher} unchanged. successive
     * calls to the function return the identical collection.
     *
     * @return collection of all vehicles that currently are in the last task, which is of type
     *         STAY */
    @Deprecated
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
                            map.get(link).add(avVehicle); // <- append vehicle to list of vehicles
                                                          // at link
                        }
                    };
            }
        return Collections.unmodifiableMap(map);
    }

    @Deprecated
    protected final LinkTimePair getLinkTimePair(AVVehicle avVehicle) {
        return avVehicleVehicleLinkPairMap.get(avVehicle).getLinkTimePair();
    }

    @Deprecated
    protected final RoboTaxi getVehicleLinkPair(AVVehicle avVehicle) {
        return avVehicleVehicleLinkPairMap.get(avVehicle);
    }

    /** @return collection of AVVehicles that are in stay mode */
    @Deprecated
    private final Collection<AVVehicle> getStayVehiclesCollection() {
        return getFunctioningVehicles().stream() //
                .filter(this::isWithoutDirective) //
                .filter(v -> Schedules.getLastTask(v.getSchedule()).getStatus().equals(Task.TaskStatus.STARTED)) //
                .collect(Collectors.toList());
    }

}
