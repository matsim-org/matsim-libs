package org.matsim.dsim.simulation.net;

import lombok.Getter;
import lombok.Setter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.vehicles.Vehicle;

import java.util.Objects;

@Deprecated
public class BasicSimVehicle implements SimVehicle {

    @Getter
    @Setter
    private double earliestExitTime;

    @Getter
    private final Id<Vehicle> id;
    @Getter
    private final SimPerson driver;
    @Getter
    private final double pce;
    @Getter
    private final double maxV;
    private final StuckTimer stuckTimer;

    public BasicSimVehicle(Id<Vehicle> id, SimPerson driver, double pce, double maxV, double stuckThreshold) {
        this.id = id;
        this.driver = driver;
        this.pce = pce;
        this.maxV = maxV;
        this.stuckTimer = new StuckTimer(stuckThreshold);
    }

    public boolean isStuck(double now) {
        return stuckTimer.isStuck(now);
    }

    public void startStuckTimer(double now) {
        stuckTimer.start(now);
    }

    public Id<Link> getNextRouteElement() {
        return driver.getRouteElement(SimPerson.RouteAccess.Next);
    }

    public Id<Link> getCurrentRouteElement() {
        return driver.getRouteElement(SimPerson.RouteAccess.Curent);
    }

    public void advanceRoute() {
        driver.advanceRoute(SimPerson.Advance.One);
    }

    public void resetStuckTimer() {
        stuckTimer.reset();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicSimVehicle that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
