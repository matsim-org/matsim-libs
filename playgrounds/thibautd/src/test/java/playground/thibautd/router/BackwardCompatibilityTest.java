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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * Tests whether the wrapping PlansCalcRoute gives the same results as
 * the base one.
 *
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class BackwardCompatibilityTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private final String configName;

	private MultiLegRoutingControler controler;
	// the "old" plansCalcRoute
	private PlansCalcRoute plansCalcRoute;
	// the new object
	private TripRouter tripRouter;
	// the "compatibility" PlansCalcRoute-subClass
	private PlanRouterWrapper wrapper;

	// we want to test backward compatibility for different settings.
	// for this, we use the parameterized approach to initialise the testcase
	// with different config files.
	// -------------------------------------------------------------------------
	@Parameters
	public static Collection<Object[]> configurations() {
		Object[][] configurations = new Object[][]{ {"config.xml"} , {"transit/config.xml"} };
		return Arrays.asList( configurations );
	}

	public BackwardCompatibilityTest(
			final String configName) {
		this.configName = configName;
	}

	@Before
	public void init() {
		Config config = utils.loadConfig( utils.getClassInputDirectory() + configName );
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

	// /////////////////////////////////////////////////////////////////////////
	// "handleLeg" methods check-
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testUnwrappedTravelTime() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = tripRouter.tripsToLegs(plan).iterator();

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
							"trip durations do not match for mode "+leg.getMode(),
							timePcr,
							getTravelTime( now , trip ),
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
				Iterator<PlanElement> iterator = tripRouter.tripsToLegs(plan).iterator();

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
				Iterator<PlanElement> iterator = tripRouter.tripsToLegs(plan).iterator();

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
	@Ignore // obviously fails with detailed pt: reinsert when the "main mode" is consistently handled
	public void testUnwrappedMode() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = tripRouter.tripsToLegs(plan).iterator();

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

	// /////////////////////////////////////////////////////////////////////////
	// plan routing methods check-
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testUnwrappedPlanRouting() {
		PlanRouter planRouter = new PlanRouter( tripRouter );
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Plan newPlan = new PlanImpl( person );
				((PlanImpl) newPlan).copyPlan( plan );
				
				plansCalcRoute.run( newPlan );
				planRouter.run( plan );

				comparePlans( newPlan , plan );
			}
		}
	}


	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof ActivityImpl ? ((ActivityImpl) act).getMaximumDuration() : Time.UNDEFINED_TIME);
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
				throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		else {
			return now + ((Leg) pe).getTravelTime();
		}
	}	

	private static void comparePlans(
			final Plan firstPlan,
			final Plan secondPlan) {
		// TODO: it would be nice to put each assertion in a separate test,
		// so that failure of one assertion does not prevent to be notified of failure
		// of others (for example, different departure times may be due to an
		// error in travel times: we want to be notified of both.)
		List<PlanElement> firstPlanElements = firstPlan.getPlanElements();
		List<PlanElement> secondPlanElements = secondPlan.getPlanElements();

		Assert.assertEquals(
				"plan lengths do not match",
				firstPlanElements.size(),
				secondPlanElements.size());

		Iterator<PlanElement> firstIterator = firstPlanElements.iterator();
		Iterator<PlanElement> secondIterator = secondPlanElements.iterator();

		PlanElement firstPlanElement, secondPlanElement;

		for ( firstPlanElement = firstIterator.next(), secondPlanElement = secondIterator.next();
				firstIterator.hasNext();
				firstPlanElement = firstIterator.next(), secondPlanElement = secondIterator.next()) {
			if (firstPlanElement instanceof Activity) {
				Activity firstAct = (Activity) firstPlanElement;
				Activity secondAct = (Activity) secondPlanElement;

				Assert.assertEquals(
						"act types do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getType(),
						secondAct.getType());

				Assert.assertEquals(
						"act link ids do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getLinkId(),
						secondAct.getLinkId());

				Assert.assertEquals(
						"act coords do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getCoord(),
						secondAct.getCoord());

				Assert.assertEquals(
						"start times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getStartTime(),
						secondAct.getStartTime(),
						MatsimTestUtils.EPSILON);

				Assert.assertEquals(
						"end times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getEndTime(),
						secondAct.getEndTime(),
						MatsimTestUtils.EPSILON);

				Assert.assertEquals(
						"maximum durations do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getMaximumDuration(),
						secondAct.getMaximumDuration(),
						MatsimTestUtils.EPSILON);
			}
			else {
				Leg firstLeg = (Leg) firstPlanElement;
				Leg secondLeg = (Leg) secondPlanElement;

				Assert.assertEquals(
						"modes do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getMode(),
						secondLeg.getMode());

				Assert.assertEquals(
						"leg travel times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getTravelTime(),
						secondLeg.getTravelTime(),
						MatsimTestUtils.EPSILON);

				Assert.assertEquals(
						"departure times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getDepartureTime(),
						secondLeg.getDepartureTime(),
						MatsimTestUtils.EPSILON);

				Assert.assertEquals(
						"route implementations do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getRoute().getClass(),
						secondLeg.getRoute().getClass());

				// this would make sense, but the core transit router does not sets
				// the route travel time...
				//Assert.assertEquals(
				//		"route travel times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
				//		+", for plan elements "+firstLeg+" and "+secondLeg,
				//		firstLeg.getRoute().getTravelTime(),
				//		secondLeg.getRoute().getTravelTime(),
				//		MatsimTestUtils.EPSILON);

				Assert.assertEquals(
						"start links do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getRoute().getStartLinkId(),
						secondLeg.getRoute().getStartLinkId());

				Assert.assertEquals(
						"end links do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getRoute().getEndLinkId(),
						secondLeg.getRoute().getEndLinkId());
			}
		}
	}

	private static double getTravelTime(
			final double depTime,
			final List<? extends PlanElement> trip) {
		double now = depTime;

		for (PlanElement pe : trip) {
			now = updateNow( now , pe );
		}

		return now - depTime;
	}
}

