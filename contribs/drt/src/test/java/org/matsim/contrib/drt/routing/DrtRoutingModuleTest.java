/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.routing;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.passenger.DefaultDvrpLoadFromTrip;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.speedy.SpeedyDijkstraFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestUtils;

import com.google.common.collect.ImmutableMap;

/**
 * @author jbischoff
 */
public class DrtRoutingModuleTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCottbusClosestAccessEgressStopFinder() {
		Scenario scenario = createTestScenario();
		ActivityFacilities facilities = scenario.getActivityFacilities();
		final double networkTravelSpeed = 0.83333;
		final double beelineFactor = 1.3;
		TeleportationRoutingModule walkRouter = new TeleportationRoutingModule(TransportMode.walk, scenario,
				networkTravelSpeed, beelineFactor);
		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(scenario.getConfig());
		String drtMode = "DrtX";
		drtCfg.mode = drtMode;
		DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
				(DefaultDrtOptimizationConstraintsSet) drtCfg.addOrGetDrtOptimizationConstraintsParams()
						.addOrGetDefaultDrtOptimizationConstraintsSet();
		defaultConstraintsSet.maxTravelTimeAlpha = 1.5;
		defaultConstraintsSet.maxTravelTimeBeta = 5 * 60;
		defaultConstraintsSet.maxWaitTime = 5 * 60;

