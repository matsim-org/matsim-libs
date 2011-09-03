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

package playground.jjoubert.roadpricing.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.jjoubert.roadpricing.senozon.SanralTollFactor;

public class MyTollPotentialEventHandler implements LinkEnterEventHandler{

	private Logger log = Logger.getLogger(MyPatronLinkEntryHandler.class);
	private List<Id> breaks;
	private List<Id> linkIds;
	private List<Map<Id,Double>> maps;
	private RoadPricingScheme scheme;

	public MyTollPotentialEventHandler(List<Id> linkIds, List<Id> breaks, RoadPricingScheme scheme) {
		this.linkIds = linkIds;
		this.breaks = breaks;
		maps = new ArrayList<Map<Id,Double>>(breaks.size());
		for(int i = 0; i < breaks.size(); i++){
			maps.add(new HashMap<Id, Double>());
		}
		this.scheme = scheme;
	}
	
	public List<Map<Id, Double>> getMaps(){
		return maps;
	}
	
	
	@Override
	public void reset(int iteration) {
		maps = new ArrayList<Map<Id,Double>>(this.breaks.size());
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
				if(Long.parseLong(event.getPersonId().toString()) < Long.parseLong(breaks.get(breakIndex).toString())){
					found = true;
					/* Check if the person entering the link is already contained
					 * in the map. If so, increment its toll tally. If not, add 
					 * it to the map with first toll value.
					 */
					double tollFactor = SanralTollFactor.getTollFactor(event.getPersonId(), event.getLinkId(), event.getTime());
					double toll = tollFactor * this.scheme.getLinkCost(event.getLinkId(), event.getTime()).amount;
					if(maps.get(breakIndex).containsKey(event.getPersonId())){
						maps.get(breakIndex).put(event.getPersonId(), maps.get(breakIndex).get(event.getPersonId()) + toll);
					} else{
						maps.get(breakIndex).put(event.getPersonId(), new Double(toll));
					}
				} else{
					breakIndex++;
				}
			}
			if(!found){
				log.warn("Could not identify a category (bracket) for agent " + event.getPersonId().toString());
			}
		}
	}

}

