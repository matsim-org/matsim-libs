package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EmissionsToRasterHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

    private final PalmChemistryInput palmChemistryInput;
    private final RasteredNetwork network;

    public PalmChemistryInput getPalmChemistryInput() {
        return palmChemistryInput;
    }

    public EmissionsToRasterHandler(RasteredNetwork network, double timeBinSize) {
        this.palmChemistryInput = new PalmChemistryInput(timeBinSize, network.getCellSize());
        this.network = network;
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions());
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getWarmEmissions());
    }

    private void handleEmissionEvent(double time, Id<Link> linkId, Map<Pollutant, Double> emissions) {

        if (!network.hasLink(linkId)) return;
        var cellCoords = network.getCellCoords(linkId);

        // distribute emissions onto the covered cells evenly
        // use stream instead of in place assignment since we don't know whether the incoming map is immutable or not
        var dividedEmissions = emissions.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue() / cellCoords.size()))
                .collect(Collectors.toUnmodifiableMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        for (Coord cellCoord : network.getCellCoords(linkId)) {
            palmChemistryInput.addPollution(time, cellCoord, dividedEmissions);
        }
    }
}
