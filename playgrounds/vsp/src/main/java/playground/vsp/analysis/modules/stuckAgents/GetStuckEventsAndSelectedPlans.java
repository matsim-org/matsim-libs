/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.stuckAgents;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.selectedPlans.GetSelectedPlansSubset;

/**
 * @author droeder
 *
 */
public class GetStuckEventsAndSelectedPlans extends AbstractAnalyisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(GetStuckEventsAndSelectedPlans.class);
	private GetStuckEvents stuckEventHandler;
	private Scenario sc;
	private GetSelectedPlansSubset plans;

	public GetStuckEventsAndSelectedPlans(Scenario sc) {
		super(GetStuckEventsAndSelectedPlans.class.getSimpleName());
		this.stuckEventHandler = new GetStuckEvents();
		this.sc = sc;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> list = new ArrayList<EventHandler>();
		list.add(this.stuckEventHandler);
		return list;
	}

	@Override
	public void preProcessData() {
		//do nothing
	}

	@Override
	public void postProcessData() {
		ArrayList<Id> stuckAgents = new ArrayList<Id>();
		for(AgentStuckEvent e: this.stuckEventHandler.getEvents()){
			stuckAgents.add(e.getPersonId());
		}
		this.plans = new GetSelectedPlansSubset(this.sc, stuckAgents);
		this.plans.postProcessData();
	}

	@Override
	public void writeResults(String outputFolder) {
		this.stuckEventHandler.writeResults(outputFolder);
		this.plans.writeResults(outputFolder);
	}
}

