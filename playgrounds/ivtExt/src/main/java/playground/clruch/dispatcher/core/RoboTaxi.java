// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;

// add a lot of documentation // TODO
// TODO are there functions, which can be used only in certain Dispatchers, e. g. UniversalDispatcher? 
// TODO which functions are designed for who? 
// TODO which function is allowed at which time?
// TODO check all access rights and restrict to minimum

public class RoboTaxi {
    private final AVVehicle avVehicle;
    private LinkTimePair linkTimePair;
    private Link currentDriveDestination; // null for stay task
    private Link currentLocation;
    private AbstractDirective directive;
    private AVStatus status;

    /** @param avVehicle
     * @param linkTimePair
     * @param currentDriveDestination
     *            null if the vehicle is in stay task */

    public RoboTaxi(AVVehicle avVehicle, LinkTimePair linkTimePair, Link currentDriveDestination) {
        GlobalAssert.that(currentDriveDestination != null);
        this.avVehicle = avVehicle;
        this.linkTimePair = linkTimePair;
        this.currentDriveDestination = currentDriveDestination;
        this.directive = null;
        this.status = AVStatus.STAY;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    /* package */ double getDivertableTime() {
        return linkTimePair.time;
    }

    /* package */ void setLinkTimePair(LinkTimePair linkTimePair) {
        this.linkTimePair = linkTimePair;
    }

    /** @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on */
    public Link getCurrentDriveDestination() {
        return currentDriveDestination;
    }

    /* package */ void setCurrentLocation(Link currentLocation) {
        GlobalAssert.that(currentLocation != null);
        this.currentLocation = currentLocation;
    }

    public Link getCurrentLocation() {
        return currentLocation;
    }

    public Schedule getSchedule() {
        return avVehicle.getSchedule();
    }

    /* package */ void setCurrentDriveDestination(Link currentDriveDestination) {
        GlobalAssert.that(currentDriveDestination != null);
        this.currentDriveDestination = currentDriveDestination;
    }

    public boolean isInStayTask() {
        boolean statusStay = status.equals(AVStatus.STAY);
        boolean scheduleStay = false;

        Task task = Schedules.getLastTask(avVehicle.getSchedule());
        if (task.getStatus().equals(Task.TaskStatus.STARTED)) {
            GlobalAssert.that(getDivertableLocation() == getCurrentLocation());
            scheduleStay = true;
        }

        GlobalAssert.that(statusStay == scheduleStay);
        return statusStay;
    }

    public Id<Vehicle> getId() {
        return avVehicle.getId();
    }

    /* package */ void setAVStatus(AVStatus status) {
        GlobalAssert.that(status != null);
        this.status = status;
    }

    public AVStatus getAVStatus() {
        return status;
    }

    protected boolean isWithoutCustomer() {
        return !status.equals(AVStatus.DRIVEWITHCUSTOMER);
    }

    /* package */ void assignDirective(AbstractDirective abstractDirective) {
        GlobalAssert.that(isWithoutDirective());
        this.directive = abstractDirective;
    }

    /* package */ boolean isWithoutDirective() {
        if (directive == null)
            return true;
        return false;
    }

    /* package */ void executeDirective() {
        directive.execute();
        directive = null;
    }

}
