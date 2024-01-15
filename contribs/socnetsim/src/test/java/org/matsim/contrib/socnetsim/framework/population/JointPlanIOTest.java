/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class JointPlanIOTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDumpAndRead() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final Scenario scenario = createScenario( jointPlans );
		final Population population = scenario.getPopulation();

		final String file = utils.getOutputDirectory()+"/jointPlansDump.xml";
		JointPlansXmlWriter.write(population, jointPlans, file);

		final Scenario rereadScenario = ScenarioUtils.createScenario( scenario.getConfig() );
		((MutableScenario) rereadScenario).setPopulation( scenario.getPopulation() );
		new JointPlansXmlReader( rereadScenario ).readFile( file );
		final JointPlans reReadJointPlans = (JointPlans)
				rereadScenario.getScenarioElement( JointPlans.ELEMENT_NAME );

		for ( Person person : population.getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				final JointPlan dumped = jointPlans.getJointPlan( plan );
				final JointPlan read = reReadJointPlans.getJointPlan( plan );

				if ( dumped == null ) {
					Assertions.assertNull(
							read,
							"dumped is null but read is not" );
					continue;
				}

				Assertions.assertNotNull(
						read,
						"dumped is not null but read is" );

				Assertions.assertEquals(
						dumped.getIndividualPlans().size(),
						read.getIndividualPlans().size(),
						"dumped has not the same size as read" );

				Assertions.assertTrue(
						dumped.getIndividualPlans().values().containsAll(
								read.getIndividualPlans().values() ),
						"plans in dumped and read do not match" );
			}
		}
	}

	@Test
	void testPlansOrderIsStableInCoreIO() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final Scenario scenario = createScenario( jointPlans );

		final String file = utils.getOutputDirectory()+"/plans.xml";
		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( file );

		final Scenario scenarioReRead = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( scenarioReRead ).readFile( file );

		final Iterator<? extends Person> dumpedPersons = scenario.getPopulation().getPersons().values().iterator();
		final Iterator<? extends Person> readPersons = scenarioReRead.getPopulation().getPersons().values().iterator();

		while ( dumpedPersons.hasNext() ) {
			final Person dumpedPerson = dumpedPersons.next();
			final Person readPerson = readPersons.next();

			final Iterator<? extends Plan> dumpedPlans = dumpedPerson.getPlans().iterator();
			final Iterator<? extends Plan> readPlans = readPerson.getPlans().iterator();
			while( dumpedPlans.hasNext() ) {
				// score are set different for every plan in the sequence
				Assertions.assertEquals(
						dumpedPlans.next().getScore(),
						readPlans.next().getScore(),
						MatsimTestUtils.EPSILON,
						"order of plans have changed through IO" );
			}
		}
	}

	private static Scenario createScenario(
			final JointPlans jointPlans ) {
		final int nMembers = 2000;
		final int nPlans = 10;
		final double pJoin = 0.2;
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Population population =  scenario.getPopulation();
		final PopulationFactory factory = population.getFactory();

		final Map<Id<Person>, Queue<Plan>> plansPerPerson = new LinkedHashMap<Id<Person>, Queue<Plan>>();

		// create plans
		int idCount = 0;
		for (int j=0; j < nMembers; j++) {
			final Id<Person> id = Id.createPersonId( idCount++ );
			final Person person = factory.createPerson( id );
			population.addPerson( person );
			for (int k=0; k < nPlans; k++) {
				final Plan plan = factory.createPlan();
				plan.setPerson( person );
				person.addPlan( plan );
				plan.setScore( (double) k );
			}
			plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
		}

		// join plans randomly
		final Random random = new Random( 1234 );
		final int nJointPlans = nMembers;
		for (int p=0; p < nJointPlans; p++) {
			final Map<Id<Person>, Plan> jointPlan = new LinkedHashMap< >();
			for (Queue<Plan> plans : plansPerPerson.values()) {
				if ( random.nextDouble() > pJoin ) continue;
				final Plan plan = plans.poll();
				if (plan != null) jointPlan.put( plan.getPerson().getId() , plan );
			}
			if (jointPlan.size() <= 1) continue;
			jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jointPlan ) );
		}

		return scenario;
	}
}

