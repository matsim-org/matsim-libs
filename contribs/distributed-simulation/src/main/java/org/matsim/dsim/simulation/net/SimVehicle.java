package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.vehicles.Vehicle;

@Deprecated
public interface SimVehicle {

    // property access
    Id<Vehicle> getId();

    SimPerson getDriver();

    double getMaxV();

    double getPce();

    // state acces
    boolean isStuck(double now);

    Id<Link> getNextRouteElement();

    Id<Link> getCurrentRouteElement();

    double getEarliestExitTime();

    // modify state
    void advanceRoute();

    void setEarliestExitTime(double earliestExitTime);

    void startStuckTimer(double now);

    void resetStuckTimer();

    // transform
//    VehicleMsg toMessage();
}
