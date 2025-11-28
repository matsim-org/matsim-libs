package org.matsim.contrib.ev.extensions.battery_chargers;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargerPower;
import org.matsim.contrib.ev.charging.DefaultChargerPower;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * This is a hysteresis-based implementation of the ChargerPower for a
 * battery-based charger.
 * 
 * - When no vehicle is plugged, the battery is charged with
 * `min(batteryChargingPower_kW, gridPower_kW)`
 * -
 * - In "high power" state, vehicles charge with `highOutputPower_kW`
 * - In "low power" state, vehicles charge with `lowOutputPower_kWh`
 * 
 * - When a vehicle is plugged, the battery is charged with the remaining grid
 * power, but with `batteryChargingPower_kW` at most
 * 
 * - The switch from "high power" to "low power" happens when the
 * `highToLowPowerThreshold_kW` is passed downwards
 * - The switch from "low power" to "high power" happens when the
 * `lowToHighPowerThreshold_kW` is passed upwards
 */
public class HysteresisChargerPower implements ChargerPower {
    static public final String CHARGER_POWER_NAME = "HysteresisBatteryChargerPower";
    static public final String BATTERY_CHARGER_ATTRIBUTE = "hysteresisSettings";

    static public record Settings( //
            double batteryChargingPower_kW, //
            double maximumGridPower_kW, //

            double highOutputPower_kW, //
            double lowOutputPower_kW, //

            double highToLowPowerThreshold_kWh, //
            double lowToHighPowerThreshold_kWh, //

            double batteryCapacity_kWh, //
            double initialSoc) {
    }

    private final Id<Charger> chargerId;
    private final Settings settings;
    private final double chargingPeriod;
    private final EventsManager eventsManager;

    private enum LogicState {
        HIGH_POWER, LOW_POWER
    }

    private LogicState logicState;

    private double previousBatteryState_kWh = Double.NaN;
    private double batteryState_kWh;

    private boolean vehiclePlugged = false;

    public HysteresisChargerPower(Settings settings, double chargingPeriod, EventsManager eventsManager,
            Id<Charger> chargerId) {
        this.settings = settings;
        this.chargingPeriod = chargingPeriod;
        this.eventsManager = eventsManager;
        this.chargerId = chargerId;

        this.batteryState_kWh = settings.batteryCapacity_kWh * settings.initialSoc;

        this.logicState = batteryState_kWh < settings.highToLowPowerThreshold_kWh ? LogicState.LOW_POWER
                : LogicState.HIGH_POWER;
    }

    @Override
    public void plugVehicle(double now, ElectricVehicle vehicle) {
        Preconditions.checkState(!vehiclePlugged);

        if (batteryState_kWh > settings.highToLowPowerThreshold_kWh) {
            logicState = LogicState.HIGH_POWER;
        }

        vehiclePlugged = true;
    }

    @Override
    public void unplugVehicle(double now, ElectricVehicle vehicle) {
        Preconditions.checkState(vehiclePlugged);
        vehiclePlugged = false;
    }

    @Override
    public double calcMaximumEnergyToCharge(double now, ElectricVehicle vehicle) {
        final double chargingPower_kW;

        if (logicState.equals(LogicState.HIGH_POWER)) {
            chargingPower_kW = settings.highOutputPower_kW;
        } else {
            chargingPower_kW = settings.lowOutputPower_kW;
        }

        double energy_kWh = chargingPower_kW * chargingPeriod / 3600.0;
        energy_kWh = Math.min(energy_kWh, batteryState_kWh);

        return EvUnits.kWh_to_J(energy_kWh);
    }

    @Override
    public void consumeEnergy(double energy) {
        // possible that more energy got drawn than battery
        batteryState_kWh -= EvUnits.J_to_kWh(energy);
        batteryState_kWh = Math.max(0.0, batteryState_kWh);
    }

    @Override
    public void update(double now) {
        // I) Battery charging
        double chargingPower_kW = Math.min(settings.batteryChargingPower_kW, settings.maximumGridPower_kW);

        if (vehiclePlugged) {
            if (logicState.equals(LogicState.HIGH_POWER)) {
                chargingPower_kW = Math.max(0.0,
                        Math.min(chargingPower_kW, settings.maximumGridPower_kW - settings.highOutputPower_kW));
            } else {
                chargingPower_kW = Math.max(0.0,
                        Math.min(chargingPower_kW, settings.maximumGridPower_kW - settings.lowOutputPower_kW));
            }
        }

        batteryState_kWh += chargingPower_kW * chargingPeriod / 3600.0;
        batteryState_kWh = Math.min(batteryState_kWh, settings.batteryCapacity_kWh);

        // II) Logic
        if (Double.isFinite(previousBatteryState_kWh)) {
            // here we simulate the hysteresis logic

            if (previousBatteryState_kWh >= settings.highToLowPowerThreshold_kWh
                    && batteryState_kWh < settings.highToLowPowerThreshold_kWh) {
                // we go into low power charging mode
                logicState = LogicState.LOW_POWER;
            }

            if (previousBatteryState_kWh <= settings.lowToHighPowerThreshold_kWh
                    && batteryState_kWh > settings.lowToHighPowerThreshold_kWh) {
                // we go into high power charging mode
                logicState = LogicState.HIGH_POWER;
            }
        }

        if (previousBatteryState_kWh != batteryState_kWh) {
            previousBatteryState_kWh = batteryState_kWh;

            eventsManager.processEvent(new BatteryChargerStateEvent(now, chargerId, batteryState_kWh,
                    batteryState_kWh / settings.batteryCapacity_kWh));
        }
    }

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static public class Factory implements ChargerPower.Factory {
        private final ChargerPower.Factory delegate;
        private final double chargingPeriod;
        private final EventsManager eventsManager;

        public Factory(ChargerPower.Factory delegate, double chargingPeriod, EventsManager eventsManager) {
            this.delegate = delegate;
            this.chargingPeriod = chargingPeriod;
            this.eventsManager = eventsManager;
        }

        public Factory(double chargingPeriod, EventsManager eventsManager) {
            this(new DefaultChargerPower.Factory(chargingPeriod), chargingPeriod, eventsManager);
        }

        @Override
        public ChargerPower create(ChargerSpecification charger) {
            try {
                String raw = (String) charger.getAttributes().getAttribute(BATTERY_CHARGER_ATTRIBUTE);

                if (raw != null) {
                    Settings settings = objectMapper.readValue(raw, Settings.class);
                    return new HysteresisChargerPower(settings, chargingPeriod, eventsManager, charger.getId());
                } else {
                    return delegate.create(charger);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static public void adaptAttributes(Attributes attributes, HysteresisChargerPower.Settings settings) {
        try {
            attributes.putAttribute(CompositeChargerPowerFactory.CHARGER_ATTRIBUTE,
                    HysteresisChargerPower.CHARGER_POWER_NAME);
            attributes.putAttribute(BATTERY_CHARGER_ATTRIBUTE,
                    objectMapper.writeValueAsString(settings));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
