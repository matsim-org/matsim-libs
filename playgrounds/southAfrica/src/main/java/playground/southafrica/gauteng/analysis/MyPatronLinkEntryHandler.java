/* *********************************************************************** *
 * project: org.matsim.*
 * MyPatronLinkEntryHandler.java
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
import org.matsim.vehicles.Vehicle;

/**
 * An implementation of the {@link LinkEnterEventHandler} to analyse the 
 * patronage of {@link Vehicle}s (in one or more categories) on a given set
 * of {@link Link}s. 
 * 
 * @author jwjoubert
 */
public class MyPatronLinkEntryHandler implements LinkEnterEventHandler{
	private Logger log = Logger.getLogger(MyPatronLinkEntryHandler.class);
	private List<Id<Vehicle>> breaks;
	private List<Id<Vehicle>> linkIds;
	private List<Map<Id<Vehicle>,Integer>> maps;

	public MyPatronLinkEntryHandler(List<Id<Vehicle>> linkIds, List<Id<Vehicle>> breaks) {
		this.linkIds = linkIds;
		this.breaks = breaks;
		maps = new ArrayList<Map<Id<Vehicle>,Integer>>(breaks.size());
		for(int i = 0; i < breaks.size(); i++){
			maps.add(new HashMap<Id<Vehicle>, Integer>());
		}
	}
	
	public List<Map<Id<Vehicle>, Integer>> getMaps(){
		return maps;
	}
	
	
	@Override
	public void reset(int iteration) {
		maps = new ArrayList<Map<Id<Vehicle>,Integer>>(this.breaks.size());
	}

	@Override
	/**
	 * If the link entered is one of the observed links, it is checked to see 
	 * if the agent entering has already entered other observed links. If not,
	 * the agent is added to the relevant map (based on the agent category).
	 * If already in the map, the agent's number of entries is incremented. 
	 */
	public void handleEvent(LinkEnterEvent event) {
		if(linkIds.contains(event.getLinkId())){
			/* The event is of interest. */
			boolean found = false;
			int breakIndex = 0;
			while(!found && breakIndex < breaks.size()){
				if(Long.parseLong(event.getVehicleId().toString()) < Long.parseLong(breaks.get(breakIndex).toString())){
					found = true;
					/* Check if the person entering the link is already contained
					 * in the map. If so, increment its number of tolled links
					 * entered. If not, add it to the map with initial value 1.
					 */
					if(maps.get(breakIndex).containsKey(event.getVehicleId())){
						maps.get(breakIndex).put(event.getVehicleId(), maps.get(breakIndex).get(event.getVehicleId())+1);
					} else{
						maps.get(breakIndex).put(event.getVehicleId(), new Integer(1));
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

