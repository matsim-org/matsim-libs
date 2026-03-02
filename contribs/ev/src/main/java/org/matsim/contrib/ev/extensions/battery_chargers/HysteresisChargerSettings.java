package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Objects;

import org.matsim.utils.objectattributes.attributable.Attributes;

import com.fasterxml.jackson.core.JsonProcessingException;

public class HysteresisChargerSettings extends BatteryChargerSettings {
    public double batteryChargingPower_kW;

    public double highOutputPower_kW;
    public double lowOutputPower_kW;

    public double highToLowPowerThreshold_kWh;
    public double lowToHighPowerThreshold_kWh;

    public HysteresisChargerSettings(double gridPower_kW, double capacity_kWh, //
            double batteryChargingPower_kW, //
            double highOutputPower_kW, double lowOutputPower_kW, //
            double highToLowPowerThreshold_kWh, double lowToHighPowerThreshold_kWh) {
        super(gridPower_kW, capacity_kWh);

        this.batteryChargingPower_kW = batteryChargingPower_kW;
        this.highOutputPower_kW = highOutputPower_kW;
        this.lowOutputPower_kW = lowOutputPower_kW;
        this.highToLowPowerThreshold_kWh = highToLowPowerThreshold_kWh;
        this.lowToHighPowerThreshold_kWh = lowToHighPowerThreshold_kWh;
    }

    public HysteresisChargerSettings(BatteryChargerSettings settings, //
            double batteryChargingPower_kW, //
            double highOutputPower_kW, double lowOutputPower_kW, //
            double highToLowPowerThreshold_kWh, double lowToHighPowerThreshold_kWh) {
        super(settings.gridPower_kW, settings.capacity_kWh);

        this.batteryChargingPower_kW = batteryChargingPower_kW;
        this.highOutputPower_kW = highOutputPower_kW;
        this.lowOutputPower_kW = lowOutputPower_kW;
        this.highToLowPowerThreshold_kWh = highToLowPowerThreshold_kWh;
        this.lowToHighPowerThreshold_kWh = lowToHighPowerThreshold_kWh;
    }

    static public void write(Attributes attributes, HysteresisChargerSettings settings) {
        try {
            String raw = objectMapper.writeValueAsString(settings);
            attributes.putAttribute(ATTRIBUTE, raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static public HysteresisChargerSettings read(Attributes attributes) {
        try {
            String raw = (String) Objects.requireNonNull(attributes.getAttribute(ATTRIBUTE));
            return objectMapper.readValue(raw, HysteresisChargerSettings.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
