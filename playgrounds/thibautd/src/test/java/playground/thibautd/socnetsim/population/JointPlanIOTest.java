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
package playground.thibautd.socnetsim.population;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class JointPlanIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDumpAndRead() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final Population population = createPopulation( jointPlans );

		final String file = utils.getOutputDirectory()+"/jointPlansDump.xml";
		JointPlansXmlWriter.write( population , jointPlans , file );

		final JointPlans reReadJointPlans = JointPlansXmlReader.readJointPlans( population , file );

		for ( Person person : population.getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				final JointPlan dumped = jointPlans.getJointPlan( plan );
				final JointPlan read = reReadJointPlans.getJointPlan( plan );

				if ( dumped == null ) {
					Assert.assertNull(
							"dumped is null but read is not",
							read );
					continue;
				}

				Assert.assertNotNull(
						"dumped is not null but read is",
						read );

				Assert.assertEquals(
						"dumped has not the same size as read",
						dumped.getIndividualPlans().size(),
						read.getIndividualPlans().size() );

				Assert.assertTrue(
						"plans in dumped and read do not match",
						dumped.getIndividualPlans().values().containsAll(
								read.getIndividualPlans().values() ) );
			}
		}
	}

	private static Population createPopulation(
			final JointPlans jointPlans ) {
		final int nMembers = 2000;
		final int nPlans = 10;
		final double pJoin = 0.2;
		final Population population =  ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation();
		final PopulationFactory factory = population.getFactory();

		final Map<Id, Queue<Plan>> plansPerPerson = new LinkedHashMap<Id, Queue<Plan>>();

		// create plans
		int idCount = 0;
		for (int j=0; j < nMembers; j++) {
			final Id id = new IdImpl( idCount++ );
			final Person person = factory.createPerson( id );
			population.addPerson( person );
			for (int k=0; k < nPlans; k++) {
				final Plan plan = factory.createPlan();
				plan.setPerson( person );
				person.addPlan( plan );
			}
			plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
		}

		// join plans randomly
		final Random random = new Random( 1234 );
		final int nJointPlans = nMembers;
		for (int p=0; p < nJointPlans; p++) {
			final Map<Id, Plan> jointPlan = new LinkedHashMap<Id, Plan>();
			for (Queue<Plan> plans : plansPerPerson.values()) {
				if ( random.nextDouble() > pJoin ) continue;
				final Plan plan = plans.poll();
				if (plan != null) jointPlan.put( plan.getPerson().getId() , plan );
			}
			if (jointPlan.size() <= 1) continue;
			jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jointPlan ) );
		}

		return population;
	}
}

