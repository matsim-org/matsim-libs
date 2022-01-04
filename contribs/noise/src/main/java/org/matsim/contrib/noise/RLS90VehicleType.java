package org.matsim.contrib.noise;

import com.google.common.collect.Range;
import org.matsim.api.core.v01.Id;

enum RLS90VehicleType implements NoiseVehicleType  {

    /**
     * pkw
     */
    car(30,130),
    /**
     * lkw
     */
    hgv(30,80);

    private final Range<Double> validSpeedRange;

    RLS90VehicleType(double lowerSpeedBound, double upperSpeedBound) {
        this.validSpeedRange = Range.closed(lowerSpeedBound, upperSpeedBound);
    }

    private Id<NoiseVehicleType> id = Id.create(this.name(), NoiseVehicleType.class);

    @Override
    public Id<NoiseVehicleType> getId() {
        return id;
    }

    @Override
    public Range<Double> getValidSpeedRange() {
        return validSpeedRange;
    }
}
