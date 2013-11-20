/* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionEvent.java
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

package playground.julia.distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventHandler;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

public class GeneratedEmissionsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	Double simulationStartTime;
	Double timeBinSize;
	Map<Double, ArrayList<EmPerBin>> emissionPerBin;
	Map<Double, ArrayList<EmPerLink>> emissionPerLink;
	Map<Id,Integer> link2xbins; 
	Map<Id,Integer> link2ybins;
	WarmPollutant warmPollutant2analyze;
	ColdPollutant coldPollutant2analyze;
	
	
	public GeneratedEmissionsHandler(Double simulationStartTime, Double timeBinSize, Map<Id, Integer>link2xbins, Map<Id, Integer>link2ybins,
			WarmPollutant warmPollutant2analyze, ColdPollutant coldPollutant2analyze){
		this.simulationStartTime = simulationStartTime;
		this.timeBinSize= timeBinSize;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.warmPollutant2analyze = warmPollutant2analyze;
		this.coldPollutant2analyze = coldPollutant2analyze;
		this.emissionPerBin = new HashMap<Double, ArrayList<EmPerBin>>();
		this.emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
		//TODO person id statt vehicleid??? woher?
		Id personId = event.getVehicleId();
		Double value = event.getColdEmissions().get(coldPollutant2analyze); 
		
		//TODO auf mehrere Zellen verteilen
		ArrayList<EmPerBin> arrayEpb = new ArrayList<EmPerBin>();
		arrayEpb= distributeOnCells(xBin, yBin, personId, value, eventStartTime);
		Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
		if (!emissionPerBin.containsKey(endOfTimeIntervall)) {
			emissionPerBin.put(endOfTimeIntervall,
					new ArrayList<EmPerBin>());
		}
		emissionPerBin.get(endOfTimeIntervall).addAll(arrayEpb);
		
		ArrayList<EmPerLink> arrayEpl = new ArrayList<EmPerLink>();
		arrayEpl = distributeOnLinks(linkId, personId, value, eventStartTime);
		if (!emissionPerLink.containsKey(endOfTimeIntervall)) {
			emissionPerLink.put(endOfTimeIntervall,
					new ArrayList<EmPerLink>());
		}
		emissionPerLink.get(endOfTimeIntervall).addAll(arrayEpl);
		}
	}

	//TODO funktioniert nur so, wenn die simulation start time =0 ist!!!!
	private Double getEndOfTimeInterval(double time) {
		Double end = Math.ceil(time/timeBinSize)*timeBinSize;
		if(end>0.0) return end;
		return timeBinSize;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {		
		Id linkId = event.getLinkId();

		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
			//TODO person id statt vehicleid??? woher?
			Id personId = event.getVehicleId();
			Double value = event.getWarmEmissions().get(warmPollutant2analyze); //TODO funktioniert das so? enum casten?
			//TODO auf mehrere Zellen verteilen
			ArrayList<EmPerBin> arrayEpb = new ArrayList<EmPerBin>();
			arrayEpb= distributeOnCells(xBin, yBin, personId, value, eventStartTime);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerBin.containsKey(endOfTimeIntervall)) {
				emissionPerBin.put(endOfTimeIntervall,
						new ArrayList<EmPerBin>());
			}
			emissionPerBin.get(endOfTimeIntervall).addAll(arrayEpb);
			// TODO auf mehrere Links verteilen
			//EmPerLink epl = new EmPerLink(linkId, personId, value);
			ArrayList<EmPerLink> arrayEpl = new ArrayList<EmPerLink>();
			arrayEpl = distributeOnLinks(linkId, personId, value, eventStartTime);
			if (!emissionPerLink.containsKey(endOfTimeIntervall)) {
				emissionPerLink.put(endOfTimeIntervall,
						new ArrayList<EmPerLink>());
			}
			emissionPerLink.get(endOfTimeIntervall).addAll(arrayEpl);
			//emissionPerLink.get(endOfTimeIntervall).add(epl);
		}
	}

	private ArrayList<EmPerLink> distributeOnLinks(Id sourcelinkId, Id personId,
			Double value, Double eventStartTime) {
		
		ArrayList<EmPerLink> distributedEmissions = new ArrayList<EmPerLink>();
		
		//EmPerLink epl = new EmPerLink(linkId, personId, value);
		
		// for each link: if distance to current link < ??
		// => EmPerLink with value = distance dependent factor * current value
		
		int sourceX = link2xbins.get(sourcelinkId);
		int sourceY = link2ybins.get(sourcelinkId);
		
		for(Id linkId: link2xbins.keySet()){
			if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
			int xDistance = Math.abs(sourceX-link2xbins.get(linkId));
			int yDistance = Math.abs(sourceY-link2ybins.get(linkId));
			int totalDistance = xDistance+yDistance;
			
			if(xDistance<4 && yDistance <4 && (totalDistance<4)){
				Double distributionFactor = 0.0;
				switch(totalDistance){
				case 0: distributionFactor = 0.170;
				case 1: distributionFactor = 0.104;
				case 2: distributionFactor = 0.024;
				case 3: distributionFactor = 0.019;
				}
				if (distributionFactor>0.0) {
					EmPerLink epl = new EmPerLink(linkId, personId, value * distributionFactor, eventStartTime);
					distributedEmissions.add(epl);
				}
			}
		}
		}
		
		return distributedEmissions;
	}

	private ArrayList<EmPerBin> distributeOnCells(Integer xBin, Integer yBin,
			Id personId, Double value, Double eventStartTime) {
		
		ArrayList<EmPerBin> distributedEmissions = new ArrayList<EmPerBin>();
		
		// distribute value onto cells: origin ... dist(origin)=3
		// factors depending on distance (measured by number of cells)
		for(int xIndex = xBin-3; xIndex<=xBin+3; xIndex++){
			for(int yIndex = yBin-3; yIndex <= yBin+3; yIndex++){
				// TODO ausserhalb des untersuchungsraums?
				Double distributionFactor = 0.0;
				int distance = Math.abs(xBin-xIndex+yBin-yIndex);
				switch(distance){
				case 0: distributionFactor = 0.170;
				case 1: distributionFactor = 0.104;
				case 2: distributionFactor = 0.024;
				case 3: distributionFactor = 0.019;
				default: distributionFactor =0.0;
				}
				if (distributionFactor>0.0) {
					EmPerBin epb = new EmPerBin(xIndex, yIndex, personId, value	* distributionFactor, eventStartTime);
					distributedEmissions.add(epb);
				}
			}
			
		}
		return distributedEmissions;
	}

	public Map<Double, ArrayList<EmPerLink>> getEmissionsPerLink() {
		return emissionPerLink;
	}

	public Map<Double, ArrayList<EmPerBin>> getEmissionsPerCell() {
		return emissionPerBin;
	}



}