		DvrpLoadType loadType = new IntegerLoadType("passengers");
		DvrpLoadFromTrip loadCreator = new DefaultDvrpLoadFromTrip(loadType, "passengers");

		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
				.getFacilities()
				.values()
				.stream()
				.map(DrtStopFacilityImpl::createFromFacility)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));

		AccessEgressFacilityFinder stopFinder = new ClosestAccessEgressFacilityFinder(
				defaultConstraintsSet.maxWalkDistance,
				scenario.getNetwork(), QuadTrees.createQuadTree(drtStops.values()));
		DrtRouteCreator drtRouteCreator = new DrtRouteCreator(drtCfg, scenario.getNetwork(),
				new SpeedyDijkstraFactory(), new FreeSpeedTravelTime(), TimeAsTravelDisutility::new,
				new DefaultDrtRouteConstraintsCalculator(drtCfg, 
						(departureTime, accessActLink, egressActLink, person, tripAttributes) -> Optional.of(defaultConstraintsSet)),
						loadCreator, loadType);
		DefaultMainLegRouter mainRouter = new DefaultMainLegRouter(drtMode, scenario.getNetwork(),
				scenario.getPopulation().getFactory(), drtRouteCreator);
		DvrpRoutingModule dvrpRoutingModule = new DvrpRoutingModule(mainRouter, walkRouter, walkRouter, stopFinder,
				drtMode, TimeInterpretation.create(scenario.getConfig()));

		// case 1: origin and destination within max walking distance from next stop (200m)
		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId(1));
		Activity h = (Activity)p1.getSelectedPlan().getPlanElements().get(0);
		Facility hf = FacilitiesUtils.toFacility(h, facilities);

		Activity w = (Activity)p1.getSelectedPlan().getPlanElements().get(2);
		Facility wf = FacilitiesUtils.toFacility(w, facilities);

		List<? extends PlanElement> routedList = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf, wf, 8 * 3600, p1));

		Assertions.assertEquals(5, routedList.size());

		Leg accessLegP1 = (Leg)routedList.get(0);
		GenericRouteImpl accessLegP1Route = (GenericRouteImpl)accessLegP1.getRoute();

		Activity stageActivityAccessP1 = (Activity)routedList.get(1);

		Leg realDrtLegP1 = (Leg)routedList.get(2);
		DrtRoute realDrtLegP1Route = (DrtRoute)realDrtLegP1.getRoute();

		Activity stageActivityEgressP1 = (Activity)routedList.get(3);

		Leg egressLegP1 = (Leg)routedList.get(4);
		GenericRouteImpl egressLegP1Route = (GenericRouteImpl)egressLegP1.getRoute();

		// drt boarding should be at link id 2183 or 2184, not totally clear which one of these (from and to nodes inverted)
		// drt alighting should be at link id 5866 or 5867, not totally clear which one of these (from and to nodes inverted)

		Assertions.assertEquals(TransportMode.walk, accessLegP1.getMode());
		Assertions.assertEquals(Id.createLinkId(3699), accessLegP1Route.getStartLinkId());
		Assertions.assertEquals(Id.createLinkId(2184), accessLegP1Route.getEndLinkId());

		Assertions.assertEquals(drtMode + " interaction", stageActivityAccessP1.getType());
		Assertions.assertEquals(Id.createLinkId(2184), stageActivityAccessP1.getLinkId());

		Assertions.assertEquals(drtMode, realDrtLegP1.getMode());
		Assertions.assertEquals(Id.createLinkId(2184), realDrtLegP1Route.getStartLinkId());
		/*
		 * links 5866 and 5867 are located between the same nodes, so their drt stops should be at the same place place,
		 * too. Therefore it is not clear which one of these is the right one, as ClosestAccessEgressStopFinder should
		 * find the nearest drt stop, but both have the same distance from the destination facility.
		 */
		Assertions.assertTrue(
				realDrtLegP1Route.getEndLinkId().equals(Id.createLinkId(5866)) || realDrtLegP1Route.getEndLinkId()
						.equals(Id.createLinkId(5867)));
		Id<Link> endLink = realDrtLegP1Route.getEndLinkId();
		// Check of other, more drt-specific attributes of the DrtRoute is missing, maybe these should be tested in DrtRoutingModule instead

		Assertions.assertEquals(drtMode + " interaction", stageActivityEgressP1.getType());
		Assertions.assertEquals(endLink, stageActivityEgressP1.getLinkId());

		Assertions.assertEquals(TransportMode.walk, egressLegP1.getMode());
		Assertions.assertEquals(endLink, egressLegP1Route.getStartLinkId());
		Assertions.assertEquals(Id.createLinkId(7871), egressLegP1Route.getEndLinkId());

		// case 2: origin and destination outside max walking distance from next stop (>2000m vs. max 200m)
		Person p2 = scenario.getPopulation().getPersons().get(Id.createPersonId(2));
		Activity h2 = (Activity)p2.getSelectedPlan().getPlanElements().get(0);
		Facility hf2 = FacilitiesUtils.toFacility(h2, facilities);

		Activity w2 = (Activity)p2.getSelectedPlan().getPlanElements().get(2);
		Facility wf2 = FacilitiesUtils.toFacility(w2, facilities);

		List<? extends PlanElement> routedList2 = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf2, wf2, 8 * 3600, p2));

		Assertions.assertNull(routedList2);

		// case 3: origin and destination at the same coordinate, > 2000 m walking distance from next stop
		Person p3 = scenario.getPopulation().getPersons().get(Id.createPersonId(3));
		Activity h3 = (Activity)p3.getSelectedPlan().getPlanElements().get(0);
		Facility hf3 = FacilitiesUtils.toFacility(h3, facilities);

		Activity w3 = (Activity)p3.getSelectedPlan().getPlanElements().get(2);
		Facility wf3 = FacilitiesUtils.toFacility(w3, facilities);

		List<? extends PlanElement> routedList3 = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf3, wf3, 8 * 3600, p3));

		Assertions.assertNull(routedList3);

		// case 4: origin and destination at the same coordinate, in 200 m walking distance from next stop
		Person p4 = scenario.getPopulation().getPersons().get(Id.createPersonId(4));
		Activity h4 = (Activity)p4.getSelectedPlan().getPlanElements().get(0);
		Facility hf4 = FacilitiesUtils.toFacility(h4, facilities);

		Activity w4 = (Activity)p4.getSelectedPlan().getPlanElements().get(2);
		Facility wf4 = FacilitiesUtils.toFacility(w4, facilities);

		List<? extends PlanElement> routedList4 = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf4, wf4, 8 * 3600, p4));

		Assertions.assertNull(routedList4);

		// case 5: origin within 200 m walking distance from next stop, but destination outside walking distance
		Person p5 = scenario.getPopulation().getPersons().get(Id.createPersonId(5));
		Activity h5 = (Activity)p5.getSelectedPlan().getPlanElements().get(0);
		Facility hf5 = FacilitiesUtils.toFacility(h5, facilities);

		Activity w5 = (Activity)p5.getSelectedPlan().getPlanElements().get(2);
		Facility wf5 = FacilitiesUtils.toFacility(w5, facilities);

		List<? extends PlanElement> routedList5 = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf5, wf5, 8 * 3600, p5));

		// TODO: Asserts are prepared for interpreting maxWalkingDistance as a real maximum, but routing still works wrongly
		Assertions.assertNull(routedList5);

		// case 6: destination within 200 m walking distance from next stop, but origin outside walking distance
		Person p6 = scenario.getPopulation().getPersons().get(Id.createPersonId(6));
		Activity h6 = (Activity)p6.getSelectedPlan().getPlanElements().get(0);
		Facility hf6 = FacilitiesUtils.toFacility(h6, facilities);

		Activity w6 = (Activity)p6.getSelectedPlan().getPlanElements().get(2);
		Facility wf6 = FacilitiesUtils.toFacility(w6, facilities);

		List<? extends PlanElement> routedList6 = dvrpRoutingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(hf6, wf6, 8 * 3600, p6));

		// TODO: Asserts are prepared for interpreting maxWalkingDistance as a real maximum, but routing still works wrongly
		Assertions.assertNull(routedList6);

	}

	@Test
	void testRouteDescriptionHandling() {
		String oldRouteFormat = "600 400";
		String newRouteFormat = "{\"maxWaitTime\":600.0,\"directRideTime\":400.0,\"unsharedPath\":[\"a\",\"b\",\"c\"]}";

		Scenario scenario = createTestScenario();
		ActivityFacilities facilities = scenario.getActivityFacilities();

		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId(1));
		Activity h = (Activity)p1.getSelectedPlan().getPlanElements().get(0);
		Facility hf = FacilitiesUtils.toFacility(h, facilities);

		Activity w = (Activity)p1.getSelectedPlan().getPlanElements().get(2);
		Facility wf = FacilitiesUtils.toFacility(w, facilities);

		DrtRoute drtRoute = new DrtRoute(h.getLinkId(), w.getLinkId());

		drtRoute.setRouteDescription(oldRouteFormat);
		Assertions.assertTrue(drtRoute.getMaxWaitTime() == 600.);
		Assertions.assertTrue(drtRoute.getDirectRideTime() == 400);

		drtRoute.setRouteDescription(newRouteFormat);
		Assertions.assertTrue(drtRoute.getMaxWaitTime() == 600.);
		Assertions.assertTrue(drtRoute.getDirectRideTime() == 400);
		Assertions.assertTrue(drtRoute.getUnsharedPath().equals(Arrays.asList("a", "b", "c")));

	}

	/**
	 * @return
	 */
	private Scenario createTestScenario() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 200;
		drtConfigGroup.transitStopFile = utils.getClassInputDirectory() + "testCottbus/drtstops.xml.gz";
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(
				utils.getClassInputDirectory() + "testCottbus/network.xml.gz");
		new TransitScheduleReader(scenario).readFile(drtConfigGroup.transitStopFile);
		createSomeAgents(scenario);
		return scenario;
	}

	/**
	 * @param scenario
	 */
	private void createSomeAgents(Scenario scenario) {
		PopulationFactory pf = scenario.getPopulation().getFactory();

		// origin and destination in 200 m walking distance from next stop
		Person p1 = pf.createPerson(Id.createPersonId(1));
		Plan plan1 = pf.createPlan();
		p1.addPlan(plan1);
		Activity home = pf.createActivityFromCoord("home", new Coord(451931.406932525, 5733832.50176344));
		home.setLinkId(Id.createLinkId(3699));
		home.setEndTime(8 * 3600);
		plan1.addActivity(home);
		plan1.addLeg(pf.createLeg("drt"));
		Activity work = pf.createActivityFromCoord("work", new Coord(453595.36658007314, 5734504.7695318265));
		work.setLinkId(Id.createLinkId(7871));
		plan1.addActivity(work);
		scenario.getPopulation().addPerson(p1);

		// origin and destination > 2000 m walking distance from next stop
		Person p2 = pf.createPerson(Id.createPersonId(2));
		Plan plan2 = pf.createPlan();
		p2.addPlan(plan2);
		Activity home2 = pf.createActivityFromCoord("home", new Coord(460077.7116017367, 5740133.3409971865));
		home2.setLinkId(Id.createLinkId(9541));
		home2.setEndTime(8 * 3600);
		plan2.addActivity(home2);
		plan2.addLeg(pf.createLeg("drt"));
		Activity work2 = pf.createActivityFromCoord("work", new Coord(461757.56027226395, 5742929.001039858));
		work2.setLinkId(Id.createLinkId(7717));
		plan2.addActivity(work2);
		scenario.getPopulation().addPerson(p2);

		// origin and destination at the same coordinate, different links! (should we test link inside walking distance but coordinate outside etc.?), > 2000 m walking distance from next stop
		Person p3 = pf.createPerson(Id.createPersonId(3));
		Plan plan3 = pf.createPlan();
		p3.addPlan(plan3);
		Activity home3 = pf.createActivityFromCoord("home", new Coord(460077.7116017367, 5740133.3409971865));
		home3.setLinkId(Id.createLinkId(9541));
		home3.setEndTime(8 * 3600);
		plan3.addActivity(home3);
		plan3.addLeg(pf.createLeg("drt"));
		Activity work3 = pf.createActivityFromCoord("work", new Coord(460077.7116017367, 5740133.3409971865));
		work3.setLinkId(Id.createLinkId(7717)); // TODO distant link - really to be tested?
		plan3.addActivity(work3);
		scenario.getPopulation().addPerson(p3);

		// origin and destination at the same coordinate, in 200 m walking distance from next stop
		Person p4 = pf.createPerson(Id.createPersonId(4));
		Plan plan4 = pf.createPlan();
		p4.addPlan(plan4);
		Activity home4 = pf.createActivityFromCoord("home", new Coord(451931.406932525, 5733832.50176344));
		home4.setLinkId(Id.createLinkId(3699));
		home4.setEndTime(8 * 3600);
		plan4.addActivity(home4);
		plan4.addLeg(pf.createLeg("drt"));
		Activity work4 = pf.createActivityFromCoord("work", new Coord(451931.406932525, 5733832.50176344));
		work4.setLinkId(Id.createLinkId(3699));
		plan4.addActivity(work4);
		scenario.getPopulation().addPerson(p4);

		// origin within 200 m walking distance from next stop, but destination outside walking distance
		Person p5 = pf.createPerson(Id.createPersonId(5));
		Plan plan5 = pf.createPlan();
		p5.addPlan(plan5);
		Activity home5 = pf.createActivityFromCoord("home", new Coord(451931.406932525, 5733832.50176344));
		home5.setLinkId(Id.createLinkId(3699));
		home5.setEndTime(8 * 3600);
		plan5.addActivity(home5);
		plan5.addLeg(pf.createLeg("drt"));
		Activity work5 = pf.createActivityFromCoord("work", new Coord(460077.7116017367, 5740133.3409971865));
		work5.setLinkId(Id.createLinkId(9541));
		plan5.addActivity(work5);
		scenario.getPopulation().addPerson(p5);

		// destination within 200 m walking distance from next stop, but origin outside walking distance
		Person p6 = pf.createPerson(Id.createPersonId(6));
		Plan plan6 = pf.createPlan();
		p6.addPlan(plan6);
		Activity home6 = pf.createActivityFromCoord("home", new Coord(460077.7116017367, 5740133.3409971865));
		home6.setLinkId(Id.createLinkId(9541));
		home6.setEndTime(8 * 3600);
		plan6.addActivity(home6);
		plan6.addLeg(pf.createLeg("drt"));
		Activity work6 = pf.createActivityFromCoord("work", new Coord(451931.406932525, 5733832.50176344));
		work6.setLinkId(Id.createLinkId(3699));
		plan6.addActivity(work6);
		scenario.getPopulation().addPerson(p6);
	}

}
