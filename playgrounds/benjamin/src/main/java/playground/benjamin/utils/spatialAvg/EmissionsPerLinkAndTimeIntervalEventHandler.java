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

package playground.benjamin.utils.spatialAvg;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.util.HashMap;
import java.util.Map;

public class EmissionsPerLinkAndTimeIntervalEventHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler{
	
	private Map<Integer, Map<Id<Link>, EmissionsAndVehicleKm>> intervals2links2emissions;
	private WarmPollutant warmPollutant;
	private ColdPollutant coldPollutant;
	private int noOfTimeBins;
	private double simulationEndTime;
	private Map<Id<Link>, ? extends Link> links;

	public EmissionsPerLinkAndTimeIntervalEventHandler(
			Map<Id<Link>, ? extends Link> links, double simulationEndTime, int noOfTimeBins, String pollutant2analyze) {
		
		this.links = links;
		warmPollutant = WarmPollutant.valueOf(pollutant2analyze);
		coldPollutant = ColdPollutant.valueOf(pollutant2analyze);
		this.noOfTimeBins=noOfTimeBins;
		this.simulationEndTime = simulationEndTime;
		intervals2links2emissions = new HashMap<Integer, Map<Id<Link>,EmissionsAndVehicleKm>>();
		for(int i=0;i<noOfTimeBins;i++){
			intervals2links2emissions.put(i, new HashMap<Id<Link>, EmissionsAndVehicleKm>());
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Link> linkId = event.getLinkId();
		Double emissionValue = event.getWarmEmissions().get(warmPollutant);
		Double linkLenghtKm = links.get(linkId).getLength() / 1000.;
		int timeInterval = (int) Math.floor(event.getTime() / simulationEndTime*noOfTimeBins);
		Map<Id<Link>, EmissionsAndVehicleKm> currentInterval = intervals2links2emissions.get(timeInterval);
		
		EmissionsAndVehicleKm eavk = currentInterval.get(linkId);
		if (eavk == null) {
			currentInterval.put(linkId, new EmissionsAndVehicleKm(emissionValue, linkLenghtKm));
		} else {
			eavk.add(emissionValue, linkLenghtKm);
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Link> linkId = event.getLinkId();
		Double emissionValue = event.getColdEmissions().get(coldPollutant);
		int timeInterval = (int) Math.floor(event.getTime() / simulationEndTime * noOfTimeBins);
		Map<Id<Link>, EmissionsAndVehicleKm> currentInterval = intervals2links2emissions.get(timeInterval);
		
		EmissionsAndVehicleKm eavk = currentInterval.get(linkId);
		if (eavk == null) {
			currentInterval.put(linkId, new EmissionsAndVehicleKm(emissionValue, 0.0));
		} else {
			eavk.add(emissionValue, 0.0);
		}
	}

	public Map<Integer, Map<Id<Link>, EmissionsAndVehicleKm>> getTimeIntervals2EmissionsPerLink() {
		return this.intervals2links2emissions;
	}
}
