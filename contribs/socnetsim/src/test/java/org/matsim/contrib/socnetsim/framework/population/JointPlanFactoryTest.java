/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanFactoryTest.java
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;

/**
 * @author thibautd
 */
public class JointPlanFactoryTest {
	@Test
	void testAddAtIndividualLevel() throws Exception {
		final Id<Person> id1 = Id.createPersonId( 1 );
		final Person person1 = PopulationUtils.getFactory().createPerson(id1);

		final Id id2 = Id.createPersonId( 2 );
		final Person person2 = PopulationUtils.getFactory().createPerson((Id<Person>) id2);

		final Map<Id<Person>, Plan> jp = new LinkedHashMap< >( );
		jp.put( id1 , PopulationUtils.createPlan(person1) );
		jp.put( id2 , PopulationUtils.createPlan(person2) );

		if ( person1.getPlans().size() != 0 ) {
			throw new RuntimeException( "person should not have plans yet, but has "+person1.getPlans().size() ); 
		}

		new JointPlanFactory().createJointPlan( jp , true );

		assertEquals(
				1,
				person1.getPlans().size(),
				"unexpected number of plans for first person");

		assertEquals(
				1,
				person2.getPlans().size(),
				"unexpected number of plans for second person");
	}

	@Test
	void testDoNotAddAtIndividualLevel() throws Exception {
		final Id id1 = Id.createPersonId( 1 );
		final Person person1 = PopulationUtils.getFactory().createPerson((Id<Person>) id1);

		final Id id2 = Id.createPersonId( 2 );
		final Person person2 = PopulationUtils.getFactory().createPerson((Id<Person>) id2);

		final Map<Id<Person>, Plan> jp = new LinkedHashMap< >( );
		jp.put( id1 , PopulationUtils.createPlan(person1) );
		jp.put( id2 , PopulationUtils.createPlan(person2) );

		if ( person1.getPlans().size() != 0 ) {
			throw new RuntimeException( "person should not have plans yet, but has "+person1.getPlans().size() ); 
		}

		new JointPlanFactory().createJointPlan( jp , false );

		assertEquals(
				0,
				person1.getPlans().size(),
				"unexpected number of plans for first person");

		assertEquals(
				0,
				person2.getPlans().size(),
				"unexpected number of plans for second person");
	}
}

