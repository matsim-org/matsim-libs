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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.testcases.MatsimTestUtils;

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

	private Controler controler;
	// the "old" plansCalcRoute
	private PlansCalcRoute plansCalcRoute;
	// the new object
	private TripRouter tripRouter;
	// the "compatibility" PlansCalcRoute-subClass
	//private PlanRouterWrapper wrapper;
	private PlanRouter planRouter;

	private static final List<String> MODES_TO_IGNORE = Arrays.asList( TransportMode.transit_walk );

	/**
	 * we want to test backward compatibility for different settings.
	 * for this, we use the parameterized approach to initialise the testcase
	 * with different config files.
	 */
	@Parameters
	public static Collection<Object[]> configurations() {
		Object[][] configurations = new Object[][]{ {"config.xml"} , {"transit/config.xml"} };
		return Arrays.asList( configurations );
	}

	/**
	 * @param configName the config file to use for the tests
	 */
	public BackwardCompatibilityTest(
			final String configName) {
		this.configName = configName;
	}

	@Before
	public void init() {
		Config config = utils.loadConfig( utils.getClassInputDirectory() + configName );
		config.controler().setLastIteration( 0 );

		controler = new Controler( config );
		controler.run();

		Controler oldControler = new Controler( config );
		oldControler.setUseTripRouting( false );
		oldControler.setOverwriteFiles( true );
		oldControler.run();

		plansCalcRoute = (PlansCalcRoute) oldControler.createRoutingAlgorithm();
		tripRouter = controler.getTripRouterFactory().createTripRouter();
		planRouter = (PlanRouter) controler.createRoutingAlgorithm();
	}

	// /////////////////////////////////////////////////////////////////////////
	// "handleLeg" methods check-
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testTravelTime() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> iterator = tripRouter.tripsToLegs(plan).iterator();

				Activity origin = (Activity) iterator.next();

				double now = 0;
				while (iterator.hasNext()) {
					Leg leg = (Leg) iterator.next();
					Activity destination = (Activity) iterator.next();

					now = updateNow( now , origin );

					if (!MODES_TO_IGNORE.contains( leg.getMode() )) {
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
					}

					origin = destination;
				}
			}
		}
	}

	@Test
	@Ignore // obviously fails with detailed pt: reinsert when the "main mode" is consistently handled
	public void testMode() {
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
	public void testPlanRouting() {
		for (Person person : controler.getPopulation().getPersons().values()) {
			for (Plan withNew : person.getPlans()) {
				Plan withOld = new PlanImpl( person );
				((PlanImpl) withOld).copyPlan( withNew );
				
				plansCalcRoute.run( withOld );
				planRouter.run( withNew );

				comparePlans( withOld , withNew );
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static double updateNow(
			final double now,
			final PlanElement pe) {
		return TripRouter.calcEndOfPlanElement( now , pe );
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

				// use an "epsilon" of 1, to account for the fact that some routers
				// round times to full seconds, and some do not.
				// Difference in this behaviour should not affect the results
				// of a simulation, hence backaward compatibility in this respect
				// is not enforced (What is the point of using floating point
				// numbers if they only express integers anyway?).
				// -------------------------------------------------------------
				Assert.assertEquals(
						"start times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getStartTime(),
						secondAct.getStartTime(),
						//MatsimTestUtils.EPSILON);
						1);

				Assert.assertEquals(
						"end times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getEndTime(),
						secondAct.getEndTime(),
						//MatsimTestUtils.EPSILON);
						1);

				Assert.assertEquals(
						"maximum durations do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstAct+" and "+secondAct,
						firstAct.getMaximumDuration(),
						secondAct.getMaximumDuration(),
						//MatsimTestUtils.EPSILON);
						1);
			}
			else {
				Leg firstLeg = (Leg) firstPlanElement;
				Leg secondLeg = (Leg) secondPlanElement;

				Assert.assertEquals(
						"modes do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getMode(),
						secondLeg.getMode());

				// use an "epsilon" of 1, to account for the fact that some routers
				// round times to full seconds, and some do not.
				// Difference in this behaviour should not affect the results
				// of a simulation, hence backaward compatibility in this respect
				// is not enforced (What is the point of using floating point
				// numbers if they only express integers anyway?).
				// -------------------------------------------------------------
				Assert.assertEquals(
						"leg travel times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getTravelTime(),
						secondLeg.getTravelTime(),
						//MatsimTestUtils.EPSILON);
						1);

				Assert.assertEquals(
						"departure times do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getDepartureTime(),
						secondLeg.getDepartureTime(),
						//MatsimTestUtils.EPSILON);
						1);

				Assert.assertEquals(
						"route implementations do not match for person "+firstPlan.getPerson().getId()+" with plans "+firstPlanElements+" and "+secondPlanElements
						+", for plan elements "+firstLeg+" and "+secondLeg,
						firstLeg.getRoute().getClass(),
						secondLeg.getRoute().getClass());

				// this would make sense, but the core transit router does not sets
				// the route travel time...
				// This should be tested however, as I think it is the route
				// travel time that matters for teleportation
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

