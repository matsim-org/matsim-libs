/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.Pollutant;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects Warm- and Cold-Emission-Events and returns them either
 * by time bin and link-id, or only by link-id.
 */
public class EmissionsOnLinkEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> timeBins;
    private final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = new HashMap<>();

	/**
	 * Drop events after end time.
	 */
	private final double endTime;

    public EmissionsOnLinkEventHandler(double timeBinSizeInSeconds) {
		this(timeBinSizeInSeconds, Double.POSITIVE_INFINITY);
    }

	public EmissionsOnLinkEventHandler(double timeBinSizeInSeconds, double endTime) {
		this.timeBins = new TimeBinMap<>(timeBinSizeInSeconds);
		this.endTime = endTime;
	}

    /**
     * Yields collected emissions
     *
     * @return Collected emissions by time bin and by link id
     */
    public TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> getTimeBins() {
        return timeBins;
    }
    /**
     * Yields summed link emissions
     *
     * @return Total emissions per pollutant by link id
     */
    public Map<Id<Link>, Map<Pollutant, Double>> getLink2pollutants() { return link2pollutants; }

    @Override
    public void reset(int iteration) {
        timeBins.clear();
        link2pollutants.clear();
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
		if (event.getTime() >= endTime)
			return;

        Map<Pollutant, Double> map = new HashMap<>(event.getWarmEmissions());
        handleEmissionEvent(event.getTime(), event.getLinkId(), map );
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
		if (event.getTime() >= endTime)
			return;

        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions());
    }

    private void handleEmissionEvent(double time, Id<Link> linkId, Map<Pollutant, Double> emissions) {

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> currentBin = timeBins.getTimeBin(time);
        if (!currentBin.hasValue()){ currentBin.setValue( new HashMap<>() ); }
        if (!currentBin.getValue().containsKey(linkId)){
            currentBin.getValue().put( linkId, new EmissionsByPollutant( new HashMap<>( emissions ) ) );
        } else { currentBin.getValue().get( linkId ).addEmissions( emissions ); }

        if (link2pollutants.get(linkId) == null) { link2pollutants.put(linkId, emissions); }
        else {
            for (Pollutant key : emissions.keySet()) {
                link2pollutants.get(linkId).merge(key, emissions.get(key), Double::sum);
            }
        }
    }
}
