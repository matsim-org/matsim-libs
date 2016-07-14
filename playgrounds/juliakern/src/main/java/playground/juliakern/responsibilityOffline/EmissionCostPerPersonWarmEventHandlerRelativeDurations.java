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
package playground.juliakern.responsibilityOffline;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.vehicles.Vehicle;


/**
 * @author benjamin, julia
 *
 */
public class EmissionCostPerPersonWarmEventHandlerRelativeDurations implements WarmEmissionEventHandler {

	Map<Id<Vehicle>, Map<WarmPollutant, Double>> warmEmissionCostTotal = new HashMap<>();

	private HashMap<Double, Double[][]> relativeDurationFactor;

	private double timeBinSize = 60*60.;
	/*Values taken from IMPACT (Maibach et al.(2008))*/
	private final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);

	private Map<Id<Link>, Integer> links2xbins;

	private Map<Id<Link>, Integer> links2ybins;

	private boolean considerCO2 =true;

	public EmissionCostPerPersonWarmEventHandlerRelativeDurations(HashMap<Double, Double[][]> relativeDurationFactor, Map<Id<Link>, Integer> links2xbins, Map<Id<Link>, Integer> links2ybins) {
		this.relativeDurationFactor = relativeDurationFactor;
		this.links2xbins=links2xbins;
		this.links2ybins=links2ybins;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(warmEmissionCostTotal.get(vehicleId) != null){
			Map<WarmPollutant, Double> warmEmissionCostsSoFar = warmEmissionCostTotal.get(vehicleId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
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
				
				Double previousCosts = warmEmissionCostsSoFar.get(pollutant);
				Double newValue;
				if(previousCosts!=null){
					newValue = previousCosts + eventCosts;
				}else{
					newValue = eventCosts;
				}
				if(newValue>0.0)warmEmissionCostsSoFar.put(pollutant, newValue);
			}
			warmEmissionCostTotal.put(vehicleId, warmEmissionCostsSoFar);
		} else {
			Map<WarmPollutant, Double> warmEmissionCostsOfEvent = new HashMap <WarmPollutant, Double>();
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
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
							if(eventCosts>0.0)warmEmissionCostsOfEvent.put(pollutant, eventCosts);
						}
					} catch (NullPointerException e) {
						// nothing to do not in research area
					}
			}
			warmEmissionCostTotal.put(vehicleId, warmEmissionCostsOfEvent);
		}
	}

	private Double costOfPollutant(WarmPollutant pollutant) {
		switch(pollutant){
			case NOX : return EURO_PER_GRAMM_NOX;
			case NMHC: return EURO_PER_GRAMM_NMVOC;
			case SO2 : return EURO_PER_GRAMM_SO2;
			case PM: 	return EURO_PER_GRAMM_PM2_5_EXHAUST;
			case CO2_TOTAL: if(considerCO2==true) {
								return EURO_PER_GRAMM_CO2;
							}else{
								return 0.0;
							}
			default: return 0.0;
		}
	}

	public Map<Id<Vehicle>, Map<WarmPollutant, Double>> getWarmEmissionCostsPerPerson() {
		return warmEmissionCostTotal;
	}

	@Override
	public void reset(int iteration) {
		warmEmissionCostTotal.clear();
	}
}
