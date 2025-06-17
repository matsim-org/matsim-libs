/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.utils.misc;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class PopulationUtilsTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testLegOverlap() {
		Fixture f = new Fixture() ;
		List<Leg> legs1 = PopulationUtils.getLegs(f.plan1) ;
		List<Leg> legs2 = PopulationUtils.getLegs(f.plan2);
		List<Leg> legs3 = PopulationUtils.getLegs(f.plan3);

//		Assert.assertEquals( 2., PopulationUtils.calculateSimilarity( legs1, legs2, null, 1., 1. ) , 0.001 ) ;
		Assertions.assertEquals( 4., PopulationUtils.calculateSimilarity( legs1, legs2, null, 1., 1. ) , 0.001 ) ;
		// (no route is now counted as "same route" and thus reaps the reward. kai, jul'18)

//		Assert.assertEquals( 1., PopulationUtils.calculateSimilarity( legs1, legs3, null, 1., 1. ) , 0.001 ) ;
		Assertions.assertEquals( 2., PopulationUtils.calculateSimilarity( legs1, legs3, null, 1., 1. ) , 0.001 ) ;
		// (no route is now counted as "same route" and thus reaps the reward. kai, jul'18)

	}

	@Test
	void testActivityOverlap() {
		Fixture f = new Fixture() ;
		List<Activity> acts1 = PopulationUtils.getActivities(f.plan1, StageActivityHandling.StagesAsNormalActivities ) ;
		List<Activity> acts2 = PopulationUtils.getActivities(f.plan2, StageActivityHandling.StagesAsNormalActivities ) ;
		List<Activity> acts3 = PopulationUtils.getActivities(f.plan3, StageActivityHandling.StagesAsNormalActivities ) ;

		Assertions.assertEquals( 6., PopulationUtils.calculateSimilarity( acts1, acts2 , 1., 1., 0. ) , 0.001 ) ;
		Assertions.assertEquals( 5., PopulationUtils.calculateSimilarity( acts1, acts3 , 1., 1., 0. ) , 0.001 ) ;
	}

	private static class Fixture {
		Plan plan1, plan2, plan3 ;
		Fixture() {
			Config config = ConfigUtils.createConfig() ;
			Scenario scenario = ScenarioUtils.createScenario(config) ;
			Population pop = scenario.getPopulation() ;
			PopulationFactory pf = pop.getFactory() ;

			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);

				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("w", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;

				plan1 = plan ;
			}
			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);

				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("w", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;

				plan2 = plan ;
			}
			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);

				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("s", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.bike ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;

				plan3 = plan ;
			}
		}
	}

	@Test
	void testEmptyPopulation() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario s2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(PopulationComparison.Result.equal, PopulationComparison.compare(s1.getPopulation(), s2.getPopulation()));
	}

	@Test
	void testEmptyPopulationVsOnePerson() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario s2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Person person = s2.getPopulation().getFactory().createPerson(Id.create("1", Person.class));
		s2.getPopulation().addPerson(person);
		Assertions.assertEquals(PopulationComparison.Result.notEqual, PopulationComparison.compare(s1.getPopulation(), s2.getPopulation()));
		Assertions.assertEquals(PopulationComparison.Result.notEqual, PopulationComparison.compare(s2.getPopulation(), s1.getPopulation()));
	}

	@Test
	void testCompareBigPopulationWithItself() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		new MatsimNetworkReader(s1.getNetwork()).readFile(netFileName);
		new PopulationReader(s1).readFile(popFileName);
		Assertions.assertEquals(PopulationComparison.Result.equal, PopulationComparison.compare(s1.getPopulation(), s1.getPopulation()));
	}

	@Test
	void testPlanAttributesCopy() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		final Person person = population.getFactory().createPerson(Id.createPersonId("Donald Trump"));

		final Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);

		final Activity act = population.getFactory().createActivityFromCoord("speech", new Coord(0, 0));
		plan.addActivity(act);

		act.getAttributes().putAttribute("makes sense", false);
		act.getAttributes().putAttribute("length", 1895L);

		final Leg leg = population.getFactory().createLeg("SUV");
		plan.addLeg(leg);
		plan.addActivity(population.getFactory().createActivityFromLinkId("tweet", Id.createLinkId(2)));

		leg.getAttributes().putAttribute("mpg", 0.000001d);


		final Plan planCopy = population.getFactory().createPlan();
		PopulationUtils.copyFromTo(plan, planCopy);

		Assertions.assertEquals(plan.getPlanElements().size(),
			planCopy.getPlanElements().size(),
			"unexpected plan length");

		final Activity activityCopy = (Activity) planCopy.getPlanElements().getFirst();

		Assertions.assertEquals(act.getAttributes().getAttribute("makes sense"),
			activityCopy.getAttributes().getAttribute("makes sense"),
			"unexpected attribute");

		Assertions.assertEquals(act.getAttributes().getAttribute("length"),
			activityCopy.getAttributes().getAttribute("length"),
			"unexpected attribute");

		final Leg legCopy = (Leg) planCopy.getPlanElements().get(1);

		Assertions.assertEquals(leg.getAttributes().getAttribute("mpg"),
			legCopy.getAttributes().getAttribute("mpg"),
			"unexpected attribute");
	}

	@Test
	void testFirstActivityBeforeCar_carFirstLeg() {
		PopulationFactory pf = getPopulationFactory();

		{
			Plan plan = pf.createPlan();
			Activity h = pf.createActivityFromLinkId("h", Id.createLinkId("1"));
			plan.addActivity(h);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("w", Id.createLinkId("2")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(plan);
			Assertions.assertEquals(h, firstActivityAfterLastCarLegOfDay);
		}
	}

	@Test
	void testFirstActivityBeforeCar_noCar() {
		PopulationFactory pf = getPopulationFactory();

		{
			Plan plan = pf.createPlan();
			Activity h = pf.createActivityFromLinkId("h", Id.createLinkId("1"));
			plan.addActivity(h);
			plan.addLeg(pf.createLeg(TransportMode.pt));
			plan.addActivity(pf.createActivityFromLinkId("w", Id.createLinkId("2")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(plan);
			Assertions.assertNull(firstActivityAfterLastCarLegOfDay);
		}
	}

	@Test
	void testFirstActivityBeforeCar_carSecond() {
		PopulationFactory pf = getPopulationFactory();

		{
			Plan plan = pf.createPlan();
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			Activity w = pf.createActivityFromLinkId("w", Id.createLinkId("2"));
			plan.addActivity(w);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(plan);
			Assertions.assertEquals(w, firstActivityAfterLastCarLegOfDay);
		}
	}

	@Test
	void testFirstActivityBeforeCar_twoCar() {
		PopulationFactory pf = getPopulationFactory();

		{
			Plan plan = pf.createPlan();
			Activity h = pf.createActivityFromLinkId("h", Id.createLinkId("1"));
			plan.addActivity(h);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("w", Id.createLinkId("2")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			plan.addActivity(pf.createActivityFromLinkId("l", Id.createLinkId("3")));
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(plan);
			Assertions.assertEquals(h, firstActivityAfterLastCarLegOfDay);
		}
	}

	private static PopulationFactory getPopulationFactory() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population pop = scenario.getPopulation();
		pop.removePerson(Id.createPersonId("1"));
		Assertions.assertTrue(pop.getPersons().isEmpty());
		PopulationFactory pf = pop.getFactory();
		return pf;
	}
}
