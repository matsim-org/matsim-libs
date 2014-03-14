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

/**
 * 
 */
package playground.johannes.gsv.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.johannes.gsv.sim.TransitAlightEvent;
import playground.johannes.gsv.sim.TransitAlightEventHandler;
import playground.johannes.gsv.sim.TransitBoardEvent;
import playground.johannes.gsv.sim.TransitBoardEventHandler;

/**
 * @author johannes
 *
 */
public class PKmCalculator implements TransitAlightEventHandler, TransitBoardEventHandler, IterationEndsListener {

	private final Network network;
	
	private Map<Id, TransitBoardEvent> boardingEvents;
	
	private double length;
	
	private int persons;
	
	public PKmCalculator(Network network) {
		this.network = network;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		System.out.println("################# PKM = " + (length * persons / 1000) + " ####################");
		boardingEvents = new HashMap<Id, TransitBoardEvent>();
		length = 0;
		persons = 0;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.TransitAlightEventHandler#handleEvent(playground.johannes.gsv.sim.TransitAlightEvent)
	 */
	@Override
	public void handleEvent(TransitAlightEvent event) {
		TransitBoardEvent boarding = boardingEvents.get(event.getPersonId());
		
		if(boarding == null)
			throw new RuntimeException("No boarding event found. Events are supposed to be in chronological order (at least per person).");
		
		if(boarding.getLine() != event.getLine())
			throw new RuntimeException("Events refer to differen transit lines.");
		
		if(boarding.getRoute() != event.getRoute())
			throw new RuntimeException("Events refer to differen transit routes.");
		
		length += calculateDistance(event.getRoute(), boarding.getStop(), event.getStop());
		persons++;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.TransitBoardEventHandler#handleEvent(playground.johannes.gsv.sim.TransitBoardEvent)
	 */
	@Override
	public void handleEvent(TransitBoardEvent event) {
		boardingEvents.put(event.getPersonId(), event);
	}

	private double calculateDistance(TransitRoute troute, TransitStopFacility access, TransitStopFacility egress) {
		NetworkRoute netRoute = troute.getRoute();
		Link accessLink = network.getLinks().get(access.getLinkId());
		Node accessNode = accessLink.getToNode();
		
		int startIdx = -1;
		List<Id> ids = new ArrayList<Id>(netRoute.getLinkIds().size() + 2);
		ids.add(netRoute.getStartLinkId());
		ids.addAll(netRoute.getLinkIds());
		ids.add(netRoute.getEndLinkId());
		
		for(int i = 0; i < ids.size(); i++) {
			Link link = network.getLinks().get(ids.get(i));
			if(link.getFromNode().equals(accessNode)) {
				startIdx = i;
				break;
			}
		}
		
		if(startIdx < 0)
			throw new RuntimeException("Access stop not found in transit route.");
		
		double length = 0;
		for(int i = startIdx; i < ids.size(); i++) {
			Link link = network.getLinks().get(ids.get(i));
			length += link.getLength();
			
			if(link.getToNode().equals(accessNode)) {
				break;
			}
		}
		
		return length;
	}
	
	public double getPKm() {
		return length * persons;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("################# PKM = " + (length * persons / 1000) + " ####################");
		
	}
}
