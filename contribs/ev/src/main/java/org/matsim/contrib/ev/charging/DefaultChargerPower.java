package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

public class DefaultChargerPower implements ChargerPower {
    private final ChargerSpecification charger;

    public DefaultChargerPower(ChargerSpecification charger) {
        this.charger = charger;
    }

    @Override
    public double calcMaximumChargingPower() {
        return charger.getPlugPower();
    }

    @Override
    public double calcMaximumEnergy() {
        // unlimited output
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void consumeEnergy(double energy) {
        // do nothing
    }
}
