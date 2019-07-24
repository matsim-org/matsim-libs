package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects Warm- and Cold-Emission-Events by time bin and by link-id
 */
class EmissionsOnLinkEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> timeBins;

    EmissionsOnLinkEventHandler(double timeBinSizeInSeconds) {

        this.timeBins = new TimeBinMap<>(timeBinSizeInSeconds);
    }

    /**
     * Yields collected emissions
     *
     * @return Collected emissions by time bin and by link id
     */
    TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> getTimeBins() {
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

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> currentBin = timeBins.getTimeBin(time);

        if (!currentBin.hasValue())
            currentBin.setValue(new HashMap<>());
        if (!currentBin.getValue().containsKey(linkId))
            currentBin.getValue().put(linkId, new EmissionsByPollutant(new HashMap<>(emissions)));
        else
            currentBin.getValue().get(linkId).addEmissions(emissions);
    }
}
