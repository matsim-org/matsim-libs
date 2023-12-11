/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansTest.java
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;

/**
 * @author thibautd
 */
public class JointPlansTest {
	@Test
	void testExceptionAddWithCache() throws Exception {
		testExceptionAdd( true );
	}

	@Test
	void testExceptionAddWithoutCache() throws Exception {
		testExceptionAdd( false );
	}

	private static void testExceptionAdd( final boolean withCache ) throws Exception {
		Plan p1 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(1)), withCache );
		Plan p2 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(2)), withCache );
		Plan p3 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(3)), withCache );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		jp1.put( p1.getPerson().getId() , p1 );
		jp1.put( p2.getPerson().getId() , p2 );

		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		jp2.put( p1.getPerson().getId() , p1 );
		jp2.put( p3.getPerson().getId() , p3 );

		JointPlans jointPlans = new JointPlans();
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		boolean gotException = false;
		try {
			jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		}
		catch (JointPlans.PlanLinkException e) {
			gotException = true;
		}

		Assertions.assertTrue(
				gotException,
				"got no exception when associating two joint plans to one individual plan");
	}

	@Test
	void testExceptionRemoveWithCache() throws Exception {
		testExceptionRemove( true );
	}

	@Test
	void testExceptionRemoveWithoutCache() throws Exception {
		testExceptionRemove( false );
	}

	private static void testExceptionRemove( final boolean withCache ) throws Exception {
		Plan p1 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(1)), withCache );
		Plan p2 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(2)), withCache );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		jp1.put( p1.getPerson().getId() , p1 );
		jp1.put( p2.getPerson().getId() , p2 );

		JointPlans jointPlans = new JointPlans();
		jointPlans.addJointPlan(
			jointPlans.getFactory().createJointPlan( jp1 ) );

		// create a new joint plan with the same individual plan:
		// this must result in a exception at removal
		JointPlan wrongInstance =
			new JointPlan(
					jp1 );

		boolean gotException = false;
		try {
			jointPlans.removeJointPlan( wrongInstance );
		}
		catch (JointPlans.PlanLinkException e) {
			gotException = true;
		}

		Assertions.assertTrue(
				gotException,
				"got no exception when associating two joint plans to one individual plan");
	}

	@Test
	void testAddAndGetSeveralInstancesWithCache() {
		testAddAndGetSeveralInstances( true );
	}

	@Test
	void testAddAndGetSeveralInstancesWithoutCache() {
		testAddAndGetSeveralInstances( false );
	}

	private static void testAddAndGetSeveralInstances( final boolean withCache ) {
		final Plan p1 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(1)), withCache );
		final Plan p2 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(2)), withCache );

		final Map<Id<Person>, Plan> jp = new HashMap< >();
		jp.put( p1.getPerson().getId() , p1 );
		jp.put( p2.getPerson().getId() , p2 );

		final JointPlans jointPlans1 = new JointPlans();
		final JointPlan jointPlan1 = jointPlans1.getFactory().createJointPlan( jp );
		jointPlans1.addJointPlan( jointPlan1 );
			
		final JointPlans jointPlans2 = new JointPlans();
		final JointPlan jointPlan2 = jointPlans2.getFactory().createJointPlan( jp );
		jointPlans2.addJointPlan( jointPlan2 );

		final JointPlans jointPlans3 = new JointPlans();
		final JointPlan jointPlan3 = jointPlans3.getFactory().createJointPlan( jp );
		jointPlans3.addJointPlan( jointPlan3 );

		final JointPlans jointPlans4 = new JointPlans();
		final JointPlan jointPlan4 = jointPlans4.getFactory().createJointPlan( jp );
		jointPlans4.addJointPlan( jointPlan4 );

		final JointPlans jointPlans5 = new JointPlans();
		final JointPlan jointPlan5 = jointPlans5.getFactory().createJointPlan( jp );
		jointPlans5.addJointPlan( jointPlan5 );

		Assertions.assertSame(
				jointPlan1,
				jointPlans1.getJointPlan( p1 ),
				"wrong joint plan 1 for person 1" );
		Assertions.assertSame(
				jointPlan1,
				jointPlans1.getJointPlan( p2 ),
				"wrong joint plan 1 for person 2" );

		Assertions.assertSame(
				jointPlan2,
				jointPlans2.getJointPlan( p1 ),
				"wrong joint plan 2 for person 1" );
		Assertions.assertSame(
				jointPlan2,
				jointPlans2.getJointPlan( p2 ),
				"wrong joint plan 2 for person 2" );

		Assertions.assertSame(
				jointPlan3,
				jointPlans3.getJointPlan( p1 ),
				"wrong joint plan 3 for person 1" );
		Assertions.assertSame(
				jointPlan3,
				jointPlans3.getJointPlan( p2 ),
				"wrong joint plan 3 for person 2" );

		Assertions.assertSame(
				jointPlan4,
				jointPlans4.getJointPlan( p1 ),
				"wrong joint plan 4 for person 1" );
		Assertions.assertSame(
				jointPlan4,
				jointPlans4.getJointPlan( p2 ),
				"wrong joint plan 4 for person 2" );

		Assertions.assertSame(
				jointPlan5,
				jointPlans5.getJointPlan( p1 ),
				"wrong joint plan 5 for person 1" );
		Assertions.assertSame(
				jointPlan5,
				jointPlans5.getJointPlan( p2 ),
				"wrong joint plan 5 for person 2" );
	}


	@Test
	void testClearWithoutCache() {
		testClear( false );
	}

	@Test
	void testClearWithCache() {
		testClear( true );
	}

	private static void testClear( final boolean withCache ) {
		Plan p1 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(1)), withCache );
		Plan p2 = createPlan(PopulationUtils.getFactory().createPerson(Id.createPersonId(2)), withCache );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		jp1.put( p1.getPerson().getId() , p1 );
		jp1.put( p2.getPerson().getId() , p2 );

		JointPlans jointPlans = new JointPlans();
		jointPlans.addJointPlan(
			jointPlans.getFactory().createJointPlan( jp1 ) );

		Assertions.assertNotNull(
				jointPlans.getJointPlan( p1 ),
				"joint plan was not added???" );
		jointPlans.clear();

		Assertions.assertNull(
				jointPlans.getJointPlan( p1 ),
				"still a joint plan after clear..." );
	}

	private static Plan createPlan( final Person person , final boolean withCache ) {
		if ( withCache ) return new PlanWithCachedJointPlan( person );
		else return PopulationUtils.createPlan(person);
	}
}

