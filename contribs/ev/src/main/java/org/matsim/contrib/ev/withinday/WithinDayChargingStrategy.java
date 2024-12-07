package org.matsim.contrib.ev.withinday;

import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.vehicles.Vehicle;

/**
 * This charging strategy makes vehicles *not* unplug automatically at a
 * charger. Instead, the vehicles remain plugged until they get unplugged by the
 * owner. Furthermore, vehicles are only charged up to a maximum SoC that can be
 * configured per vehicle.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayChargingStrategy implements ChargingStrategy {
    static public final String MAXIMUM_SOC_VEHICLE_ATTRIBUTE = "wevc:maximumSoc";

    private final ChargerSpecification charger;
    private final ElectricVehicle ev;

    public WithinDayChargingStrategy(ChargerSpecification charger, ElectricVehicle ev) {
        this.charger = charger;
        this.ev = ev;
    }

    @Override
    public double calcRemainingEnergyToCharge() {
        Double maximumSoc = getMaximumSoc(ev);

        if (maximumSoc == null) {
            maximumSoc = 1.0;
        }

        Battery battery = ev.getBattery();
        return maximumSoc * battery.getCapacity() - battery.getCharge();
    }

    @Override
    public double calcRemainingTimeToCharge() {
        return ((BatteryCharging) ev.getChargingPower()).calcChargingTime(charger, calcRemainingEnergyToCharge());
    }

    @Override
    public boolean isChargingCompleted() {
        if (getMaximumSoc(ev) != null) {
            return false;
        } else {
            return calcRemainingEnergyToCharge() <= 0;
        }
    }

    private Double getMaximumSoc(ElectricVehicle ev) {
        return getMaximumSoc(ev.getVehicleSpecification().getMatsimVehicle());
    }

    /**
     * Returns the maximum SoC of a vehicle.
     */
    static public Double getMaximumSoc(Vehicle vehicle) {
        return (Double) vehicle.getAttributes()
                .getAttribute(MAXIMUM_SOC_VEHICLE_ATTRIBUTE);
    }

    /**
     * Sets the maximum SoC of a vehicle.
     */
    static public void setMaximumSoc(Vehicle vehicle, double maximumSoc) {
        vehicle.getAttributes().putAttribute(MAXIMUM_SOC_VEHICLE_ATTRIBUTE, maximumSoc);
    }

    public static class Factory implements ChargingStrategy.Factory {
        @Override
        public ChargingStrategy createStrategy(ChargerSpecification charger, ElectricVehicle ev) {
            return new WithinDayChargingStrategy(charger, ev);
        }
    }
}