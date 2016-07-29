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
package playground.vsp.analysis.modules.plansSubset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author droeder
 *
 */
public class GetPlansSubset extends AbstractAnalysisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GetPlansSubset.class);
	private Collection<Id<Person>> ids;
	private Scenario sc;
	private Scenario newSc;
	private boolean selectedOnly;

	/**
	 * writes persons and their plans from sc to a file. when ids is null
	 * all persons are selected, otherwise only the persons with the specified id's. 
	 * @param sc, the scenario, containing population AND network
	 * @param ids, the person-ids of the persons you want to write. might be null.
	 * @param selectedPlansOnly, set this true if you are interested in the selected plans only
	 */
	public GetPlansSubset(Scenario sc, Collection<Id<Person>> ids, boolean selectedPlansOnly) {
		super(GetPlansSubset.class.getSimpleName());
		this.ids = ids;
		this.sc = sc;
		this.selectedOnly = selectedPlansOnly;
	}

	/**
	  * writes persons and their plans from sc to a file. when ids is null
	 * all persons are selected, otherwise only the persons with the specified id's. 
	 * @param sc, the scenario, containing population AND network
	 * @param ids, the person-ids of the persons you want to write. might be null.
	 */
	public GetPlansSubset(Scenario sc, Collection<Id<Person>> ids) {
		this(sc, ids, false);
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
				if(this.selectedOnly){
					newPerson.addPlan(p.getSelectedPlan());
				}else{
					for(Plan plan : p.getPlans()){
						
						newPerson.addPlan(plan);
					}
				}
				this.newSc.getPopulation().addPerson(newPerson);
			}
		}
	}
	@Override
	public void writeResults(String outputFolder) {
		new PopulationWriter(this.newSc.getPopulation(), this.sc.getNetwork()).write(outputFolder + "plans.xml.gz");
	}
}

