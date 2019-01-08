package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.HashMap;
import java.util.Map;

public class EmissionEventsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final double timeBinSize;
    private Map<Integer, TimeBin<Id<Link>, WarmLinkEmissions>> timeBins = new HashMap<>();

    public EmissionEventsHandler(double timeBinSizeInSeconds) {
        this.timeBinSize = timeBinSizeInSeconds;
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {

        TimeBin<Id<Link>, WarmLinkEmissions> currentBin = getTimeBin(event.getTime());


    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {

    }

    private TimeBin<Id<Link>, WarmLinkEmissions> getTimeBin(double forTime) {
        int currentTimeBinIndex = (int) (forTime / timeBinSize);
        if (!timeBins.containsKey(currentTimeBinIndex))
            timeBins.put(currentTimeBinIndex, new TimeBin<>(currentTimeBinIndex * timeBinSize));
        return timeBins.get(currentTimeBinIndex);
    }
}
