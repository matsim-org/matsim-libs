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

package playground.julia.responsibilityOffline;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import playground.julia.distribution.EmPerCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * class to handle warm and cold emission events
 * 
 * for each emission event: distribute the emission value onto nearby cells and/or links
 * store information as emission per link/ emission per cell
 * 
 * @author julia, benjamin
 */
public class GeneratedEmissionsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	Double simulationStartTime;
	Double timeBinSize;
	Map<Double, ArrayList<EmPerCell>> emissionPerCell;
	Map<Double, ArrayList<EmPerLink>> emissionPerLink;
	Map<Id<Link>,Integer> link2xbins; 
	Map<Id<Link>,Integer> link2ybins;
	WarmPollutant warmPollutant2analyze;
	ColdPollutant coldPollutant2analyze;
	
	private final static Double dist0factor = 0.216;
	private final static Double dist1factor = 0.132;
	private final static Double dist2factor = 0.029;
	private final static Double dist3factor = 0.002;	
	
	public GeneratedEmissionsHandler(Double simulationStartTime, Double timeBinSize, Map<Id<Link>, Integer>link2xbins, Map<Id<Link>, Integer>link2ybins,
			WarmPollutant warmPollutant2analyze, ColdPollutant coldPollutant2analyze){
		this.simulationStartTime = simulationStartTime;
		this.timeBinSize= timeBinSize;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.warmPollutant2analyze = warmPollutant2analyze;
		this.coldPollutant2analyze = coldPollutant2analyze;
		this.emissionPerCell = new HashMap<Double, ArrayList<EmPerCell>>();
		this.emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
	}
	
	@Override
	public void reset(int iteration) {		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		//event information
		Id<Link> linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
			// TODO person id statt vehicleid??? woher?
			Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
			Double value = event.getColdEmissions().get(coldPollutant2analyze);

			// distribute onto cells
			ArrayList<EmPerCell> arrayEpb = new ArrayList<EmPerCell>();
			arrayEpb = distributeOnCells(xBin, yBin, personId, value, eventStartTime);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerCell.containsKey(endOfTimeIntervall)) {
				emissionPerCell.put(endOfTimeIntervall, new ArrayList<EmPerCell>());
			}
			emissionPerCell.get(endOfTimeIntervall).addAll(arrayEpb);

			// distribute onto links
			ArrayList<EmPerLink> arrayEpl = new ArrayList<EmPerLink>();
			arrayEpl = distributeOnLinks(linkId, personId, value,	eventStartTime);
			if (!emissionPerLink.containsKey(endOfTimeIntervall)) {
				emissionPerLink.put(endOfTimeIntervall,	new ArrayList<EmPerLink>());
			}
			emissionPerLink.get(endOfTimeIntervall).addAll(arrayEpl);
		}
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// event information 
		Id linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
			//TODO person id statt vehicleid??? woher?
			Id personId = event.getVehicleId();
			Double value = event.getWarmEmissions().get(warmPollutant2analyze);
			//distribute onto cells
			ArrayList<EmPerCell> arrayEpb = new ArrayList<EmPerCell>();
			arrayEpb= distributeOnCells(xBin, yBin, personId, value, eventStartTime);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerCell.containsKey(endOfTimeIntervall)) {
				emissionPerCell.put(endOfTimeIntervall,	new ArrayList<EmPerCell>());
			}
			emissionPerCell.get(endOfTimeIntervall).addAll(arrayEpb);
			//distribute onto links
			ArrayList<EmPerLink> arrayEpl = new ArrayList<EmPerLink>();
			arrayEpl = distributeOnLinks(linkId, personId, value, eventStartTime);
			if (!emissionPerLink.containsKey(endOfTimeIntervall)) {
				emissionPerLink.put(endOfTimeIntervall,	new ArrayList<EmPerLink>());
			}
			emissionPerLink.get(endOfTimeIntervall).addAll(arrayEpl);
		}
	}

	private ArrayList<EmPerLink> distributeOnLinks(Id sourcelinkId, Id personId,
			Double value, Double eventStartTime) {
		
		/*
		 * distribute the emission value onto 25 cells:
		 * use the distance from the source cell as a measure for the distribution weights.
		 * origin cell factor: 0.216, distance = 1 -> 0.132, distance = 2 -> 0.029,
		 * distance = 3 -> 0.002 
		 * values are oriented at a normalized Gaussian distribution
		 * and therefore add up to 1.0
		 * 
		 */
		
		ArrayList<EmPerLink> distributedEmissions = new ArrayList<EmPerLink>();
		
		// for each link: if distance to current link < 4
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
					case 0: distributionFactor = dist0factor; break;
					case 1: distributionFactor = dist1factor; break;
					case 2: distributionFactor = dist2factor; break;
					case 3: distributionFactor = dist3factor; break;
				}
				if (distributionFactor>0.0) {
					EmPerLink epl = new EmPerLink(linkId, personId, value * distributionFactor, eventStartTime);
					distributedEmissions.add(epl);
				}
				distributionFactor = 0.0;
			}
		}
		}
		return distributedEmissions;
	}

	private ArrayList<EmPerCell> distributeOnCells(Integer xBin, Integer yBin,
			Id personId, Double value, Double eventStartTime) {
		
		/*
		 * negative emission values are distributed as well
		 *
		 * distribute the emission value onto 25 cells:
		 * use the distance from the source cell as a measure for the distribution weights.
		 * origin cell factor: 0.216, distance = 1 -> 0.132, distance = 2 -> 0.029,
		 * distance = 3 -> 0.002 
		 * values are oriented at a normalized Gaussian distribution
		 * and therefore add up to 1.0
		 * 
		 */
		
		ArrayList<EmPerCell> distributedEmissions = new ArrayList<EmPerCell>();
		
		// distribute value onto cells: origin ... dist(origin)=3
		// factors depending on distance (measured by number of cells)
		for(int xIndex = xBin-3; xIndex<=xBin+3; xIndex++){
			for(int yIndex = yBin-3; yIndex <= yBin+3; yIndex++){
				Double distributionFactor = 0.0;
				int distance = Math.abs(xBin-xIndex)+Math.abs(yBin-yIndex);
				
				switch(distance){
				case 0: distributionFactor = dist0factor; break;
				case 1: distributionFactor = dist1factor; break;
				case 2: distributionFactor = dist2factor; break;
				case 3: distributionFactor = dist3factor; break;
				}
				
				if (distributionFactor>0.0) {
					EmPerCell epb = new EmPerCell(xIndex, yIndex, personId, value	* distributionFactor, eventStartTime);
					distributedEmissions.add(epb);
				}
			}			
		}
		return distributedEmissions;
	}

	private Double getEndOfTimeInterval(double time) {
		Double end = Math.ceil(time/timeBinSize)*timeBinSize;
		if(end>0.0) return end;
		return timeBinSize;
	}
	
	public Map<Double, ArrayList<EmPerLink>> getEmissionsPerLink() {
		return emissionPerLink;
	}

	public Map<Double, ArrayList<EmPerCell>> getEmissionsPerCell() {
		return emissionPerCell;
	}
}
