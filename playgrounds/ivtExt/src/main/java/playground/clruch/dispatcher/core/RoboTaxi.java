// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;

/** RoboTaxi is central classs to be used in all dispatchers.
 * Dispatchers control a fleet of RoboTaxis, each is uniquely
 * associated to an AVVehicle object in MATSim.
 * 
 * @author Claudio Ruch */
public class RoboTaxi {
    private final AVVehicle avVehicle;
    private AVStatus status;

    private Link lastKnownLocation; // last known location of the RoboTaxi
    private Link driveDestination; // drive destination of the RoboTaxi, null for stay task
    /** location/time pair from where / when RoboTaxi path can be altered. */
    private LinkTimePair divertableLinkTime;
    private AbstractDirective directive;

    /** Standard constructor
     * 
     * @param avVehicle binding association to MATSim AVVehicle object
     * @param linkTimePair
     * @param driveDestination */
    /* package */ RoboTaxi(AVVehicle avVehicle, LinkTimePair divertableLinkTime, Link driveDestination) {
        GlobalAssert.that(driveDestination != null);
        this.avVehicle = avVehicle;
        this.divertableLinkTime = divertableLinkTime;
        this.driveDestination = driveDestination;
        this.directive = null;
        this.status = AVStatus.STAY;
    }

    // ===================================================================================
    // methods to be used by dispatchers, public

    /** @return {@link} location at which robotaxi can be diverted, i.e. a Link with an endnode
     *         at which the robotaxi path can be altered */
    public Link getDivertableLocation() {
        return divertableLinkTime.link;
    }

    /** @return time when robotaxi can be diverted */
    /* package */ double getDivertableTime() {
        return divertableLinkTime.time;
    }

    /** @return null if vehicle is currently not driving, else
     *         the final {@link Link} of the path that the vehicles is currently driving on */
    public Link getCurrentDriveDestination() {
        return driveDestination;
    }

    /** @return last konwn {@link} location of the RoboTaxi, meant for
     *         data capturing, current location is not necessarily divertablelocation
     *         from where RoboTaxi could change its path, therefore use
     *         getDivertableLocation() for computations. */
    public Link getLastKnownLocation() {
        return lastKnownLocation;
    }

    /** @return true if vehicle is staying */
    public boolean isInStayTask() {
        return status.equals(AVStatus.STAY);
    }

    /** @return {@Id<Link>} of the RoboTaxi, robotaxi ID is the same as AVVehicle ID */
    public Id<Vehicle> getId() {
        return avVehicle.getId();
    }

    /** @return {@AVStatus} of the vehicle */
    public AVStatus getAVStatus() {
        return status;
    }

    // ===================================================================================
    // methods to be used by Core package

    /** @param divertableLinkTime update the divertableLinkTime of the RoboTaxi, to be used
     *            only from VehicleMaintainer */
    /* package */ void setDivertableLinkTime(LinkTimePair divertableLinkTime) {
        GlobalAssert.that(divertableLinkTime != null);
        this.divertableLinkTime = divertableLinkTime;
    }

    /** @param currentLocation {@link} last known link of RoboTaxi location, to be used only
     *            by VehicleMaintainer in update steps. */
    /* package */ void setLastKnownLocation(Link currentLocation) {
        GlobalAssert.that(currentLocation != null);
        this.lastKnownLocation = currentLocation;
    }

    /** @param currentDriveDestination {@link} roboTaxi is driving to, to be used only
     *            by core package, use setVehiclePickup and setVehicleRebalance in dispatchers */
    /* package */ void setCurrentDriveDestination(Link currentDriveDestination) {
        GlobalAssert.that(currentDriveDestination != null);
        this.driveDestination = currentDriveDestination;
    }

    /** @param {@AVStatus} for robotaxi to be updated to, to be used only by
     *            core package, in dispatcher implementations, status will be adapted
     *            automatically. */
    /* package */ void setAVStatus(AVStatus status) {
        GlobalAssert.that(status != null);
        this.status = status;
    }

    /** @return true if customer is without a customer */
    /* package */ boolean isWithoutCustomer() {
        return !status.equals(AVStatus.DRIVEWITHCUSTOMER);
    }

    /** @return {@Schedule} of the RoboTaxi, to be used only inside core package, the schedule will
     *         be used automatically for all updates associated to pickups, rebalance tasks */
    /* package */ Schedule getSchedule() {
        return avVehicle.getSchedule();
    }

    /** @param abstractDirective to be issued to RoboTaxi when commands change, to be used only
     *            in the core package, directives will be issued automatically when
     *            setVehiclePickup,
     *            setVehicleRebalance are called. */
    /* package */ void assignDirective(AbstractDirective abstractDirective) {
        GlobalAssert.that(isWithoutDirective());
        this.directive = abstractDirective;
    }

    /** @return true if RoboTaxi is without an unexecuted directive, to be used only inside
     *         core package */
    /* package */ boolean isWithoutDirective() {
        if (directive == null)
            return true;
        return false;
    }

    /** execute the directive of a RoboTaxi, to be used only inside core package */
    /* package */ void executeDirective() {
        directive.execute();
        directive = null;
    }

}
