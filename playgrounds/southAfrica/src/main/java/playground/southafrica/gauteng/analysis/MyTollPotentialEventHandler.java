/* *********************************************************************** *
 * project: org.matsim.*
 * MyTollPotentialEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.roadpricing.RoadPricingSchemeUsingTollFactor;
import org.matsim.vehicles.Vehicle;


public class MyTollPotentialEventHandler implements LinkEnterEventHandler{

	private Logger log = Logger.getLogger(MyPatronLinkEntryHandler.class);
	private List<Id<Link>> breaks;
	private List<Id<Link>> linkIds;
	private List<Map<Id<Vehicle>,Double>> valueMaps;
	private List<Map<Id<Vehicle>,Integer>> countMaps;
	private RoadPricingSchemeUsingTollFactor scheme;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public MyTollPotentialEventHandler(List<Id<Link>> linkIds, List<Id<Link>> breaks, RoadPricingSchemeUsingTollFactor scheme) {
		this.linkIds = linkIds;
		this.breaks = breaks;
		valueMaps = new ArrayList<>(breaks.size());
		countMaps = new ArrayList<>(breaks.size());
		for(int i = 0; i < breaks.size(); i++){
			valueMaps.add(new HashMap<Id<Vehicle>, Double>());
			countMaps.add(new HashMap<Id<Vehicle>, Integer>());
		}
		this.scheme = scheme;
	}
	
	
	public List<Map<Id<Vehicle>, Double>> getValueMaps(){
		return valueMaps;
	}
	
	
	public List<Map<Id<Vehicle>, Integer>> getCountMaps(){
		return countMaps;
	}
	
	
	@Override
	public void reset(int iteration) {
		valueMaps = new ArrayList<>(this.breaks.size());
		countMaps = new ArrayList<>(this.breaks.size());
	}

	@Override
	/**
	 * 
	 */
	public void handleEvent(LinkEnterEvent event) {
		if(linkIds.contains(event.getLinkId())){
			/* The event is of interest. */
			
			/* Get the link's toll amount. */
			boolean found = false;
			int breakIndex = 0;
			while(!found && breakIndex < breaks.size()){
				if(Long.parseLong(event.getVehicleId().toString()) < Long.parseLong(breaks.get(breakIndex).toString())){
					found = true;
					/* Check if the vehicle entering the link is already contained
					 * in the map. If so, increment its toll tally. If not, add 
					 * it to the map with first toll value.
					 */
					Cost cost = this.scheme.getLinkCostInfo(event.getLinkId(), event.getTime(), delegate.getDriverOfVehicle(event.getVehicleId()), event.getVehicleId() );
					double toll = (cost == null) ? 0 : cost.amount;
					if(valueMaps.get(breakIndex).containsKey(event.getVehicleId())){
						valueMaps.get(breakIndex).put(event.getVehicleId(), valueMaps.get(breakIndex).get(event.getVehicleId()) + toll);
						countMaps.get(breakIndex).put(event.getVehicleId(), countMaps.get(breakIndex).get(event.getVehicleId()) + 1);
					} else{
						valueMaps.get(breakIndex).put(event.getVehicleId(), new Double(toll));
						countMaps.get(breakIndex).put(event.getVehicleId(), new Integer(1));
					}
				} else{
					breakIndex++;
				}
			}
			if(!found){
				log.warn("Could not identify a category (bracket) for agent " + event.getVehicleId().toString());
			}
		}
	}

}

