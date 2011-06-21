/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Testcase for the JointPlan class
 * @author thibautd
 */
public class JointPlanTest {
	private Map<Id, Plan> individualUnsynchronizedPlans;
	private Clique clique;

	private final Id pers1 = new IdImpl(1);
	private final Id pers2 = new IdImpl(2);

	private final double time11 = 30;
	private final double time12 = 70;
	private final double time13 = 90;
	private final double time14 = 130;

	private final double time21 = 1030;
	private final double time22 = 1070;
	private final double time23 = 1090;
	private final double time24 = 3090;

	// /////////////////////////////////////////////////////////////////////////
	// test methods
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void init() {
		individualUnsynchronizedPlans = new HashMap<Id, Plan>();
		Map<Id, Person> persons = getTestPersons(individualUnsynchronizedPlans);
		this.clique = new Clique(new IdImpl(1), persons);
	}

	/**
	 * Tests whether the synchronisation of plans works.
	 */
	@Test
	public void testSynchonization() {
		JointPlan testPlan =
			new JointPlan(
					clique,
					individualUnsynchronizedPlans,
					false,
					true);

		Activity pickUp1 = (Activity)
			testPlan.getIndividualPlan(pers1).getPlanElements().get(2);
		Activity pickUp2 = (Activity)
			testPlan.getIndividualPlan(pers2).getPlanElements().get(2);

		// test synchro
		Assert.assertEquals(
				"plan synchronisation failed!",
				pickUp1.getEndTime(),
				pickUp2.getEndTime(),
				MatsimTestUtils.EPSILON);

		// test consistency
		List<PlanElement> pes = testPlan.getIndividualPlan(pers1).getPlanElements();
		double oldNow = Double.NEGATIVE_INFINITY;
		double now;
		for (int i=0; i < pes.size(); i+=2) {
			now = ((Activity) pes.get(i)).getEndTime();
			Assert.assertTrue(
					"inconsistent durations after synchronisation",
					now > oldNow);
			oldNow = now;
		}
	}

	/**
	 * Tests whether the individual plans are not synchronised when it is not
	 * asked.
	 */
	@Test
	public void testNoSynchonization() {
		JointPlan testPlan =
			new JointPlan(
					clique,
					individualUnsynchronizedPlans,
					false,
					false);

		List<PlanElement> pe1 = testPlan.getIndividualPlanElements().get(pers1);
		List<PlanElement> pe2 = testPlan.getIndividualPlanElements().get(pers2);

		assertTimesEquals((Activity) pe1.get(0), time11);
		assertTimesEquals((Activity) pe1.get(2), time12);
		assertTimesEquals((Activity) pe1.get(4), time13);
		assertTimesEquals((Activity) pe1.get(6), time14);
				
		assertTimesEquals((Activity) pe2.get(0), time21);
		assertTimesEquals((Activity) pe2.get(2), time22);
		assertTimesEquals((Activity) pe2.get(4), time23);
		assertTimesEquals((Activity) pe2.get(6), time24);
	}

	@Test
	public void testAddAtIndividualLevel() {
		// test if well added
		JointPlan testPlan =
			new JointPlan(
					clique,
					individualUnsynchronizedPlans,
					true,
					false);
		Plan plan1 = testPlan.getIndividualPlan(pers1);
		Assert.assertTrue(
				"plan not added at individual level when asked",
				clique.getMembers().get(pers1).getPlans().contains(plan1));

		// test if not added
		testPlan =
			new JointPlan(
					clique,
					individualUnsynchronizedPlans,
					false,
					false);
		plan1 = testPlan.getIndividualPlan(pers1);
		Assert.assertFalse(
				"plan added at individual level when not asked",
				clique.getMembers().get(pers1).getPlans().contains(plan1));
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private Map<Id, Person> getTestPersons(final Map<Id, Plan> plans) {
		Map<Id, Person> out = new HashMap<Id, Person>();

		Id link1 = new IdImpl(1);
		Id link2 = new IdImpl(2);
		Id link3 = new IdImpl(3);
		Id link4 = new IdImpl(4);

		String puName = JointActingTypes.PICK_UP_BEGIN +
			JointActingTypes.PICK_UP_SPLIT_EXPR + 1;

		// pers 1
		Id id = pers1;
		PersonImpl pers = new PersonImpl(id);
		PlanImpl plan = new PlanImpl(pers);
		pers.addPlan(plan);

		Activity act = new ActivityImpl("home", link1);
		act.setEndTime(time11);
		plan.addActivity(act);

		plan.createAndAddLeg(TransportMode.walk);

		act = new ActivityImpl(puName, link2);
		act.setEndTime(time12);
		plan.addActivity(act);

		plan.createAndAddLeg(JointActingTypes.PASSENGER);

		act = new ActivityImpl(JointActingTypes.DROP_OFF, link3);
		act.setEndTime(time13);
		plan.addActivity(act);

		plan.createAndAddLeg(TransportMode.walk);

		act = new ActivityImpl("lunch", link4);
		act.setEndTime(time14);
		plan.addActivity(act);

		out.put(id, pers);
		plans.put(id, plan);

		// pers 2
		id = pers2;
		pers = new PersonImpl(id);

		plan = new PlanImpl(pers);
		pers.addPlan(plan);

		act = new ActivityImpl("home", link1);
		act.setEndTime(time21);
		plan.addActivity(act);

		plan.createAndAddLeg(TransportMode.car);

		act = new ActivityImpl(puName, link2);
		act.setEndTime(time22);
		plan.addActivity(act);

		plan.createAndAddLeg(TransportMode.car);

		act = new ActivityImpl(JointActingTypes.DROP_OFF, link3);
		act.setEndTime(time23);
		plan.addActivity(act);

		plan.createAndAddLeg(TransportMode.car);

		act = new ActivityImpl("lunch", link4);
		act.setEndTime(time24);
		plan.addActivity(act);

		out.put(id, pers);
		plans.put(id, plan);

		return out;
	}

	private void assertTimesEquals(final Activity act,final double time) {
		String msg = "plan durations where change without synchronisation";
		Assert.assertEquals(
				msg, time, act.getEndTime(), MatsimTestUtils.EPSILON);
	}
}

