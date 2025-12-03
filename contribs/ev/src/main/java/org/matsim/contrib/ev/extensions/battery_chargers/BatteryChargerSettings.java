package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Objects;

import org.matsim.utils.objectattributes.attributable.Attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BatteryChargerSettings {
    static public final String ATTRIBUTE = "chargerSettings";

    public final double gridPower_kW;
    public final double capacity_kWh;
    public final double initialSoc;

    public BatteryChargerSettings(double gridPower_kW, double capacity_kWh, double initialSoc) {
        this.gridPower_kW = gridPower_kW;
        this.capacity_kWh = capacity_kWh;
        this.initialSoc = initialSoc;
    }

    protected final static ObjectMapper objectMapper = new ObjectMapper();

    static public void write(Attributes attributes, BatteryChargerSettings settings) {
        try {
            String raw = objectMapper.writeValueAsString(settings);
            attributes.putAttribute(ATTRIBUTE, raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static public BatteryChargerSettings read(Attributes attributes) {
        try {
            String raw = (String) Objects.requireNonNull(attributes.getAttribute(ATTRIBUTE));
            return objectMapper.readValue(raw, BatteryChargerSettings.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
