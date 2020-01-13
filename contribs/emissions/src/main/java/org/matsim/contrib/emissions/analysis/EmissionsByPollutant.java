package org.matsim.contrib.emissions.analysis;


import java.util.Map;

/**
 * Sums up emissions by pollutant. Basically wraps a hash map but is here for better
 * readability of {@link org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler}
 */
class EmissionsByPollutant {
    // The EmissionsByPollutant potentially adds up the same emissions coming from cold and warm.  Thus, this cannot be combined into the enum approach
    // without some thinking.  kai, jan'20

    private final Map<String, Double> emissionByPollutant;

    EmissionsByPollutant(Map<String, Double> emissions) {
        this.emissionByPollutant = emissions;
    }

    void addEmissions(Map<String, Double> emissions) {
        emissions.forEach(this::addEmission);
    }

    double addEmission(String pollutant, double value) {
        return emissionByPollutant.merge(pollutant, value, Double::sum);
    }

    Map<String, Double> getEmissions() {
        return emissionByPollutant;
    }

    double getEmission(String pollutant) {
        return emissionByPollutant.get(pollutant);
    }
}
