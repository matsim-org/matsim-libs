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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

/**
 * Take the plans that the agents have after within-day replanning, and write them to file.
 * <br>
 * Not the same as the {@link ExperiencedPlansService}, since that is based on the events.
 *
 * @author (of documentation) nagel
  */
@Singleton
public class ExecutedPlansServiceImpl implements AfterMobsimListener, ExecutedPlansService {
	// I renamed this from ExperiencedPlansWriter into ExecutedPlansWriter since we also have an ExperiencedPlansService that
	// reconstructs experienced plans from events. kai, jun'16

	private static final Logger log = LogManager.getLogger( ExecutedPlansServiceImpl.class );

	public static final String EXECUTEDPLANSFILE = "executedPlans.xml.gz";

	private Population experiencedPopulation ;

	private Scenario scenario;

	private MobsimDataProvider mobsimDataProvider;

	private OutputDirectoryHierarchy controlerIO;

	@Inject
	ExecutedPlansServiceImpl( Scenario scenario, MobsimDataProvider mobsimDataProvider, OutputDirectoryHierarchy controlerIO ) {
		this.scenario = scenario;
		this.mobsimDataProvider = mobsimDataProvider;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Gbl.assertNotNull(scenario);
		final Config config = scenario.getConfig();
		Scenario experiencedScenario = ScenarioUtils.createScenario(config);
		experiencedPopulation = experiencedScenario.getPopulation();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			MobsimAgent agent = this.mobsimDataProvider.getAgent(person.getId());

			if (agent == null || !(agent instanceof PersonDriverAgentImpl)) experiencedPopulation.addPerson(person);
			else {
				Person experiencedPerson = experiencedPopulation.getFactory().createPerson(person.getId());

				// add experienced plan
				final Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
				experiencedPerson.addPlan(plan);
				experiencedPerson.setSelectedPlan(plan);

				// copy attributes
				PersonUtils.setAge(experiencedPerson, PersonUtils.getAge(person));
				PersonUtils.setCarAvail(experiencedPerson, PersonUtils.getCarAvail(person));
				PersonUtils.setEmployed(experiencedPerson, PersonUtils.isEmployed(person));
				PersonUtils.setLicence(experiencedPerson, PersonUtils.getLicense(person));
				PersonUtils.setSex(experiencedPerson, PersonUtils.getSex(person));

				experiencedPopulation.addPerson(experiencedPerson);
			}
		}

		// write this in every iteration in order to be consistent with previous design.  I think this should be changed.  kai, jun'16
		// as a quick fix, writing this as often as the base plans file.  kai, jun'23
		if ( event.getIteration() % scenario.getConfig().controller().getWritePlansInterval() == 0 ){
			String outputFile = controlerIO.getIterationFilename( event.getIteration(), EXECUTEDPLANSFILE );
			writeExecutedPlans( outputFile );
		}
	}

	@Override
	public void writeExecutedPlans(String outputFile) {
		final Config config = scenario.getConfig();
		final String inputCRS = config.plans().getInputCRS();
		final String internalCRS = config.global().getCoordinateSystem();

		if ( inputCRS == null ) {
			new PopulationWriter(experiencedPopulation, scenario.getNetwork()).write(outputFile);
		}
		else {
			log.info( "re-projecting \"experienced\" population from "+internalCRS+" to "+inputCRS+" for export" );

			final CoordinateTransformation transformation =
					TransformationFactory.getCoordinateTransformation(
							internalCRS,
							inputCRS );

			new PopulationWriter(transformation, experiencedPopulation, scenario.getNetwork()).write(outputFile);
		}
	}

	@Override
	public Map<Id<Person>, Plan> getExecutedPlans() {
		IdMap<Person, Plan> map = new IdMap<>(Person.class) ;
		for ( Person pp : this.experiencedPopulation.getPersons().values() ) {
			map.put( pp.getId(), pp.getSelectedPlan() ) ;
		}
		return map ;
	}
}
