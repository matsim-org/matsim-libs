/* *********************************************************************** *
 * project: org.matsim.*
 * FixedGroupsTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.grouping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.socnetsim.framework.cliques.config.CliquesConfigGroup;
import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * @author thibautd
 */
public class FixedGroupsIT {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIterationOrderIsDeterministic() throws Exception {
		final String configFile = new File( utils.getPackageInputDirectory() ).getParentFile().getParentFile().getParentFile()+"/config.xml";
		final Config config = JointScenarioUtils.loadConfig( configFile );

		Collection<ReplanningGroup> previous = null;

		// avoid spamming the log file
		for (int i=0; i < 100; i++) {
			final FixedGroupsIdentifier identifier =
				FixedGroupsIdentifierFileParser.readCliquesFile(
						((CliquesConfigGroup) config.getModule(
							CliquesConfigGroup.GROUP_NAME)).getInputFile());
			// recreate each time, in case it changes iteration order
			final Population population = getPopulation( config );
			Collection<ReplanningGroup> groups = identifier.identifyGroups( population );

			if (previous != null) {
				assertIterationOrder(
						previous,
						groups);
			}
			previous = groups;
		}
	}

	private static void assertIterationOrder(
			final Collection<ReplanningGroup> expected,
			final Collection<ReplanningGroup> actual) {
		assertEquals(
				expected.size(),
				actual.size(),
				"not the same number of groups");

		final Iterator<ReplanningGroup> expectedIter = expected.iterator();
		final Iterator<ReplanningGroup> actualIter = actual.iterator();

		int c = 0;
		while (expectedIter.hasNext()) {
			c++;
			final ReplanningGroup expectedGroup = expectedIter.next();
			final ReplanningGroup actualGroup = actualIter.next();

			final Iterator<Person> actualGroupIterator = actualGroup.getPersons().iterator();
			for (Person expectedPerson : expectedGroup.getPersons()) {
				assertEquals(
						expectedPerson.getId(),
						actualGroupIterator.next().getId(),
						"groups "+expectedGroup+" and "+actualGroup+" in position "+
						c+" are not equal or do not present the persons in the same order");
			}
		}
	}

	private static Population getPopulation( final Config config ) {
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		// generate persons not allocated to any clique,
		// making sure that they are not added in the same order as the
		// ordering of their ids.
		final Random random = new Random( 1432 );
		for (int i=0; i < 100; i++) {
			scenario.getPopulation().addPerson(
					PopulationUtils.getFactory().createPerson(Id.create(
					"garbage-" + random.nextInt(999999) + "-" + i, Person.class)));
		}
		return scenario.getPopulation();
	}
}

