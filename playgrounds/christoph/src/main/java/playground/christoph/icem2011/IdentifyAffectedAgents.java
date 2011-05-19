/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifyAffectedAgents.java
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

package playground.christoph.icem2011;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class IdentifyAffectedAgents implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentDepartureEventHandler {

	private Set<Id> affectedLinks;
	private Set<Id> inbetweenOnLinkAgents;
	private Set<Id> beforeBeginOnLinkAgents;
	private double begin;
	private double end;
	
	public static void main(String[] args) {
		if (args.length != 5) return;
		
		String eventsFile = args[0];
		String networkFile = args[1];
		String changeEventsFile = args[2];
		double begin = Double.parseDouble(args[3]);
		double end = Double.parseDouble(args[4]);
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.network().setChangeEventInputFile(changeEventsFile);
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		
		IdentifyAffectedAgents iaa = new IdentifyAffectedAgents(network, begin, end);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(iaa);
		
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		
		System.out.println("Found " + iaa.getAffectedAgents().size() + " affected agents.");
	}
	
	/*
	 * HowTo:
	 * Before the beginning track all agents that are actively on one of
	 * the links. After beginning, all agents that enter one of the links
	 * are automatically affected. After ending, nothing is done anymore.
	 */
	public IdentifyAffectedAgents(Network network, double begin, double end) {
		this.begin = begin;
		this.end = end;
		
		inbetweenOnLinkAgents = new HashSet<Id>();
		beforeBeginOnLinkAgents = new HashSet<Id>();
		
		affectedLinks = new HashSet<Id>();
		for (NetworkChangeEvent networkChangeEvent : ((NetworkImpl)network).getNetworkChangeEvents()) {
			for (Link link : networkChangeEvent.getLinks()) {
				affectedLinks.add(link.getId());				
			}
		}
	}
	
	public Set<Id> getAffectedAgents() {
		Set<Id> agents = new HashSet<Id>();
		agents.addAll(beforeBeginOnLinkAgents);
		agents.addAll(inbetweenOnLinkAgents);
		return agents;
	}
	
	@Override
	public void reset(int iteration) {
		inbetweenOnLinkAgents.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(!affectedLinks.contains(event.getLinkId())) return;
		
		if (event.getTime() < begin) {
			beforeBeginOnLinkAgents.add(event.getPersonId());
		} else if (event.getTime() > end) return;
		else {
			inbetweenOnLinkAgents.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(!affectedLinks.contains(event.getLinkId())) return;
		
		if (event.getTime() < begin) {
			beforeBeginOnLinkAgents.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(!affectedLinks.contains(event.getLinkId())) return;
		
		if (event.getTime() < begin) {
			beforeBeginOnLinkAgents.remove(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(!affectedLinks.contains(event.getLinkId())) return;
		
		if (event.getTime() < begin) {
			beforeBeginOnLinkAgents.add(event.getPersonId());
		} else if (event.getTime() > end) return;
		else {
			inbetweenOnLinkAgents.add(event.getPersonId());
		}
	}
}