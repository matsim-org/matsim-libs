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

/**
 *
 */
package org.matsim.contrib.drt.routing;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.util.List;

/**
 * @author jbischoff
 */
public class StopBasedDrtRoutingModuleTest {

	@Test
	public void test() {
		Scenario scenario = createTestScenario();
		ActivityFacilities facilities = scenario.getActivityFacilities();;
		final Double networkTravelSpeed = 0.83333;
		final Double beelineFactor = 1.3;
		TeleportationRoutingModule walkRouter = new TeleportationRoutingModule(TransportMode.walk,
				scenario.getPopulation().getFactory(), networkTravelSpeed, beelineFactor);
		DrtConfigGroup drtCfg = DrtConfigGroup.get(scenario.getConfig());
		AccessEgressStopFinder stopFinder = new DefaultAccessEgressStopFinder(scenario.getTransitSchedule(), drtCfg,
				scenario.getConfig().plansCalcRoute(), scenario.getNetwork());
		DrtRoutingModule drtRoutingModule = new DrtRoutingModule(drtCfg, scenario.getNetwork(),
				new FreeSpeedTravelTime(), TimeAsTravelDisutility::new, scenario.getPopulation().getFactory(),
				walkRouter);
		StopBasedDrtRoutingModule stopBasedDRTRoutingModule = new StopBasedDrtRoutingModule(
				scenario.getPopulation().getFactory(), drtRoutingModule, walkRouter, stopFinder, drtCfg);

		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId(1));
		Activity h = (Activity)p1.getSelectedPlan().getPlanElements().get(0);
		Facility hf = ActivityWrapperFacility.toFacility(h, facilities );

		Activity w = (Activity)p1.getSelectedPlan().getPlanElements().get(2);
		Facility wf = ActivityWrapperFacility.toFacility(w, facilities );

		List<? extends PlanElement> routedList = stopBasedDRTRoutingModule.calcRoute(hf, wf, 8 * 3600, p1);

		Assert.assertEquals(5, routedList.size());

		Person p2 = scenario.getPopulation().getPersons().get(Id.createPersonId(2));
		Activity h2 = (Activity)p2.getSelectedPlan().getPlanElements().get(0);
		Facility hf2 = ActivityWrapperFacility.toFacility(h2, facilities );

		Activity w2 = (Activity)p2.getSelectedPlan().getPlanElements().get(2);
		Facility wf2 = ActivityWrapperFacility.toFacility(w2, facilities );

		List<? extends PlanElement> routedList2 = stopBasedDRTRoutingModule.calcRoute(hf2, wf2, 8 * 3600, p2);

		Person p3 = scenario.getPopulation().getPersons().get(Id.createPersonId(3));
		Activity h3 = (Activity) p3.getSelectedPlan().getPlanElements().get(0);
		Facility hf3 = ActivityWrapperFacility.toFacility(h3, facilities );

		Activity w3 = (Activity) p3.getSelectedPlan().getPlanElements().get(2);
		Facility wf3 = ActivityWrapperFacility.toFacility(w3, facilities );

		List<? extends PlanElement> routedList3 = stopBasedDRTRoutingModule.calcRoute(hf3, wf3, 8 * 3600, p3);


		Assert.assertEquals(5, routedList.size());
		Assert.assertEquals(5, routedList2.size());
		Assert.assertEquals(1, routedList3.size());

		System.out.println(routedList);

	}

	/**
	 * @return
	 */
	private Scenario createTestScenario() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.setMaxWalkDistance(200);
		drtConfigGroup.setTransitStopFile("./src/test/resources/cottbus/drtstops.xml.gz");
		config.addModule(drtConfigGroup);

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("./src/test/resources/cottbus/network.xml.gz");
		new TransitScheduleReader(scenario).readFile(drtConfigGroup.getTransitStopFile());
		createSomeAgents(scenario);
		return scenario;
	}

	/**
	 * @param scenario
	 */
	private void createSomeAgents(Scenario scenario) {
		PopulationFactory pf = scenario.getPopulation().getFactory();

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


		Person p3 = pf.createPerson(Id.createPersonId(3));
		Plan plan3 = pf.createPlan();
		p3.addPlan(plan3);
		Activity home3 = pf.createActivityFromCoord("home", new Coord(460077.7116017367, 5740133.3409971865));
		home3.setLinkId(Id.createLinkId(9541));
		home3.setEndTime(8 * 3600);
		plan3.addActivity(home3);
		plan3.addLeg(pf.createLeg("drt"));
		Activity work3 = pf.createActivityFromCoord("work", new Coord(460077.7116017367, 5740133.3409971865));
		work3.setLinkId(Id.createLinkId(7717));
		plan3.addActivity(work3);
		scenario.getPopulation().addPerson(p3);
	}

}
