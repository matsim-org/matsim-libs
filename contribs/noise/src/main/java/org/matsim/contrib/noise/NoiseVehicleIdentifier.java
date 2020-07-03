package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public interface NoiseVehicleIdentifier {

    Id<NoiseVehicleType> identifyVehicle(Id<Vehicle> id);
}
