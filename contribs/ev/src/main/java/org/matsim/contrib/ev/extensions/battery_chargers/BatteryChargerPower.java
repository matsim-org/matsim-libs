package org.matsim.contrib.ev.extensions.battery_chargers;

import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargerPower;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * This is an implementation of ChargerPower that assumes a charger with a
 * battery:
 * 
 * - If the battery is empty, the grid power is distributed over the charging
 * vehicles.
 * - If the battery is not empty, vehicles charge with the nominal plug power.
 * - If no vehicles are plugged, or less total nominal plug power than the grid
 * power is used, the battery is charged with the remaining power.
 * 
 * @author sebhoerl
 */
public class BatteryChargerPower implements ChargerPower {
    static public final String CHARGER_POWER_NAME = "BatteryChargerPower";

    private final BatteryChargerSettings settings;
    private final double chargingPeriod;
    private final EventsManager eventsManager;

    private double previousState_kWh = Double.NaN;
    private double state_kWh;

    private int chargingVehicles;

    private double plugOutput_kW;

    private final ChargerSpecification specification;

    public BatteryChargerPower(ChargerSpecification specification, BatteryChargerSettings settings,
            double chargingPeriod,
            EventsManager eventsManager) {
        this.settings = settings;
        this.specification = specification;
        this.chargingPeriod = chargingPeriod;
        this.eventsManager = eventsManager;
        this.state_kWh = settings.capacity_kWh * BatteryChargerSettings.getInitialSoc(specification.getAttributes(), 1.0);
        this.plugOutput_kW = EvUnits.W_to_kW(specification.getPlugPower());
    }

    @Override
    public void plugVehicle(double now, ElectricVehicle vehicle) {
        // not used
    }

    @Override
    public void unplugVehicle(double now, ElectricVehicle vehicle) {
        // not used
    }

    @Override
    public double calcMaximumEnergyToCharge(double now, ElectricVehicle vehicle) {
        double energy_kWh = plugOutput_kW * chargingPeriod / 3600.0;
        return EvUnits.kWh_to_J(energy_kWh);
    }

    @Override
    public void consumeEnergy(double energy) {
        // track vehicles
        chargingVehicles++;

        // housekeeping
        state_kWh -= EvUnits.J_to_kWh(energy);
        state_kWh = Math.max(state_kWh, 0.0);
    }

    @Override
    public void update(double now) {
        if (previousState_kWh != state_kWh) {
            previousState_kWh = state_kWh;
            eventsManager.processEvent(
                    new BatteryChargerStateEvent(now, specification.getId(), state_kWh,
                            state_kWh / settings.capacity_kWh));
        }

        // update distribution of output power
        double totalOutput_kW = 0.0;

        if (chargingVehicles > 0) {
            plugOutput_kW = EvUnits.W_to_kW(specification.getPlugPower());

            if (state_kWh == 0.0) {
                plugOutput_kW = settings.gridPower_kW / chargingVehicles;
            }

            totalOutput_kW = plugOutput_kW * chargingVehicles;
        }

        // charge the battery
        if (totalOutput_kW < settings.gridPower_kW) {
            state_kWh += (settings.gridPower_kW - totalOutput_kW) * chargingPeriod / 3600.0;
            state_kWh = Math.min(state_kWh, settings.capacity_kWh);
        }

        // reset for next iteration
        chargingVehicles = 0;
    }

    static public class Factory implements ChargerPower.Factory {
        private final double chargingPeriod;
        private final EventsManager eventsManager;

        public Factory(double chargingPeriod, EventsManager eventsManager) {
            this.chargingPeriod = chargingPeriod;
            this.eventsManager = eventsManager;
        }

        @Override
        public ChargerPower create(ChargerSpecification charger) {
            BatteryChargerSettings settings = BatteryChargerSettings.read(charger.getAttributes());
            return new BatteryChargerPower(charger, settings, chargingPeriod, eventsManager);
        }
    }
}
