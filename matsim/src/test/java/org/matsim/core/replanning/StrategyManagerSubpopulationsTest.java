/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerSubpopulationsTest.java
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

package org.matsim.core.replanning;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

public class StrategyManagerSubpopulationsTest {
//	private static final String SUBPOP_ATT_NAME = "subpopulation";

	private static final String POP_NAME_1 = "mangeurs_de_boudin";
	private static final String POP_NAME_2 = "buveurs_de_vin";

	@Test
	void testStrategiesAreExecutedOnlyForGivenSubpopulation() {
		final StrategyManager manager = new StrategyManager();

		final Random r = new Random(123);
		final Population population = ScenarioUtils.createScenario(
				ConfigUtils.createConfig()).getPopulation();
		for (int i = 0; i < 1000; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			population.addPerson(p);
			final int group = r.nextInt(3);
			switch (group) {
			case 0:
				break; // "default" population
			case 1:
//				population.getPersonAttributes().putAttribute(
//						p.getId().toString(), SUBPOP_ATT_NAME, POP_NAME_1);
//				PopulationUtils.putPersonAttribute( p, SUBPOP_ATT_NAME, POP_NAME_1 );
				PopulationUtils.putSubpopulation( p, POP_NAME_1 );
				break;
			case 2:
//				population.getPersonAttributes().putAttribute(
//						p.getId().toString(), SUBPOP_ATT_NAME, POP_NAME_2);
//				PopulationUtils.putPersonAttribute( p, SUBPOP_ATT_NAME, POP_NAME_2 );
				PopulationUtils.putSubpopulation( p, POP_NAME_2 );
				break;
			default:
				throw new RuntimeException(group + " ???");
			}
		}

		final Counter counter = new Counter( "test person # " );
//		manager.setSubpopulationAttributeName(SUBPOP_ATT_NAME);
		manager.addStrategy(new PlanStrategy() {
					@Override
					public void run(HasPlansAndId<Plan, Person> person) {
						counter.incCounter();
						Person person1 = population.getPersons().get( person.getId() ) ;
						Gbl.assertNotNull( person1 );
						Assertions.assertNull(
							PopulationUtils.getSubpopulation( person1 ),
//							  PopulationUtils.getPersonAttribute( person1, SUBPOP_ATT_NAME) );
								"unexpected subpopulation" );

					}

					@Override
					public void init(ReplanningContext replanningContext) {}

					@Override
					public void finish() {}
				},
				null,
				1 );
		manager.addStrategy(new PlanStrategy() {
					@Override
					public void run(HasPlansAndId<Plan, Person> person) {
						counter.incCounter();
						Person person1 = population.getPersons().get( person.getId() ) ;
						Gbl.assertNotNull( person1 );
						Assertions.assertEquals(
							POP_NAME_1,
//							  PopulationUtils.getPersonAttribute( person1, SUBPOP_ATT_NAME) );
								PopulationUtils.getSubpopulation( person1 ),
//							  PopulationUtils.getPersonAttribute( person1, SUBPOP_ATT_NAME) );
								"unexpected subpopulation" );
					}

					@Override
					public void init(ReplanningContext replanningContext) {}

					@Override
					public void finish() {}
				},
				POP_NAME_1,
				1 );
		manager.addStrategy(new PlanStrategy() {
					@Override
					public void run(HasPlansAndId<Plan, Person> person) {
						counter.incCounter();
						Person person1 = population.getPersons().get( person.getId() ) ;
						Gbl.assertNotNull( person1 );
						Assertions.assertEquals(
							POP_NAME_2,
//							  PopulationUtils.getPersonAttribute( person1, SUBPOP_ATT_NAME) );
								PopulationUtils.getSubpopulation( person1 ),
//							  PopulationUtils.getPersonAttribute( person1, SUBPOP_ATT_NAME) );
								"unexpected subpopulation" );
					}

					@Override
					public void init(ReplanningContext replanningContext) {}

					@Override
					public void finish() {}
				},
				POP_NAME_2,
				1 );

		manager.run( population , 1 , null );
		counter.printCounter();
	}
}
