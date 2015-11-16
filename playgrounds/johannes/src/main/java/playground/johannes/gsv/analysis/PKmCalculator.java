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

import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.johannes.gsv.sim.TransitAlightEvent;
import playground.johannes.gsv.sim.TransitAlightEventHandler;
import playground.johannes.gsv.sim.TransitBoardEvent;
import playground.johannes.gsv.sim.TransitBoardEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class PKmCalculator implements TransitAlightEventHandler, TransitBoardEventHandler {
	
	private static final String SYSTEM_ALL = "all";

	private final Network network;
	
	private Map<Id, TransitBoardEvent> boardingEvents;
	
	private TObjectDoubleHashMap<String> distances;
	
	private TObjectIntHashMap<String> persons;
	
	private TransitLineAttributes attributes;
	
	public PKmCalculator(Network network, TransitLineAttributes attributes) {
		this.network = network;
		this.attributes = attributes;
	}
	
	@Override
	public void reset(int iteration) {
		boardingEvents = new HashMap<Id, TransitBoardEvent>();
		distances = new TObjectDoubleHashMap<String>();
		persons = new TObjectIntHashMap<String>();
	}

	@Override
	public void handleEvent(TransitAlightEvent event) {
		TransitBoardEvent boarding = boardingEvents.get(event.getPersonId());
		
		if(boarding == null)
			throw new RuntimeException("No boarding event found. Events are supposed to be in chronological order (at least per person).");
		
		if(boarding.getLine() != event.getLine())
			throw new RuntimeException("Events refer to differen transit lines.");
		
		if(boarding.getRoute() != event.getRoute())
			throw new RuntimeException("Events refer to differen transit routes.");
		
		double d = calculateDistance(event.getRoute(), boarding.getStop(), event.getStop());
		String system = attributes.getTransportSystem(event.getLine().getId().toString());
		distances.adjustOrPutValue(system, d, d);
		persons.adjustOrPutValue(system, 1, 1);
		
		distances.adjustOrPutValue(SYSTEM_ALL, d, d);
		persons.adjustOrPutValue(SYSTEM_ALL, 1, 1);
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.TransitBoardEventHandler#handleEvent(playground.johannes.gsv.sim.TransitBoardEvent)
	 */
	@Override
	public void handleEvent(TransitBoardEvent event) {
		boardingEvents.put(event.getPersonId(), event);
	}

	public Map<String, Double> statistics() {
		Map<String, Double> stats = new HashMap<String, Double>();
		
		Object[] keys = distances.keys();
		for(Object key : keys) {
			double val = distances.get((String) key);// * persons.get((String) key);
			stats.put((String) key, val);
		}
		
		return stats;
	}
	
	private double calculateDistance(TransitRoute troute, TransitStopFacility access, TransitStopFacility egress) {
		NetworkRoute netRoute = troute.getRoute();
		Link accessLink = network.getLinks().get(access.getLinkId());
		Node accessNode = accessLink.getToNode();
		
		Link egressLink = network.getLinks().get(egress.getLinkId());
		Node egressNode = egressLink.getFromNode();
		
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
		boolean found = false;
		for(int i = startIdx; i < ids.size(); i++) {
			Link link = network.getLinks().get(ids.get(i));
			length += link.getLength();
			
			if(link.getToNode().equals(egressNode)) {
				found = true;
				break;
			}
		}
		
		if(!found) {
			throw new RuntimeException("Ergress node not found in transit route.");
		}
		
		return length;
	}
}
