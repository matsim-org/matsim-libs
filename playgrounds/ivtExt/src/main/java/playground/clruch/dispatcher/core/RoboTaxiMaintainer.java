// code by jph
// API change by clruch
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** The purpose of RoboTaxiMaintainer is to register {@link AVVehicle} and provide the collection of
 * available vehicles to derived class.
 * <p>
 * manages assignments of {@link AbstractDirective} to {@link AVVehicle}s. path computations
 * attached to assignments are computed in parallel
 * {@link ParallelLeastCostPathCalculator}. */
abstract class RoboTaxiMaintainer implements AVDispatcher {
    protected final EventsManager eventsManager;
    private final List<RoboTaxi> roboTaxis = new ArrayList<>();
    private Double private_now = null;
    public InfoLine infoLine = null;

    RoboTaxiMaintainer(EventsManager eventsManager, AVDispatcherConfig avDispatcherConfig) {
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        this.eventsManager = eventsManager;
        this.infoLine = new InfoLine(safeConfig.getInteger("infoLinePeriod", 10));

    }

    /** @return time of current re-dispatching iteration step
     * @throws NullPointerException
     *             if dispatching has not started yet */
    protected final double getTimeNow() {
        return private_now;
    }

    /** @return collection of RoboTaxis */
    protected final List<RoboTaxi> getRoboTaxis() {
        if (roboTaxis.isEmpty() || !roboTaxis.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        return Collections.unmodifiableList(roboTaxis);
    }

    private void updateDivertableLocations() {
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
                        LinkTimePair linkTimePair = ((OnlineDriveTaskTrackerImpl) onlineDriveTaskTracker).getSafeDiversionPoint();
                        GlobalAssert.that(linkTimePair != null);
                        robotaxi.setDivertableLinkTime(linkTimePair);
                        robotaxi.setCurrentDriveDestination(avDriveTask.getPath().getToLink());
                        GlobalAssert.that(!robotaxi.getAVStatus().equals(AVStatus.DRIVEWITHCUSTOMER));
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
                    if (Schedules.isLastTask(avStayTask) && !isInPickupRegister(robotaxi)) {
                        GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow());
                        GlobalAssert.that(avStayTask.getLink() != null);
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
        roboTaxis.add(new RoboTaxi(vehicle, new LinkTimePair(vehicle.getStartLink(), 0.0), vehicle.getStartLink()));
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    @Override
    public final void onNextTimestep(double now) {
        private_now = now; // <- time available to derived class via getTimeNow()
        updateInfoLine();
        notifySimulationSubscribers(Math.round(now));
        
        consistencyCheck();
        beforeStepTasks();
        executePickups();
        redispatch(now);
        afterStepTasks();
        executeDirectives();
        consistencyCheck();
    }

    protected void updateInfoLine() {
        String infoLine = getInfoLine();
        this.infoLine.updateInfoLine(infoLine, getTimeNow());
    }

    /** derived classes should override this function to add details
     * 
     * @return String with infoLine content */
    protected String getInfoLine() {
        final String string = getClass().getSimpleName() + "        ";
        return String.format("%s@%6d V=(%4ds,%4dd)", //
                string.substring(0, 6), //
                (long) getTimeNow(), //
                roboTaxis.stream().filter(rt -> rt.isInStayTask()).count(), //
                roboTaxis.stream().filter(rt -> (rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER) || rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER)))
                        .count());

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
        stopAbortedPickupRoboTaxis();
    }

    private void consistencyCheck() {
        consistencySubCheck();

    }

    private void executeDirectives() {
        roboTaxis.stream().filter(rt -> !rt.isWithoutDirective()).forEach(RoboTaxi::executeDirective);
    }

    private void updateCurrentLocations() {
        int failed = 0;
        if (!roboTaxis.isEmpty()) {
            for (RoboTaxi robotaxi : roboTaxis) {
                final Link link = AVLocation.of(robotaxi);
                if (link != null) {
                    robotaxi.setLastKnownLocation(link);
                } else {
                    ++failed;
                }

            }
        }
    }

    /* package */ abstract void executePickups();

    /* package */ abstract void stopAbortedPickupRoboTaxis();

    /* package */ abstract void consistencySubCheck();

    /* package */ abstract void notifySimulationSubscribers(long round_now);

    /* package */ abstract boolean isInPickupRegister(RoboTaxi robotaxi);

    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }

    /** derived classes should override this function
     * 
     * @param now */
    protected abstract void redispatch(double now);

}
