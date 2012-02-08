/* *********************************************************************** *
 * project: org.matsim.*
 * BackwardCompatibilityTest.java
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
package playground.thibautd.router;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * Tests whether the wrapping PlansCalcRoute gives the same results as
 * the base one.
 *
 * @author thibautd
 */
public class BackwardCompatibilityTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private MultiLegRoutingControler controler;
	// the "old" plansCalcRoute
	private PlansCalcRoute plansCalcRoute;
	// the new object
	private TripRouter tripRouter;
	// the "compatibility" PlansCalcRoute-subClass
	private PlanRouterWrapper wrapper;

	@Before
	public void init() {
		Config config = utils.loadConfig( utils.getClassInputDirectory() + "config.xml" );
		config.controler().setLastIteration( 0 );

		controler = new MultiLegRoutingControler( config );
		controler.run();

		Controler oldControler = new Controler( config );
		oldControler.setOverwriteFiles( true );
		oldControler.run();

		plansCalcRoute = (PlansCalcRoute) oldControler.createRoutingAlgorithm();
		tripRouter = controler.getTripRouterFactory().createTripRouter();
		wrapper= (PlanRouterWrapper) controler.createRoutingAlgorithm();
	}

	@Test
	public void testUnwrappedTravelTime() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = plan.getPlanElements().iterator();

				Activity origin = (Activity) iterator.next();

				double now = 0;
				while (iterator.hasNext()) {
					Leg leg = (Leg) iterator.next();
					Activity destination = (Activity) iterator.next();

					now = updateNow( now , origin );

					double timePcr = plansCalcRoute.handleLeg(
							person,
							leg,
							origin,
							destination,
							now);

					List<? extends PlanElement> trip = tripRouter.calcRoute(
							leg.getMode(),
							new ActivityWrapperFacility( origin ),
							new ActivityWrapperFacility( destination ),
							now,
							person);

					Assert.assertEquals(
							"unexpected trip length for mode "+leg.getMode(),
							1,
							trip.size());

					Assert.assertEquals(
							"trip durations do not match for mode "+leg.getMode(),
							timePcr,
							((Leg) trip.get( 0 )).getTravelTime(),
							MatsimTestUtils.EPSILON);

					origin = destination;
				}
			}
		}
	}

	@Test
	public void testWrappedTravelTime() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = plan.getPlanElements().iterator();

				Activity origin = (Activity) iterator.next();

				double now = 0;
				while (iterator.hasNext()) {
					Leg leg = (Leg) iterator.next();
					Activity destination = (Activity) iterator.next();

					now = updateNow( now , origin );

					double timePcr = plansCalcRoute.handleLeg(
							person,
							leg,
							origin,
							destination,
							now);

					double timeWrapper = wrapper.handleLeg(
							person,
							leg,
							origin,
							destination,
							now);

					Assert.assertEquals(
							"trip durations do not match for mode "+leg.getMode(),
							timePcr,
							timeWrapper,
							MatsimTestUtils.EPSILON);

					origin = destination;
				}
			}
		}
	}

	@Test
	public void testWrappedMode() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = plan.getPlanElements().iterator();

				Activity origin = (Activity) iterator.next();

				double now = 0;
				while (iterator.hasNext()) {
					Leg leg = (Leg) iterator.next();
					Activity destination = (Activity) iterator.next();

					double endTime = origin.getEndTime();
					double startTime = origin.getStartTime();
					double dur = (origin instanceof ActivityImpl ? ((ActivityImpl) origin).getMaximumDuration() : Time.UNDEFINED_TIME);
					if (endTime != Time.UNDEFINED_TIME) {
						// use fromAct.endTime as time for routing
						now = endTime;
					}
					else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
						// use fromAct.startTime + fromAct.duration as time for routing
						now = startTime + dur;
					}
					else if (dur != Time.UNDEFINED_TIME) {
						// use last used time + fromAct.duration as time for routing
						now += dur;
					}
					else {
						throw new RuntimeException("activity of plan of person " + plan.getPerson().getId() + " has neither end-time nor duration." + origin);
					}

					String mode = leg.getMode();
					double timePcr = wrapper.handleLeg(
							person,
							leg,
							origin,
							destination,
							now);

					Assert.assertEquals(
							"unexpected mode after wrapped routing",
							mode,
							leg.getMode());

					origin = destination;
				}
			}
		}
	}

	@Test
	public void testUnwrappedMode() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = plan.getPlanElements().iterator();

				Activity origin = (Activity) iterator.next();

				double now = 0;
				while (iterator.hasNext()) {
					Leg leg = (Leg) iterator.next();
					Activity destination = (Activity) iterator.next();

					now = updateNow( now , origin );

					String mode = leg.getMode();
					List<? extends PlanElement> trip = tripRouter.calcRoute(
							leg.getMode(),
							new ActivityWrapperFacility( origin ),
							new ActivityWrapperFacility( destination ),
							now,
							person);

					Assert.assertEquals(
							"unexpected mode after unwrapped routing",
							mode,
							((Leg) trip.get(0)).getMode());

					origin = destination;
				}
			}
		}
	}

	private static double updateNow(
			final double now,
			final Activity origin) {
		double endTime = origin.getEndTime();
		double startTime = origin.getStartTime();
		double dur = (origin instanceof ActivityImpl ? ((ActivityImpl) origin).getMaximumDuration() : Time.UNDEFINED_TIME);
		if (endTime != Time.UNDEFINED_TIME) {
			// use fromAct.endTime as time for routing
			return endTime;
		}
		else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
			// use fromAct.startTime + fromAct.duration as time for routing
			return startTime + dur;
		}
		else if (dur != Time.UNDEFINED_TIME) {
			// use last used time + fromAct.duration as time for routing
			return now + dur;
		}
		else {
			throw new RuntimeException("activity has neither end-time nor duration." + origin);
		}
	}	
}

