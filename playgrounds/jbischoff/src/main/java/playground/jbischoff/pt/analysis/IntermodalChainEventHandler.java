/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class IntermodalChainEventHandler
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {

	private final Map<Id<Person>,String> lastArrivedMode = new HashMap<>();
	private final List<String> intermodals = new ArrayList<String>();
	private final Network network;
	
	/**
	 * 
	 */
	@Inject
	public IntermodalChainEventHandler(Network network) {
		this.network=network;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		lastArrivedMode.clear();
		intermodals.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityStartEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityStartEvent)
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equals("pt interaction")){
			lastArrivedMode.remove(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!event.getLegMode().contains("walk")) //transit_walk, walk, egress_walk, access_walk...
		{
			lastArrivedMode.put(event.getPersonId(), event.getLegMode());
		}
			
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getLegMode().contains("walk")) //transit_walk, walk, egress_walk, access_walk...
		{
		if (lastArrivedMode.containsKey(event.getPersonId())){
			String lastMode = lastArrivedMode.get(event.getPersonId());
			int time = (int) event.getTime();
			Id<Link> linkId = event.getLinkId();
			String nextMode = event.getLegMode();
			if (!nextMode.equals(lastMode)){
			Coord coord = network.getLinks().get(linkId).getCoord();
			String chain = time+";"+lastMode+"-"+nextMode+";"+linkId.toString()+";"+coord.getX()+";"+coord.getY()+";"+event.getPersonId().toString();
			intermodals.add(chain);
			}
		}
		}
	}
	
	public void writeToFile(String fileName){
		String header = "time;modeConnection;linkId;coordX;coordY;AgentId";
		JbUtils.collection2Text(intermodals, fileName, header);
	}

}
