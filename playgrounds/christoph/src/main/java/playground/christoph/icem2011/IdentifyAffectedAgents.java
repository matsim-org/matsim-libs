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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class IdentifyAffectedAgents implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentDepartureEventHandler {

	private static final Logger log = Logger.getLogger(IdentifyAffectedAgents.class);
	
	private Scenario scenario;
	private Set<Id> affectedLinks;
	private Set<Id> inbetweenOnLinkAgents;
	private Set<Id> beforeBeginOnLinkAgents;
	private double begin;
	private double end;
	private Charset charset = Charset.forName("UTF-8");
	
	public static void main(String[] args) {
		if (args.length != 4) return;
		
		String eventsFile = args[0];
		String affectedLinksFile = args[1];
		double begin = Double.parseDouble(args[2]);
		double end = Double.parseDouble(args[3]);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		IdentifyAffectedAgents iaa = new IdentifyAffectedAgents(scenario, begin, end, affectedLinksFile);
		
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
	public IdentifyAffectedAgents(Scenario scenario, double begin, double end, String affectedLinksFile) {
		this.scenario = scenario;
		this.begin = begin;
		this.end = end;
		
		inbetweenOnLinkAgents = new HashSet<Id>();
		beforeBeginOnLinkAgents = new HashSet<Id>();
		affectedLinks = new HashSet<Id>();
		
		parseAffectedLinks(affectedLinksFile);
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
	
	private void parseAffectedLinks(String affectedLinksFile) {
	    	    
	    try {
	    	Counter lineCounter = new Counter("Parsed replanning link ids ");

	    	FileInputStream fis = null;
	    	InputStreamReader isr = null;
	    	BufferedReader br = null;

	    	log.info("start parsing...");
	    	fis = new FileInputStream(affectedLinksFile);
	    	isr = new InputStreamReader(fis, charset);
	    	br = new BufferedReader(isr);

	    	// skip first Line which is only a header
	    	br.readLine();
	    	
	    	String line;
	    	while((line = br.readLine()) != null) {
	    		
	    		affectedLinks.add(this.scenario.createId(line));
	    		lineCounter.incCounter();
	    	}	    	

	    	br.close();
	    	isr.close();
	    	fis.close();
	    	
	    	log.info("done.");
	    	log.info("Found " + affectedLinks.size() + " replanning links.");
	    } catch (IOException e) {
	    	log.error("Error when trying to parse the replanning links file. No replanning links identified!");
	    }
	}
}