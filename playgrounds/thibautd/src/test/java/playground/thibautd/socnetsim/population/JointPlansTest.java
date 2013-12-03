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
package playground.thibautd.socnetsim.population;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author thibautd
 */
public class JointPlansTest {
	@Test
	public void testExceptionAddWithCache( ) throws Exception {
		testExceptionAdd( true );
	}

	@Test
	public void testExceptionAddWithoutCache( ) throws Exception {
		testExceptionAdd( false );
	}

	private static void testExceptionAdd( final boolean withCache ) throws Exception {
		Plan p1 = createPlan( new PersonImpl( new IdImpl( 1 ) ) , withCache );
		Plan p2 = createPlan( new PersonImpl( new IdImpl( 2 ) ) , withCache );
		Plan p3 = createPlan( new PersonImpl( new IdImpl( 3 ) ) , withCache );

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		jp1.put( p1.getPerson().getId() , p1 );
		jp1.put( p2.getPerson().getId() , p2 );

		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
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

		Assert.assertTrue(
				"got no exception when associating two joint plans to one individual plan",
				gotException);
	}


	@Test
	public void testExceptionRemoveWithCache( ) throws Exception {
		testExceptionRemove( true );
	}

	@Test
	public void testExceptionRemoveWithoutCache( ) throws Exception {
		testExceptionRemove( false );
	}

	private static void testExceptionRemove( final boolean withCache ) throws Exception {
		Plan p1 = createPlan( new PersonImpl( new IdImpl( 1 ) ) , withCache );
		Plan p2 = createPlan( new PersonImpl( new IdImpl( 2 ) ) , withCache );

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
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

		Assert.assertTrue(
				"got no exception when associating two joint plans to one individual plan",
				gotException);
	}

	private static Plan createPlan( final Person person , final boolean withCache ) {
		if ( withCache ) return new PlanWithCachedJointPlan( person );
		else return new PlanImpl( person );
	}
}

