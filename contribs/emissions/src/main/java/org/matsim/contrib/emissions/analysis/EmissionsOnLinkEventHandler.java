package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.Pollutant;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EmissionsOnLinkEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final TimeBinMap<Map<Id<Link>, LinkEmissions>> timeBins;

    public EmissionsOnLinkEventHandler(double timeBinSizeInSeconds) {

        this.timeBins = new TimeBinMap<>(timeBinSizeInSeconds);
    }

    public TimeBinMap<Map<Id<Link>, LinkEmissions>> getTimeBins() {
        return timeBins;
    }

    @Override
    public void reset(int iteration) {
        timeBins.clear();
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {

        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getWarmEmissions());
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {

        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions());
    }

    private void handleEmissionEvent(double time, Id<Link> linkId, Map<String, Double> emissions) {

        TimeBinMap.TimeBin<Map<Id<Link>, LinkEmissions>> currentBin = timeBins.getTimeBin(time);

        Map<Pollutant, Double> typedEmissions = emissions.entrySet().stream()
                .collect(Collectors.toMap(entry -> Pollutant.valueOf(entry.getKey()), Map.Entry::getValue));

        if (!currentBin.hasValue())
            currentBin.setValue(new HashMap<>());
        if (!currentBin.getValue().containsKey(linkId))
            currentBin.getValue().put(linkId, new LinkEmissions(linkId, typedEmissions));
        else
            currentBin.getValue().get(linkId).addEmissions(typedEmissions);
    }
}
