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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author thibautd
 */
public class JointPlansTest {
	@Test
	public void testExceptionAdd() throws Exception {
		Plan p1 = new PlanImpl( new PersonImpl( new IdImpl( 1 ) ) );
		Plan p2 = new PlanImpl( new PersonImpl( new IdImpl( 2 ) ) );
		Plan p3 = new PlanImpl( new PersonImpl( new IdImpl( 3 ) ) );

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
	public void testExceptionRemove() throws Exception {
		Plan p1 = new PlanImpl( new PersonImpl( new IdImpl( 1 ) ) );
		Plan p2 = new PlanImpl( new PersonImpl( new IdImpl( 2 ) ) );

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

	//@Test
	//public void testIsAdded() throws Exception {
	//	Plan p1 = new PlanImpl( new PersonImpl( new IdImpl( 1 ) ) );
	//	Plan p2 = new PlanImpl( new PersonImpl( new IdImpl( 2 ) ) );
	//	Plan p3 = new PlanImpl( new PersonImpl( new IdImpl( 3 ) ) );
	//	Plan p4 = new PlanImpl( new PersonImpl( new IdImpl( 4 ) ) );
	//	Plan p5 = new PlanImpl( new PersonImpl( new IdImpl( 5 ) ) );

	//	Map<Id, Plan> jpm1 = new HashMap<Id, Plan>();
	//	jpm1.put( p1.getPerson().getId() , p1 );
	//	jpm1.put( p2.getPerson().getId() , p2 );

	//	Map<Id, Plan> jpm2 = new HashMap<Id, Plan>();
	//	jpm2.put( p3.getPerson().getId() , p3 );
	//	jpm2.put( p4.getPerson().getId() , p4 );
	//	jpm2.put( p5.getPerson().getId() , p5 );

	//	JointPlan jp1 = JointPlanFactory.createJointPlan( jpm1 );
	//	JointPlan jp2 = JointPlanFactory.createJointPlan( jpm2 );

	//	for (Plan p : jpm1.values()) {
	//		Assert.assertEquals(
	//				"unexpected joint plan",
	//				JointPlanFactory.getPlanLinks().getJointPlan( p ),
	//				jp1 );
	//	}

	//	for (Plan p : jpm2.values()) {
	//		Assert.assertEquals(
	//				"unexpected joint plan",
	//				JointPlanFactory.getPlanLinks().getJointPlan( p ),
	//				jp2 );
	//	}
	//}

	//@Test
	//@Ignore
	//// this fails... come back to it in case of memory leaks
	//// It may fail only because the assert is done while the GC is running...
	//public void testForgetting() throws Exception {
	//	WeakReference<JointPlan> refToJp;

	//	{
	//		Plan p1 = new PlanImpl( new PersonImpl( new IdImpl( 1 ) ) );
	//		Plan p2 = new PlanImpl( new PersonImpl( new IdImpl( 2 ) ) );

	//		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
	//		jp1.put( p1.getPerson().getId() , p1 );
	//		jp1.put( p2.getPerson().getId() , p2 );

	//		refToJp = new WeakReference<JointPlan>(
	//				JointPlanFactory.createJointPlan( jp1 ) );
	//	}

	//	gc();
	//	Assert.assertEquals(
	//			"JointPlans did not forget",
	//			null,
	//			refToJp.get());
	//}

	//@Test
	//public void testDoNotForgetTooEarly() throws Exception {
	//	WeakReference<JointPlan> refToJp;

	//	Plan p1 = new PlanImpl( new PersonImpl( new IdImpl( 1 ) ) );
	//	Plan p2 = new PlanImpl( new PersonImpl( new IdImpl( 2 ) ) );

	//	Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
	//	jp1.put( p1.getPerson().getId() , p1 );
	//	jp1.put( p2.getPerson().getId() , p2 );

	//	refToJp = new WeakReference<JointPlan>(
	//			JointPlanFactory.createJointPlan( jp1 ) );

	//	gc();
	//	Assert.assertNotNull(
	//			"JointPlans did forget while Plans are still referenced",
	//			refToJp.get());
	//}

	//private static void gc() {
	//	// trick to be sure the gc is run (need this to test the forgetting behavior)
	//	WeakReference ref = new WeakReference<Object>( new Object() );
	//	while (ref.get() != null) System.gc();
	//}
}

