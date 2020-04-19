package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.Map;

public class EmissionsToRasterHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

    private final TimeBinMap<EmissionRaster> timeBins;
    private final Network network;

    public EmissionsToRasterHandler(double timeBinSize, Network network) {
        this.timeBins = new TimeBinMap<>(timeBinSize);
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

        var currentBin = timeBins.getTimeBin(time);

        if (!currentBin.hasValue()) {
            currentBin.setValue(new EmissionRaster(500, network));
        }

        var raster = currentBin.getValue();

        raster.addEmissions(linkId, emissions);
    }
}
