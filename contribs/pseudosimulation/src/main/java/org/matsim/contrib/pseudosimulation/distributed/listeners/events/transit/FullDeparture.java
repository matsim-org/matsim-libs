package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * Created by fouriep on 12/15/14.
 */
class FullDeparture{
    private final Id<Departure> fullDepartureId;
    private final Id<TransitLine> lineId;
    private final Id<TransitRoute> routeId;
    private final Id<Vehicle> vehicleId;
    private final Id<Departure> departureId;

    public FullDeparture(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<Vehicle> vehicleId, Id<Departure> departureId) {
        super();
        this.lineId = lineId;
        this.routeId = routeId;
        this.vehicleId = vehicleId;
        this.departureId = departureId;
        fullDepartureId = Id.create(lineId.toString() + "_" + routeId.toString() + "_" + vehicleId.toString()
                + "_" + departureId.toString(), Departure.class);
    }

    public Id<Departure> getFullDepartureId() {
        return fullDepartureId;
    }

    public Id<TransitLine> getLineId() {
        return lineId;
    }

    public Id<TransitRoute> getRouteId() {
        return routeId;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public Id<Departure> getDepartureId() {
        return departureId;
    }
}