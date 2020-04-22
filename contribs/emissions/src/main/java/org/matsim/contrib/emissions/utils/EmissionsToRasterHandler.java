package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.Map;

public class EmissionsToRasterHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

    private final PalmChemistryInput palmChemistryInput;
    private final RasteredNetwork network;

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

        var cellCoords = network.getCellCoords(linkId);

        // distribute emissions onto the covered cells evenly
        for (Map.Entry<Pollutant, Double> entry : emissions.entrySet()) {
            entry.setValue(entry.getValue() / cellCoords.size());
        }

        for (Coord cellCoord : network.getCellCoords(linkId)) {
            palmChemistryInput.addPollution(time, cellCoord, emissions);
        }
    }
}
