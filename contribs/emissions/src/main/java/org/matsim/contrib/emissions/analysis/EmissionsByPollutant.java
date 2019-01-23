package org.matsim.contrib.emissions.analysis;

import org.matsim.contrib.emissions.types.Pollutant;

import java.util.Map;

/**
 * Sums up emissions by pollutant. Basically wraps a hash map but is here for better
 * readability of {@link org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler}
 */
class EmissionsByPollutant {

    private Map<Pollutant, Double> emissionByPollutant;

    EmissionsByPollutant(Map<Pollutant, Double> emissions) {
        this.emissionByPollutant = emissions;
    }

    void addEmissions(Map<Pollutant, Double> emissions) {
        emissions.forEach(this::addEmission);
    }

    double addEmission(Pollutant pollutant, double value) {

        return emissionByPollutant.merge(pollutant, value, Double::sum);
    }

    Map<Pollutant, Double> getEmissions() {
        return emissionByPollutant;
    }

    double getEmission(Pollutant pollutant) {
        return emissionByPollutant.get(pollutant);
    }
}
