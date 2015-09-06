/* *********************************************************************** *
 * project: org.matsim.*
 * ExperiencedPlansWriter.java
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

package org.matsim.withinday.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;

public class ExperiencedPlansWriter implements AfterMobsimListener {

	public static String EXPERIENCEDPLANSFILE = "experiencedPlans.xml.gz";
	
	private final MobsimDataProvider mobsimDataProvider;
	
	public ExperiencedPlansWriter(MobsimDataProvider mobsimDataProvider) {
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		Controler controler = event.getControler();
		Scenario scenario = controler.getScenario();
		
		
		Scenario experiencedScenario = ScenarioUtils.createScenario(controler.getConfig());
		Population experiencedPopulation = experiencedScenario.getPopulation(); 
				
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			MobsimAgent agent = this.mobsimDataProvider.getAgent(person.getId());
			
			if (agent == null || !(agent instanceof PersonDriverAgentImpl)) experiencedPopulation.addPerson(person);
			else {
				Person experiencedPerson = experiencedPopulation.getFactory().createPerson(person.getId());
				
				// add experienced plan
				experiencedPerson.addPlan(WithinDayAgentUtils.getModifiablePlan(agent));
				
				// copy attributes if possible
				if (person instanceof PersonImpl && experiencedPerson instanceof PersonImpl) {
					PersonImpl.setAge(experiencedPerson, PersonImpl.getAge(person));
					PersonImpl.setCarAvail(experiencedPerson, PersonImpl.getCarAvail(person));
					PersonImpl.setEmployed(experiencedPerson, PersonImpl.isEmployed(person));
					PersonImpl.setLicence(experiencedPerson, PersonImpl.getLicense(person));
					PersonImpl.setSex(experiencedPerson, PersonImpl.getSex(person));
				}
				
				experiencedPopulation.addPerson(experiencedPerson);
			}
		}

		String outputFile = controler.getControlerIO().getIterationFilename(event.getIteration(), EXPERIENCEDPLANSFILE);
		new PopulationWriter(experiencedPopulation, scenario.getNetwork()).write(outputFile);
	}
}