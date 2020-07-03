package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;

public class RLS19NoiseVehicleIdentifier implements NoiseVehicleIdentifier {

    @Inject
    Scenario scenario;

    @Override
    public Id<NoiseVehicleType> identifyVehicle(Id<Vehicle> id) {
        Vehicle vehicle = VehicleUtils.findVehicle(id, scenario);
        String typeString = (String) vehicle.getType().getAttributes().getAttribute("RLS19Type");
        return RLS19VehicleType.valueOf(typeString).getId();
    }
}
