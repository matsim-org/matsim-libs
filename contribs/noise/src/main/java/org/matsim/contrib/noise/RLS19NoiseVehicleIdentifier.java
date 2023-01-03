package org.matsim.contrib.noise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;

import static org.matsim.vehicles.VehicleUtils.getOrCreateAllvehicles;

public class RLS19NoiseVehicleIdentifier implements NoiseVehicleIdentifier {

    private final Logger logger = LogManager.getLogger(RLS19NoiseVehicleIdentifier.class);

    boolean warn = true;

    @Inject
    Scenario scenario;

    @Override
    public NoiseVehicleType identifyVehicle(Id<Vehicle> id) {
//        Vehicle vehicle = VehicleUtils.findVehicle(id, scenario);
        Vehicle vehicle = getOrCreateAllvehicles( scenario ).getVehicles().get( id );
        if(vehicle == null) {
            if(warn) {
                logger.warn("No vehicle found for " + id + ", defaulting to type \"pkw\" (car). This message is only given once.");
                warn = !warn;
            }
            return RLS19VehicleType.pkw;
        }
        String typeString = (String) vehicle.getType().getAttributes().getAttribute("RLS19Type");
        return RLS19VehicleType.valueOf(typeString);
    }
}
