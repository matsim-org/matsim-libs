package org.matsim.contrib.emissions.types;

public enum Pollutant {

    CO("CO"), CO2_TOTAL("CO2_TOTAL"), FC("FC"), HC("HC"), NMHC("NMHC"), NOX("NOX"), NO2("NO2"), PM("PM"), SO2("SO2");

    private final String key;

    Pollutant(String key) {
        this.key = key;
    }

    public String getText() {
        return key;
    }
}
