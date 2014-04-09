/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.julia.responsibilityOffline;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonWarmEventHandlerRelativeDurations implements WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerPersonWarmEventHandlerRelativeDurations.class);

	Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

	private HashMap<Double, Double[][]> relativeDurationFactor;

	private double timeBinSize = 60*60.;

	private Map<Id, Integer> links2xbins;

	private Map<Id, Integer> links2ybins;

	public EmissionsPerPersonWarmEventHandlerRelativeDurations(HashMap<Double, Double[][]> relativeDurationFactor, Map<Id, Integer> links2xbins, Map<Id, Integer> links2ybins) {
		this.relativeDurationFactor = relativeDurationFactor;
		this.links2xbins=links2xbins;
		this.links2ybins=links2ybins;
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(warmEmissionsTotal.get(vehicleId) != null){
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(vehicleId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				
					Double timeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
					Id linkId = event.getLinkId();
					try {
						int xCell = links2xbins.get(linkId);
						int yCell = links2ybins.get(linkId);
						Double relativeFactor = relativeDurationFactor
								.get(timeBin)[xCell][yCell];
						if (relativeFactor != null) {
							eventValue = eventValue * relativeFactor;
						}
					} catch (NullPointerException e) {
						// nothing to do not in research area
					}
				
				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
			}
			warmEmissionsTotal.put(vehicleId, warmEmissionsSoFar);
		} else {
			warmEmissionsTotal.put(vehicleId, warmEmissionsOfEvent);
		}
	}

	public Map<Id, Map<WarmPollutant, Double>> getWarmEmissionsPerPerson() {
		return warmEmissionsTotal;
	}

	public void reset(int iteration) {
		warmEmissionsTotal.clear();
	}
}
