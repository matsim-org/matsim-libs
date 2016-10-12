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
package playground.agarwalamit.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * This class takes list of eventsFile as input and then returns set of all stuck (1) events and (2) persons. 
 * 
 * @author amit
 */

public class StuckAgentsFilter extends AbstractAnalysisModule {
	private static final Logger LOG = Logger.getLogger(StuckAgentsFilter.class);
	private final Set<Id<Person>> stuckPersonsFromEventsFiles = new HashSet<>();
	private final Set<PersonStuckEvent> stuckEventsFromEventsFiles = new HashSet<>();
	private final List<String> eventsFiles;
	private final List<StuckEventsHandler> handlers;

	public StuckAgentsFilter(final List<String> eventsFiles) {
		super(StuckAgentsFilter.class.getSimpleName());
		this.eventsFiles = eventsFiles;
		this.handlers = new ArrayList<>();
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null ;
	}

	@Override
	public void preProcessData() {

		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		
		for (String eventsFile : this.eventsFiles){
			StuckEventsHandler h = new StuckEventsHandler();
			handlers.add(h);
			manager.addHandler(h);
			reader.readFile(eventsFile);
		}
	}

	@Override
	public void postProcessData() {
		for (StuckEventsHandler eh : handlers){
			stuckEventsFromEventsFiles.addAll(eh.getStuckEvents());
			stuckPersonsFromEventsFiles.addAll(eh.getStuckPersons());
		}
		LOG.info(stuckPersonsFromEventsFiles.size()+" persons are stuck in "+eventsFiles.size()+" files.");
	}

	@Override
	public void writeResults(String outputFolder) {
		//nothing to do 
	}
	
	/**
	 * @return Set of stuck persons from all events files.
	 */
	public Set<Id<Person>> getStuckPersonsFromEventsFiles() {
		return stuckPersonsFromEventsFiles;
	}

	/**
	 * @return Set of stuck events from all events files.
	 */
	public Set<PersonStuckEvent> getStuckEventsFromEventsFiles() {
		return stuckEventsFromEventsFiles;
	}

	//==========EventHandler=============
	public class StuckEventsHandler implements PersonStuckEventHandler{

		private final Set<Id<Person>> stuckPersons = new HashSet<>();
		private final Set<PersonStuckEvent> stuckEvents = new HashSet<>();

		@Override
		public void reset(int iteration) {
			this.stuckPersons.clear();
			this.stuckEvents.clear();
		}

		@Override
		public void handleEvent(PersonStuckEvent event) {
			this.stuckEvents.add(event);
			this.stuckPersons.add(event.getPersonId());			
		}

		public Set<Id<Person>> getStuckPersons() {
			return stuckPersons;
		}

		public Set<PersonStuckEvent> getStuckEvents() {
			return stuckEvents;
		}
	}
}
