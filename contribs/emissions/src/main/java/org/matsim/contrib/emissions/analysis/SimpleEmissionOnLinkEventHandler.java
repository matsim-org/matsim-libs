package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.HashMap;
import java.util.Map;

public class SimpleEmissionOnLinkEventHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

    private final Map<Id<Link>, Double> linkEmissions = new HashMap<>();

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        handleEmissionEvent(event.getLinkId(), event.getColdEmissions());
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        handleEmissionEvent(event.getLinkId(), event.getWarmEmissions());
    }

    private void handleEmissionEvent(Id<Link> linkId, Map<Pollutant, Double> emissions) {

        // sum up only single pollutant for now, to simplify implementation of blur algorithm
        // have to wrap my head around this first
        linkEmissions.merge(linkId, emissions.get(Pollutant.CO2_TOTAL), Double::sum);
    }
}
