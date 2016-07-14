/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonColdEventHandler.java
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
package playground.juliakern.responsibilityOffline;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.vehicles.Vehicle;


/**
 * @author benjamin, julia
 *
 */
public class EmissionCostPerPersonColdEventHandlerRelativeDurations implements ColdEmissionEventHandler {

	Map<Id<Vehicle>, Map<ColdPollutant, Double>> coldEmissionCostTotal = new HashMap<>();

	private Map<Id<Link>, Integer> links2xbins;

	private Map<Id<Link>, Integer> links2ybins;

	private double timeBinSize = 60*60.;

	/*Values taken from IMPACT (Maibach et al.(2008))*/
	private final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	
	private HashMap<Double, Double[][]> relativeDurationFactor;

	public EmissionCostPerPersonColdEventHandlerRelativeDurations(
			HashMap<Double, Double[][]> relativeDurationFactor, Map<Id<Link>, Integer> links2xbins, Map<Id<Link>, Integer> links2ybins) {
		this.relativeDurationFactor = relativeDurationFactor;
		this.links2xbins = links2xbins;
		this.links2ybins = links2ybins;
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions();

		if(coldEmissionCostTotal.get(vehicleId) != null){
			Map<ColdPollutant, Double> coldEmissionCostsSoFar = coldEmissionCostTotal.get(vehicleId);
			for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
				ColdPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				Double costFactor = costOfPollutant(pollutant);
				Double eventCosts = 0.0;
				
					Double timeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
					Id<Link> linkId = event.getLinkId();
					try {
						int xCell = links2xbins.get(linkId);
						int yCell = links2ybins.get(linkId);
						Double relativeFactor = relativeDurationFactor
								.get(timeBin)[xCell][yCell];
						if (relativeFactor != null ) {
							eventCosts = eventValue * relativeFactor * costFactor;
						}
					} catch (NullPointerException e) {
						// nothing to do not in research area
					}
				
				Double previousCosts = coldEmissionCostsSoFar.get(pollutant);
				Double newValue;
				if(previousCosts!=null){
					newValue = previousCosts + eventCosts;
				}else{
					newValue = eventCosts;
				}
				if(newValue>0.0)coldEmissionCostsSoFar.put(pollutant, newValue);
			}
			coldEmissionCostTotal.put(vehicleId, coldEmissionCostsSoFar);
		} else {
			Map<ColdPollutant, Double> coldEmissionCostsOfEvent = new HashMap <ColdPollutant, Double>();
			for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
				ColdPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				Double costFactor = costOfPollutant(pollutant);
				Double eventCosts = 0.0;
				
					Double timeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
					Id<Link> linkId = event.getLinkId();
					try {
						int xCell = links2xbins.get(linkId);
						int yCell = links2ybins.get(linkId);
						Double relativeFactor = relativeDurationFactor
								.get(timeBin)[xCell][yCell];
						if (relativeFactor != null) {
							eventCosts = eventValue * relativeFactor * costFactor;
							if(eventCosts>0.0)coldEmissionCostsOfEvent.put(pollutant, eventCosts);
						}
					} catch (NullPointerException e) {
						// nothing to do not in research area
					}
			}
			coldEmissionCostTotal.put(vehicleId, coldEmissionCostsOfEvent);
		}
	}

	public Map<Id<Vehicle>, Map<ColdPollutant, Double>> getColdEmissionCostsPerPerson() {
		return coldEmissionCostTotal;
	}

	private Double costOfPollutant(ColdPollutant pollutant) {
		switch(pollutant){
			case NOX : return EURO_PER_GRAMM_NOX;
			case NMHC: return EURO_PER_GRAMM_NMVOC;
			case PM: 	return EURO_PER_GRAMM_PM2_5_EXHAUST;
			default: return 0.0;
		}
	}
	
	@Override
	public void reset(int iteration) {
		coldEmissionCostTotal.clear();
	}
}
