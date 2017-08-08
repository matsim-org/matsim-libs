// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class RoboTaxi {
    private final AVVehicle avVehicle;
    private LinkTimePair linkTimePair;
    private Link currentDriveDestination; // null for stay task
    private AbstractDirective directive;
    private boolean isWithoutCustomer;

    /**
     * @param avVehicle
     * @param linkTimePair
     * @param currentDriveDestination
     *            null if the vehicle is in stay task
     */
    
    public RoboTaxi(AVVehicle avVehicle, LinkTimePair linkTimePair, Link currentDriveDestination) {
        this.avVehicle = avVehicle;
        this.linkTimePair = linkTimePair;
        this.currentDriveDestination = currentDriveDestination;
        this.directive = null;
        this.isWithoutCustomer = true;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    public double getDivertableTime() {
        return linkTimePair.time;
    }

    // TODO remove this function
    public LinkTimePair getLinkTimePair(){
        return linkTimePair;
    }
    
    public void setLinkTimePair(LinkTimePair linkTimePair){
        this.linkTimePair = linkTimePair;
    }
    
    /**
     * @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on
     */
    public Link getCurrentDriveDestination() {
        return currentDriveDestination;
    }

    
    public void setCurrentDriveDestination(Link currentDriveDestination){
        this.currentDriveDestination = currentDriveDestination;        
    }
    
    /**
     * @return true if vehicle is driving and divertible link is also destination link of drive task;
     *         false if vehicle is in stay task, or divertible link is not destination of drive task of vehicle.
     */
    public boolean isDivertableLocationAlsoDriveTaskDestination() {
        return getDivertableLocation() == getCurrentDriveDestination();
    }

    public boolean isVehicleInStayTask() {
        return getCurrentDriveDestination() == null;
    }
    
    // TODO remove this function as AVVehicle should not be used in dispatching layers.
    public AVVehicle getAVVehicle(){
        return avVehicle;
    }
    
    
    public AbstractDirective getDirective(){
        return directive;
    }
    
    
    public void setDirective(AbstractDirective directive){
        this.directive = directive;
    }
    
    public void setCustomerStatus(boolean isWithoutCustomer){
        this.isWithoutCustomer = isWithoutCustomer;
    }
    
    public boolean isWithoutCustomer(){
        return isWithoutCustomer;
    }
    
    

}
