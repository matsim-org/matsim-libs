package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class VehicleLinkPair {
    public final AVVehicle avVehicle;
    public final LinkTimePair linkTimePair;
    private final Link currentDriveDestination; // null for stay task

    /**
     * @param avVehicle
     * @param linkTimePair
     * @param currentDriveDestination
     *            null if the vehicle is in stay task
     */
    public VehicleLinkPair(AVVehicle avVehicle, LinkTimePair linkTimePair, Link currentDriveDestination) {
        this.avVehicle = avVehicle;
        this.linkTimePair = linkTimePair;
        this.currentDriveDestination = currentDriveDestination;
    }

    public Link getDivertableLocation() {
        return linkTimePair.link;
    }

    /**
     * @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on
     */
    public Link getCurrentDriveDestination() {
        return currentDriveDestination;
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

}
