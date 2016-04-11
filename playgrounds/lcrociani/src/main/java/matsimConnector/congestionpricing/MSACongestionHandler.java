/* *********************************************************************** *
 * project: org.matsim.*
 * MSACongestionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package matsimConnector.congestionpricing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.events.CongestionEvent;

/**
 * Congestion handler similar to
 * L\"ammel, G. (2011). Escaping the Tsunami: Evacuation Strategies for Large Urban Areas. Concepts and Implementation of a Multi-Agent Based Approach. PhD thesis, TU Berlin
 * @author laemmel
 *
 */
public class MSACongestionHandler implements LinkEnterEventHandler,
		LinkLeaveEventHandler {

	
	private final Map<Id<Link>,LinkInfo> lis = new HashMap<>();
	
	private final EventsManager events;
	private final Scenario scenario;

	public MSACongestionHandler(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;
		
	}
	

	@Override
	public void reset(int iteration) {
		this.lis.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo li = this.lis.get(event.getLinkId());
		if (li == null) {
			return;
			// nothing is done for the first link of the route (for which no link enter event exists). tt,Apr'14
		}
		li.agentsOnLink--;
		AgentInfo ai = li.ais.remove(event.getVehicleId());
		ai.leftTime = event.getTime();
		double att = event.getTime() - ai.enterTime;
		if (li.agentsOnLink == 0 || att <= li.freespeedTT) { // queue (no matter if spill back or flow queue) is interrupted. tt, Apr'14
			// calculate link leave time difference to all agents ahead in the queue. tt, Apr'14
			// don't you forget the first agent of the queue i.e. the last one who is traveling without delay but causes delay to the next agents? tt, Apr'14
			for (AgentInfo c : li.congested) {
				double delay = event.getTime() - c.leftTime;
				CongestionEvent e = new CongestionEvent(event.getTime(), "null", Id.createPersonId(c.p), Id.createPersonId("null"), delay, event.getLinkId(), c.enterTime);
				this.events.processEvent(e);
			}
			
			li.congested.clear();
		} else { // att > freespeedTT && there are agents on the link. tt, Arp'14
			li.congested.add(ai);
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = this.lis.get(event.getLinkId());
		if (li == null) {
			li = new LinkInfo();
			Link l = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			double fs = l.getFreespeed();
			double fsTT = l.getLength()/fs;
			li.freespeedTT = fsTT;
			this.lis.put(event.getLinkId(), li);
		}
		li.agentsOnLink++;
		AgentInfo ai = new AgentInfo();
		ai.enterTime = event.getTime();
		ai.p = event.getVehicleId();
		li.ais.put(ai.p, ai);
	}
	
	private static final class AgentInfo {
		public double enterTime;
		public double leftTime;
		Id<Vehicle> p;
	}
	
	private static final class LinkInfo {
		double freespeedTT;
		int agentsOnLink = 0;
		Map<Id<Vehicle>,AgentInfo> ais = new HashMap<>();
		List<AgentInfo> congested = new ArrayList<>();
	}
	
}
