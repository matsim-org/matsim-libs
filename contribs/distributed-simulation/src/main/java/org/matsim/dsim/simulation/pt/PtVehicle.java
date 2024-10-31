package org.matsim.dsim.simulation.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.messages.VehicleMsg;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.dsim.simulation.net.SimVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class PtVehicle implements SimVehicle {

    private final SimVehicle basicVehicle;

    PtVehicle(SimVehicle basicVehicle) {
        this.basicVehicle = basicVehicle;
    }

    TransitStopFacility getNextTransitStop() {
        throw new RuntimeException("Not yet implemented");
    }

    double startBoarding() {
        throw new RuntimeException("not yet implemented");
    }

    // ------------ delegate methods to basic vehicle -------------

    @Override
    public Id<Vehicle> getId() {
        return basicVehicle.getId();
    }

    @Override
    public SimPerson getDriver() {
        return null;
    }

    @Override
    public double getMaxV() {
        return basicVehicle.getMaxV();
    }

    @Override
    public double getPce() {
        return basicVehicle.getMaxV();
    }

    @Override
    public boolean isStuck(double now) {
        return basicVehicle.isStuck(now);
    }

    @Override
    public Id<Link> getNextRouteElement() {
        return basicVehicle.getNextRouteElement();
    }

    @Override
    public Id<Link> getCurrentRouteElement() {
        return basicVehicle.getNextRouteElement();
    }

    @Override
    public double getEarliestExitTime() {
        return basicVehicle.getEarliestExitTime();
    }

    @Override
    public void advanceRoute() {
        basicVehicle.advanceRoute();
    }

    @Override
    public void setEarliestExitTime(double earliestExitTime) {
        basicVehicle.setEarliestExitTime(earliestExitTime);
    }

    @Override
    public void startStuckTimer(double now) {
        basicVehicle.startStuckTimer(now);
    }

    @Override
    public void resetStuckTimer() {
        basicVehicle.resetStuckTimer();
    }


    @Override
    public VehicleMsg toMessage() {
        throw new RuntimeException("Not yet implemented");
    }
}
