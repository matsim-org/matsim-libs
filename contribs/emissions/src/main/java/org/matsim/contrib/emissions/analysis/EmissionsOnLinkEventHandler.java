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
 * Collects Warm- and Cold-Emission-Events by time bin and by link-id
 */
public class EmissionsOnLinkEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> timeBins;

    public EmissionsOnLinkEventHandler(double timeBinSizeInSeconds) {

        this.timeBins = new TimeBinMap<>(timeBinSizeInSeconds);
    }

    /**
     * Yields collected emissions
     *
     * @return Collected emissions by time bin and by link id
     */
    public TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> getTimeBins() {
        return timeBins;
    }

    @Override
    public void reset(int iteration) {
        timeBins.clear();
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        Map<Pollutant,Double> map = new HashMap<>() ;
        for( Map.Entry<Pollutant, Double> entry : event.getWarmEmissions().entrySet() ){
            map.put( entry.getKey(), entry.getValue() ) ;
        }
        handleEmissionEvent(event.getTime(), event.getLinkId(), map );
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {

        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions());
    }

    private void handleEmissionEvent(double time, Id<Link> linkId, Map<Pollutant, Double> emissions) {

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> currentBin = timeBins.getTimeBin(time);

        if (!currentBin.hasValue()){
            currentBin.setValue( new HashMap<>() );
        }
        if (!currentBin.getValue().containsKey(linkId)){
            currentBin.getValue().put( linkId, new EmissionsByPollutant( new HashMap<>( emissions ) ) );
        } else{
            currentBin.getValue().get( linkId ).addEmissions( emissions );
        }
    }
}
