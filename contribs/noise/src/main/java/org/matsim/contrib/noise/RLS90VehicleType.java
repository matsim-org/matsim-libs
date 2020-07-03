package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;

enum RLS90VehicleType implements NoiseVehicleType  {
    car, hgv;

    private Id<NoiseVehicleType> id = Id.create(this.name(), NoiseVehicleType.class);

    @Override
    public Id<NoiseVehicleType> getId() {
        return id;
    }
}
