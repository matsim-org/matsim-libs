package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Objects;
import java.util.Optional;

import org.matsim.utils.objectattributes.attributable.Attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BatteryChargerSettings {
    static public final String ATTRIBUTE = "chargerSettings";
    static public final String INITIAL_SOC = "initialSoc";

    public final double gridPower_kW;
    public final double capacity_kWh;

    public BatteryChargerSettings(double gridPower_kW, double capacity_kWh) {
        this.gridPower_kW = gridPower_kW;
        this.capacity_kWh = capacity_kWh;
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

    static public void setInitialSoc(Attributes attributes, double initialSoc) {
        attributes.putAttribute(INITIAL_SOC, initialSoc);
    }

    static public Double getInitialSoc(Attributes attributes, double defaultValue) {
        return Optional.ofNullable((Double) attributes.getAttribute(INITIAL_SOC)).orElse(defaultValue);
    }
}
