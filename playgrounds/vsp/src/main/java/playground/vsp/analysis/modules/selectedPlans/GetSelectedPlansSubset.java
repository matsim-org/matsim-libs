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
package playground.vsp.analysis.modules.selectedPlans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author droeder
 *
 */
public class GetSelectedPlansSubset extends AbstractAnalyisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GetSelectedPlansSubset.class);
	private Collection<Id> ids;
	private Scenario sc;
	private Scenario newSc;

	/**
	 * writes persons and their selected plans from sc to a file. when ids is null
	 * all persons are selected, otherwise only the persons with the specified id's. 
	 * @param sc, the scenario, containing population AND network
	 * @param ids, the person-ids of the persons you want to write. might be null.
	 */
	public GetSelectedPlansSubset(Scenario sc, Collection<Id> ids) {
		super(GetSelectedPlansSubset.class.getSimpleName());
		this.ids = ids;
		this.sc = sc;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new ArrayList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		// do nothing
	}

	@Override
	public void postProcessData() {
		this.newSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// get selected plans
		Person newPerson;
		for(Person p: this.sc.getPopulation().getPersons().values()){
			if((this.ids == null) || this.ids.contains(p.getId())){
				newPerson = this.newSc.getPopulation().getFactory().createPerson(p.getId());
				newPerson.addPlan(p.getSelectedPlan());
				this.newSc.getPopulation().addPerson(newPerson);
			}
		}
	}
	@Override
	public void writeResults(String outputFolder) {
		new PopulationWriter(this.newSc.getPopulation(), this.sc.getNetwork()).write(outputFolder + "selectedPlans.xml");
	}
}

