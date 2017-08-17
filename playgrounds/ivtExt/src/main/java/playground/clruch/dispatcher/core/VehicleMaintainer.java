// code by jph
// API change by clruch
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
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
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
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
    private final List<RoboTaxi> roboTaxis = new ArrayList<>();
    private Double private_now = null;
    private int infoLinePeriod = 0;
    private String previousInfoMarker = "";
    private Integer AVVEHILCECOUNT = null;

    VehicleMaintainer(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    /** @return time of current re-dispatching iteration step
     * @throws NullPointerException
     *             if dispatching has not started yet */
    protected final double getTimeNow() {
        return private_now;
    }

    /** @return collection of AVVehicles that have started their schedule */
    protected final List<RoboTaxi> getRoboTaxis() {
        if (roboTaxis.isEmpty() || !roboTaxis.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(roboTaxis);
    }

    protected final Collection<RoboTaxi> getDivertableRoboTaxis() {
        return roboTaxis.stream().filter(rt -> (rt.isWithoutDirective())).filter(RoboTaxi::isWithoutCustomer).collect(Collectors.toList());
    }

    private final void updateDivertableLocations() {
        for (RoboTaxi robotaxi : getRoboTaxis()) {
            GlobalAssert.that(robotaxi.isWithoutDirective());
            Schedule schedule = robotaxi.getSchedule();
            new AVTaskAdapter(schedule.getCurrentTask()) {
                @Override
                public void handle(AVDriveTask avDriveTask) {
                    // for empty cars the drive task is second to last task
                    if (Schedules.isNextToLastTask(avDriveTask)) {
                        TaskTracker taskTracker = avDriveTask.getTaskTracker();
                        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                        LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint();
                        robotaxi.setDivertableLinkTime(linkTimePair);
                        robotaxi.setCurrentDriveDestination(avDriveTask.getPath().getToLink());
                    } else
                        GlobalAssert.that(robotaxi.getAVStatus().equals(AVStatus.DRIVEWITHCUSTOMER));
                }

                @Override
                public void handle(AVPickupTask avPickupTask) {
                    GlobalAssert.that(robotaxi.getAVStatus().equals(AVStatus.DRIVEWITHCUSTOMER));
                }

                @Override
                public void handle(AVDropoffTask avDropOffTask) {
                    GlobalAssert.that(robotaxi.getAVStatus().equals(AVStatus.DRIVEWITHCUSTOMER));
                }

                @Override
                public void handle(AVStayTask avStayTask) {
                    // for empty vehicles the current task has to be the last task
                    if (Schedules.isLastTask(avStayTask)) {
                        GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow());
                        robotaxi.setDivertableLinkTime(new LinkTimePair(avStayTask.getLink(), getTimeNow()));
                        robotaxi.setCurrentDriveDestination(avStayTask.getLink());
                        robotaxi.setAVStatus(AVStatus.STAY);
                    }
                }
            };
        }
    }

    @Override
    public final void registerVehicle(AVVehicle vehicle) {
        GlobalAssert.that(vehicle.getStartLink() != null);
        roboTaxis.add(new RoboTaxi(vehicle, new LinkTimePair(vehicle.getStartLink(), -1.0), vehicle.getStartLink()));
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    @Override
    public final void onNextTimestep(double now) {
        private_now = now; // <- time available to derived class via getTimeNow()

        if (0 < infoLinePeriod && Math.round(now) % infoLinePeriod == 0) {
            String infoLine = getInfoLine();
            String marker = infoLine.substring(16);
            if (!marker.equals(previousInfoMarker)) {
                previousInfoMarker = marker;
                System.out.println(infoLine);
            }
        }

        consistencyCheck();
        beforeStepTasks();
        executePickups();
        redispatch(now);
        afterStepTasks();
        notifySimulationSubscribers(Math.round(now));
        consistencyCheck();

        for (RoboTaxi robotaxi : roboTaxis) {
            if (!robotaxi.isWithoutDirective()) {
                robotaxi.executeDirective();
            }
        }

    }

    private void beforeStepTasks() {

        // update divertable locations of RoboTaxis
        updateDivertableLocations();

        // update current locations of RoboTaxis
        if (private_now > 0) { // at time 0, tasks are not started.
            updateCurrentLocations();
        }

    }

    private void afterStepTasks() {
        stopUnusedVehicles();
    }

    private void consistencyCheck() {
        consistencySubCheck();

    }

    void updateCurrentLocations() {
        int failed = 0;
        if (!roboTaxis.isEmpty()) {
            for (RoboTaxi robotaxi : roboTaxis) {
                final Link link = AVLocation.of(robotaxi);
                if (link != null) {
                    robotaxi.setCurrentLocation(link);
                } else {
                    ++failed;
                }

            }
            if (AVVEHILCECOUNT == null)
                AVVEHILCECOUNT = getRoboTaxis().size();
        }
    }

    /* package */ abstract void executePickups();

    /* package */ abstract void stopUnusedVehicles();

    /* package */ abstract void consistencySubCheck();

    /* package */ abstract void notifySimulationSubscribers(long round_now);

    /** invoked at the beginning of every iteration dispatchers can update their data structures
     * based on the stay vehicle set function is not meant
     * for issuing directives */
    /// * package */ abstract void updateDatastructures();

    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }

    /** derived classes should override this function
     * 
     * @param now */
    protected abstract void redispatch(double now);

    // ===================================================================================
    // INFOLINE functions

    /** derived classes should override this function to add details
     * 
     * @return */
    protected String getInfoLine() {
        final String string = getClass().getSimpleName() + "        ";
        return String.format("%s@%6d V=(%4ds,%4dd)", //
                string.substring(0, 6), //
                (long) getTimeNow(), //
                getRoboTaxis().stream().filter(rt -> rt.isInStayTask()).count(), //
                getRoboTaxis().stream().filter(rt -> (rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER) || rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER)))
                        .count());
    }

    /** @param infoLinePeriod
     *            positive values determine the period, negative values or 0 will disable the
     *            printout */
    public final void setInfoLinePeriod(int infoLinePeriod) {
        this.infoLinePeriod = infoLinePeriod;
    }

}
