package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.Pollutant;

import java.util.Map;

public class LinkEmissions {

    private final Id<Link> linkId;
    private final Map<Pollutant, Double> emissionByPollutant;

    public LinkEmissions(final Id<Link> linkId, Map<Pollutant, Double> emissions) {
        this.linkId = linkId;
        this.emissionByPollutant = emissions;
    }

    public void addEmissions(Map<Pollutant, Double> emissions) {

        emissions.forEach(this::addEmission);
    }

    public double addEmission(Pollutant pollutant, double value) {

        if (emissionByPollutant.containsKey(pollutant))
            value += emissionByPollutant.get(pollutant);
        emissionByPollutant.put(pollutant, value);
        return value;
    }

    public Map<Pollutant, Double> getEmissions() {
        return emissionByPollutant;
    }

    public double getEmission(Pollutant pollutant) {
        return emissionByPollutant.get(pollutant);
    }
}
