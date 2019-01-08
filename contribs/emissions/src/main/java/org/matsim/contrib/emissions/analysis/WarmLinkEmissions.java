package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class WarmLinkEmissions {

    private final Link link;
    private final Map<WarmPollutant, Double> pollutionByPollutant;

    public WarmLinkEmissions(final Link link) {
        this.link = link;
        this.pollutionByPollutant = Arrays.stream(WarmPollutant.values()).collect(Collectors.toMap(p -> p, p -> 0.0));
    }
}
