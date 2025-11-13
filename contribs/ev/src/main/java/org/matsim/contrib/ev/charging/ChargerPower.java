package org.matsim.contrib.ev.charging;

import java.util.Collection;

import org.matsim.contrib.ev.charging.ChargingLogic.ChargingVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * This interface decides how much power a charger can still provide and at
 * which speed. In a way it is the "charger perspective" whilch ChargingPower is
 * the "vehicle perspective". For historical reasons, the interfaces (more the
 * latter) have a bit ambiguous names.
 * 
 * TODO: Eventually, rename ChargingPower to VehicleChargingPower and this one
 * to ChargerChargingPower. Both could even be hidden behind an interface that
 * takes both as an argument. But it should be possible to instantiate them
 * independently. For instance, the charging power logic of a charger should not
 * be created by a factory that only takes a vehicle.
 */
public interface ChargerPower {
    interface Factory {
        ChargerPower create(ChargerSpecification charger);
    }

    double calcMaximumChargingPower(Collection<ChargingVehicle> vehicles);

    double calcMaximumEnergy();

    void consumeEnergy(double energy);

    void update(double chargePeriod, double now, Collection<ChargingVehicle> vehicles);
}
