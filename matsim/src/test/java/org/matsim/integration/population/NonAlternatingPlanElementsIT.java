/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.integration.population;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests that a simple simulation can be run with plans where
 * activities and legs are not always alternating.
 *
 * @author mrieser / senozon
 */
public class NonAlternatingPlanElementsIT {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test_Controler_QSim_Routechoice_acts() {
		Config config = this.utils.loadConfig("test/scenarios/equil/config.xml");
		config.controller().setMobsim("qsim");
		config.controller().setLastIteration(10);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		config.replanning().addParam("Module_2", "ReRoute");
		config.replanning().addParam("ModuleProbability_2", "1.0");
		config.transit().setUseTransit(true);

		// a scenario is created to take only network from config file; rest inputs are ignored;
		// facility file is provided in config and facilitySource is 'fromFile', the facilitySource must be changed. Amit Jan'18
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");

		addSimpleTransitServices(scenario);

		Plan plan = createPlanWithConsecutiveActivitiesForEquilNet(scenario);
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(1, Person.class));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setCreateGraphs(false);
        controler.run();

		Assertions.assertTrue(person.getPlans().size() > 1); // ensure there was some replanning
	}

	@Test
	void test_Controler_QSim_Routechoice_legs() {
		Config config = this.utils.loadConfig("test/scenarios/equil/config.xml");
		config.controller().setMobsim("qsim");
		config.controller().setLastIteration(10);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		config.replanning().addParam("Module_2", "ReRoute");
		config.replanning().addParam("ModuleProbability_2", "1.0");
		config.transit().setUseTransit(true);

		// a scenario is created to take only network from config file; rest inputs are ignored;
		// facility file is provided in config and facilitySource is 'fromFile', the facilitySource must be changed. Amit Jan'18
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");

		addSimpleTransitServices(scenario);

		Plan plan = createPlanWithConsecutiveLegsForEquilNet(scenario);
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(1, Person.class));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setCreateGraphs(false);
        controler.run();

		Assertions.assertTrue(person.getPlans().size() > 1); // ensure there was some replanning
	}

	// TODO: make more complicated plans when testing subtour mode choice
	private static Plan createPlanWithConsecutiveLegsForEquilNet(final Scenario scenario) {
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Plan plan = pf.createPlan();

		Activity home1 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x1 = -17000;
		((Activity) home1).setCoord(new Coord(x1, (double) 500));
		home1.setEndTime(7.0 * 3600);

		Leg leg1 = pf.createLeg("transit_walk");
		leg1.setRoute(RouteUtils.createGenericRouteImpl(Id.create(1, Link.class), Id.create(14, Link.class)));
		leg1.getRoute().setTravelTime(0.); // retrofitting to repair failing test. kai, apr'15

		Leg leg2 = pf.createLeg("pt");
		leg2.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.create(14, Link.class), new Id[] {Id.create(20, Link.class)}, Id.create(21, Link.class)));
		leg2.getRoute().setTravelTime(0.); // retrofitting to repair failing test. kai, apr'15

		Leg leg3 = pf.createLeg("transit_walk");
		leg3.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.create(14, Link.class), new Id[0], Id.create(14, Link.class)));
		leg3.getRoute().setTravelTime(0.); // retrofitting to repair failing test. kai, apr'15

		Activity work = pf.createActivityFromLinkId("w", Id.create(21, Link.class));
		work.setEndTime(17.0 * 3600);
		final double y = -8000;
		((Activity) work).setCoord(new Coord((double) 5000, y));

		Leg leg4 = pf.createLeg("car");
		leg4.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.create(21, Link.class), new Id[] {Id.create(22, Link.class), Id.create(23, Link.class)}, Id.create(1, Link.class)));
		leg4.getRoute().setTravelTime(0.); // retrofitting to repair failing test. kai, apr'15

		Activity home2 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x = -17000;
		((Activity) home2).setCoord(new Coord(x, (double) 500));

		plan.addActivity(home1);
		plan.addLeg(leg1);
		plan.addLeg(leg2);
		plan.addLeg(leg3);
		plan.addActivity(work);
		plan.addLeg(leg4);
		plan.addActivity(home2);

		return plan;
	}

	private static Plan createPlanWithConsecutiveActivitiesForEquilNet(final Scenario scenario) {
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Plan plan = pf.createPlan();

		Activity home1 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x3 = -17000;
		((Activity) home1).setCoord(new Coord(x3, (double) 500));
		home1.setEndTime(7.0 * 3600);

		Leg leg1 = pf.createLeg("walk");
		leg1.setRoute(RouteUtils.createGenericRouteImpl(Id.create(1, Link.class), Id.create(21, Link.class)));
		leg1.getRoute().setTravelTime(0.); // retrofitting failing test. kai, apr'15

		Activity work = pf.createActivityFromLinkId("w", Id.create(21, Link.class));
		work.setEndTime(17.0 * 3600);
		final double y1 = -8000;
		((Activity) work).setCoord(new Coord((double) 5000, y1));

		Activity shop = pf.createActivityFromLinkId("h", Id.create(21, Link.class));
		shop.setEndTime(17.5 * 3600);
		final double y = -8000;
		((Activity) shop).setCoord(new Coord((double) 5000, y));

		Leg leg2 = pf.createLeg("car");
		leg2.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.create(21, Link.class), new Id[] {Id.create(22, Link.class), Id.create(23, Link.class)}, Id.create(1, Link.class)));
		leg2.getRoute().setTravelTime(0.); // retrofitting failing test. kai, apr'15

		Activity home2 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x2 = -17000;
		((Activity) home2).setCoord(new Coord(x2, (double) 500));
		home2.setEndTime(21 * 3600);

		Activity home3 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x1 = -17000;
		((Activity) home2).setCoord(new Coord(x1, (double) 500));
		home2.setEndTime(22 * 3600);

		Activity home4 = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
		final double x = -17000;
		((Activity) home2).setCoord(new Coord(x, (double) 500));

		plan.addActivity(home1);
		plan.addLeg(leg1);
		plan.addActivity(work);
		plan.addActivity(shop);
		plan.addLeg(leg2);
		plan.addActivity(home2);
		plan.addActivity(home3);
		plan.addActivity(home4);

		return plan;
	}

	private void addSimpleTransitServices(Scenario scenario) {

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory f = schedule.getFactory();
		final double x = -6000;
		TransitStopFacility stopFacility1 = f.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(x, (double) 1500), false);
		stopFacility1.setLinkId(Id.create(14, Link.class));
		final double y = -4000;
		TransitStopFacility stopFacility2 = f.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord((double) 5000, y), false);
		stopFacility2.setLinkId(Id.create(21, Link.class));
		schedule.addStopFacility(stopFacility1);
		schedule.addStopFacility(stopFacility2);

		TransitLine line1 = f.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(Id.create("14", Link.class), new Id[] { Id.create("20", Link.class) }, Id.create("21", Link.class));
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStopBuilder(stopFacility1).departureOffset(0).build());
		stops.add(f.createTransitRouteStopBuilder(stopFacility2).arrivalOffset(180).build());
		TransitRoute route1 = f.createTransitRoute(Id.create(1, TransitRoute.class), netRoute, stops, "bus");
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		for (int i = 0; i < 20; i++) {
			route1.addDeparture(f.createDeparture(Id.create(i, Departure.class), 6.0 * 3600 + i * 600));
		}

		new CreateVehiclesForSchedule(schedule, scenario.getTransitVehicles()).run();
	}

}
