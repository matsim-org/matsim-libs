package org.matsim.contrib.noise;

import com.google.common.collect.Range;
import org.matsim.api.core.v01.Identifiable;

public interface NoiseVehicleType extends Identifiable<NoiseVehicleType> {
    Range<Double> getValidSpeedRange();
}
