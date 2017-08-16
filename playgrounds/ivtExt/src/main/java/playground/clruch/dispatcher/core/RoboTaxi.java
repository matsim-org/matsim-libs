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
    // private boolean isWithoutCustomer;

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
        // this.isWithoutCustomer = true;
        this.status = AVStatus.STAY;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    public double getDivertableTime() {
        return linkTimePair.time;
    }

    // TODO remove this function
    public LinkTimePair getLinkTimePair() {
        return linkTimePair;
    }

    public void setLinkTimePair(LinkTimePair linkTimePair) {
        this.linkTimePair = linkTimePair;
    }

    /** @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on */
    public Link getCurrentDriveDestination() {
        return currentDriveDestination;
    }

    public void setCurrentLocation(Link currentLocation) {
        GlobalAssert.that(currentLocation != null);
        this.currentLocation = currentLocation;
    }

    public Link getCurrentLocation() {
        return currentLocation;
    }
    
    public Schedule getSchedule(){
        return avVehicle.getSchedule();
    }
    

   /*package*/ void setCurrentDriveDestination(Link currentDriveDestination) {
        GlobalAssert.that(currentDriveDestination != null);
        this.currentDriveDestination = currentDriveDestination;
    }

    /** @return true if vehicle is driving and divertible link is also destination link of drive
     *         task;
     *         false if vehicle is in stay task, or divertible link is not destination of drive task
     *         of vehicle. */
    public boolean isDivertableLocationAlsoDriveTaskDestination() {
        return getDivertableLocation() == getCurrentDriveDestination();
    }

    
    // TODO remove one of the two
    public boolean isVehicleInStayTask() {
        return status.equals(AVStatus.STAY);
    }
    
    public boolean isInStayTask() {
        Task task = Schedules.getLastTask(getAVVehicle().getSchedule());
        if (task.getStatus().equals(Task.TaskStatus.STARTED)){
            GlobalAssert.that(getDivertableLocation() == getCurrentLocation());
            return true;
        }
        return false;
    }
    

    // TODO can AVVehicle be removed from more layers? 
    /*package*/ AVVehicle getAVVehicle() {
        return avVehicle;
    }
    
    public Id<Vehicle> getId(){
        return avVehicle.getId();
    }
    

    public AbstractDirective getDirective() {
        return directive;
    }

    public void setDirective(AbstractDirective directive) {
        this.directive = directive;
    }

    public void setAVStatus(AVStatus status) {
        GlobalAssert.that(status != null);
        this.status = status;
    }

    public AVStatus getAVStatus() {
        return status;
    }

    public boolean isWithoutCustomer() {
        return !status.equals(AVStatus.DRIVEWITHCUSTOMER);
    }

}
