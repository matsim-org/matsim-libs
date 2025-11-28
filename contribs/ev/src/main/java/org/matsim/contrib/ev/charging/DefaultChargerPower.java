package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

public class DefaultChargerPower implements ChargerPower {
    private final ChargerSpecification charger;

    public DefaultChargerPower(ChargerSpecification charger) {
        this.charger = charger;
    }

    @Override
    public void plugVehicle(double now, ElectricVehicle vehicle) {
        // do nothing
    }

    @Override
    public void unplugVehicle(double now, ElectricVehicle vehicle) {
        // do nothing
    }

    @Override
    public double calcAvailableEnergyToCharge(double now, ElectricVehicle vehicle) {
        return Double.POSITIVE_INFINITY; // unlimited
    }

    @Override
    public double calcChargingPower(double now, ElectricVehicle vehicle) {
        return charger.getPlugPower();
    }

    @Override
    public void consumeEnergy(double energy) {
        // has no effect
    }

    @Override
    public void update(double now) {
        // has no effect
    }
}
