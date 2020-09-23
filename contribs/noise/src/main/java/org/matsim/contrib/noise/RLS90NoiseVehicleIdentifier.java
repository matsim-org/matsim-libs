package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

class RLS90NoiseVehicleIdentifier implements NoiseVehicleIdentifier {

    private final NoiseConfigGroup noiseParams;
    private final NoiseContext noiseContext;

    @Inject
    RLS90NoiseVehicleIdentifier(NoiseContext noiseContext) {
        noiseParams = noiseContext.getNoiseParams();
        this.noiseContext = noiseContext;
    }

    @Override
    public NoiseVehicleType identifyVehicle(Id<Vehicle> vehicleId) {
        boolean isHGV = false;
        for (String hgvPrefix : noiseParams.getHgvIdPrefixesArray()) {
            if (vehicleId.toString().startsWith(hgvPrefix)) {
                isHGV = true;
                break;
            }
        }

        if (isHGV || this.noiseContext.getBusVehicleIDs().contains(vehicleId)) {
            // HGV or Bus
            return RLS90VehicleType.hgv;
        } else {
            return RLS90VehicleType.car;
        }
    }
}
