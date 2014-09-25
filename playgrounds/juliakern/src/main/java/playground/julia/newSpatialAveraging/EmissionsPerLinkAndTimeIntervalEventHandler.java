/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.newSpatialAveraging;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.util.HashMap;
import java.util.Map;

public class EmissionsPerLinkAndTimeIntervalEventHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler{

	
	private Map<Integer, Map<Id, Double>> intervals2links2emissions;
	private WarmPollutant warmPollutant;
	private ColdPollutant coldPollutant;
	private int noOfTimeBins;
	private double simulationEndTime;

	public EmissionsPerLinkAndTimeIntervalEventHandler(
			double simulationEndTime, int noOfTimeBins, String pollutant2analyze) {
		
		warmPollutant = WarmPollutant.valueOf(pollutant2analyze);
		coldPollutant = ColdPollutant.valueOf(pollutant2analyze);
		this.noOfTimeBins=noOfTimeBins;
		this.simulationEndTime = simulationEndTime;
		intervals2links2emissions = new HashMap<Integer, Map<Id,Double>>();
		for(int i=0;i<noOfTimeBins;i++){
			intervals2links2emissions.put(i, new HashMap<Id, Double>());
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id linkId = event.getLinkId();
		Double emissionValue = event.getWarmEmissions().get(warmPollutant);
		int timeInterval = (int) Math.floor(event.getTime()/simulationEndTime*noOfTimeBins);
		Map<Id, Double> currentInterval = intervals2links2emissions.get(timeInterval);
		if(!currentInterval.containsKey(linkId)){
			currentInterval.put(linkId, emissionValue);
		}else{
			currentInterval.put(linkId, emissionValue+currentInterval.get(linkId));
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id linkId = event.getLinkId();
		Double emissionValue = event.getColdEmissions().get(coldPollutant);
		int timeInterval = (int) Math.floor(event.getTime()/simulationEndTime*noOfTimeBins);
		Map<Id, Double> currentInterval = intervals2links2emissions.get(timeInterval);
		if(!currentInterval.containsKey(linkId)){
			currentInterval.put(linkId, emissionValue);
		}else{
			currentInterval.put(linkId, emissionValue+currentInterval.get(linkId));
		}
		
	}

	public Map<Integer, Map<Id, Double>> getTimeIntervals2EmissionsPerLink() {
		return this.intervals2links2emissions;
	}

}
