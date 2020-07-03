package org.matsim.contrib.noise;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;

public class RLS19NoiseVehicleIdentifier implements NoiseVehicleIdentifier {

    private final Logger logger = Logger.getLogger(RLS19NoiseVehicleIdentifier.class);

    boolean warn = true;

    @Inject
    Scenario scenario;

    @Override
    public Id<NoiseVehicleType> identifyVehicle(Id<Vehicle> id) {
        Vehicle vehicle = VehicleUtils.findVehicle(id, scenario);
        if(vehicle == null) {
            if(warn) {
                logger.warn("No vehicle found for " + id + ", defaulting to type \"pkw\" (car). This message is only given once.");
                warn = !warn;
            }
            return RLS19VehicleType.pkw.getId();
        }
        String typeString = (String) vehicle.getType().getAttributes().getAttribute("RLS19Type");
        return RLS19VehicleType.valueOf(typeString).getId();
    }
}
