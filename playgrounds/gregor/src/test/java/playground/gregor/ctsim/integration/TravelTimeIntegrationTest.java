package playground.gregor.ctsim.integration;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestCase;
import playground.gregor.ctsim.simulation.CTMobsimFactory;
import playground.gregor.ctsim.simulation.CTTripRouterFactory;
import playground.gregor.utils.Variance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by laemmel on 15/10/15.
 */
public class TravelTimeIntegrationTest extends MatsimTestCase {
	private static final Logger log = Logger.getLogger(TravelTimeIntegrationTest.class);


	@Test
	public void testTravelTimeOverMultipleLinks() {

		//This test compares the average travel times for a 80m long walk of 1000 agents of:
		//scenario 1: via 8 10m links
		//scenario 2: via 1 80m links
		//While travel times will obviously differ because of the model's stochastic nature and the difference in the number
		//of cells that have to be traversed (additional nodes imply more cells), it should be in the same order of magnitude.
		//Moreover, the travel time in scenario 1 MUST be longer than travel time in scenario 2


		Config c1 = ConfigUtils.createConfig();
		Scenario sc1 = ScenarioUtils.createScenario(c1);
		createSc1(sc1);
		createBigPop(sc1);
		setupConfig(sc1);
		c1.controler().setOutputDirectory(getOutputDirectory() + "/sc1/");
		AverageTravelTime ttObserver1 = new AverageTravelTime();
		runScenario(sc1, ttObserver1);
		double tt1 = ttObserver1.ttVariance.getMean();
		double var1 = ttObserver1.ttVariance.getVar();


		Config c2 = ConfigUtils.createConfig();
		Scenario sc2 = ScenarioUtils.createScenario(c2);
		createSc2(sc2);
		createBigPop(sc2);
		setupConfig(sc2);
		c2.controler().setOutputDirectory(getOutputDirectory() + "/sc2/");
		AverageTravelTime ttObserver2 = new AverageTravelTime();
		runScenario(sc2, ttObserver2);
		double tt2 = ttObserver2.ttVariance.getMean();
		double var2 = ttObserver2.ttVariance.getVar();

//		//est additional dist
		int nrNodes = 7;
		final double nodeLength = Math.sqrt(3) * Math.sqrt(64 / (1.5 * Math.sqrt(3))) / 2;
		double additionalLength = nrNodes * nodeLength;
		double totalLength = 80. + additionalLength;
		double coeff = totalLength / 80.;
		double expTT = tt2 * coeff;
		double mxDev = Math.abs(expTT - tt2);

		assertEquals("similar travel times", tt1, tt2, mxDev);

		boolean tt1Bigger = tt1 > tt2;
		assertTrue("longer avg travel time in scenario 1", tt1Bigger);

	}

	private void runScenario(Scenario sc, TTObserver ttObserver) {
		final Controler controller = new Controler(sc);

//		//DEBUG
//		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
//
//
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
//		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
//		InfoBox iBox = new InfoBox(dbg, sc);
//		dbg.addAdditionalDrawer(iBox);
//		//		dbg.addAdditionalDrawer(new Branding());
////			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
////			dbg.addAdditionalDrawer(qDbg);
//
//		EventsManager em = controller.getEvents();
////			em.addHandler(qDbg);
//		em.addHandler(dbg);
//		//END_DEBUG


		controller.getEvents().addHandler(ttObserver);
		LeastCostPathCalculatorFactory cost = createDefaultLeastCostPathCalculatorFactory(sc);
		CTTripRouterFactory tripRouter = new CTTripRouterFactory(sc, cost);

		controller.setTripRouterFactory(tripRouter);


		final CTMobsimFactory factory = new CTMobsimFactory();

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals("ctsim")) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return factory.createMobsim(controller.getScenario(), controller.getEvents());
						}
					});
				}
			}
		});

		controller.run();
	}

	private static LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(
			Scenario scenario) {
		Config config = scenario.getConfig();
		if (config.controler().getRoutingAlgorithmType()
				.equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
			return new DijkstraFactory();
		}
		else {
			if (config
					.controler()
					.getRoutingAlgorithmType()
					.equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
				return new AStarLandmarksFactory(
						scenario.getNetwork(),
						new FreespeedTravelTimeAndDisutility(config.planCalcScore()),
						config.global().getNumberOfThreads());
			}
			else {
				if (config.controler().getRoutingAlgorithmType()
						.equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
					return new FastDijkstraFactory();
				}
				else {
					if (config
							.controler()
							.getRoutingAlgorithmType()
							.equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
						return new FastAStarLandmarksFactory(
								scenario.getNetwork(),
								new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
					}
					else {
						throw new IllegalStateException(
								"Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
					}
				}
			}
		}
	}

	private void createSc1(Scenario sc1) {
		Network net1 = sc1.getNetwork();
		NetworkFactory fac1 = net1.getFactory();
		Node n1_0 = fac1.createNode(Id.createNodeId("n1_0"), CoordUtils.createCoord(0, 0));
		Node n1_1 = fac1.createNode(Id.createNodeId("n1_1"), CoordUtils.createCoord(10, 0));
		Node n1_2 = fac1.createNode(Id.createNodeId("n1_2"), CoordUtils.createCoord(20, 0));
		Node n1_3 = fac1.createNode(Id.createNodeId("n1_3"), CoordUtils.createCoord(30, 0));
		Node n1_4 = fac1.createNode(Id.createNodeId("n1_4"), CoordUtils.createCoord(40, 0));
		Node n1_5 = fac1.createNode(Id.createNodeId("n1_5"), CoordUtils.createCoord(50, 0));
		Node n1_6 = fac1.createNode(Id.createNodeId("n1_6"), CoordUtils.createCoord(60, 0));
		Node n1_7 = fac1.createNode(Id.createNodeId("n1_7"), CoordUtils.createCoord(70, 0));
		Node n1_8 = fac1.createNode(Id.createNodeId("n1_8"), CoordUtils.createCoord(80, 0));
		Node n1_9 = fac1.createNode(Id.createNodeId("n1_9"), CoordUtils.createCoord(90, 0));
		net1.addNode(n1_0);
		net1.addNode(n1_1);
		net1.addNode(n1_2);
		net1.addNode(n1_3);
		net1.addNode(n1_4);
		net1.addNode(n1_5);
		net1.addNode(n1_6);
		net1.addNode(n1_7);
		net1.addNode(n1_8);
		net1.addNode(n1_9);
		Link l1_0 = fac1.createLink(Id.createLinkId("l1_0"), n1_0, n1_1);
		Link l1_1 = fac1.createLink(Id.createLinkId("l1_1"), n1_1, n1_2);
		Link l1_2 = fac1.createLink(Id.createLinkId("l1_2"), n1_2, n1_3);
		Link l1_3 = fac1.createLink(Id.createLinkId("l1_3"), n1_3, n1_4);
		Link l1_4 = fac1.createLink(Id.createLinkId("l1_4"), n1_4, n1_5);
		Link l1_5 = fac1.createLink(Id.createLinkId("l1_5"), n1_5, n1_6);
		Link l1_6 = fac1.createLink(Id.createLinkId("l1_6"), n1_6, n1_7);
		Link l1_7 = fac1.createLink(Id.createLinkId("l1_7"), n1_7, n1_8);
		Link l1_8 = fac1.createLink(Id.createLinkId("l1_8"), n1_8, n1_9);
		net1.addLink(l1_0);
		net1.addLink(l1_1);
		net1.addLink(l1_2);
		net1.addLink(l1_3);
		net1.addLink(l1_4);
		net1.addLink(l1_5);
		net1.addLink(l1_6);
		net1.addLink(l1_7);
		net1.addLink(l1_8);
		Set<String> modes = new HashSet<>();
		modes.add("walkct");
		for (Link l : net1.getLinks().values()) {
			l.setAllowedModes(modes);
			l.setFreespeed(20);
			l.setLength(10.);
			l.setCapacity(8. * 1.33);
		}


	}

	private void createSc2(Scenario sc1) {
		Network net1 = sc1.getNetwork();
		NetworkFactory fac1 = net1.getFactory();
		Node n1_0 = fac1.createNode(Id.createNodeId("n1_0"), CoordUtils.createCoord(0, 0));
		Node n1_1 = fac1.createNode(Id.createNodeId("n1_1"), CoordUtils.createCoord(10, 0));
		Node n1_9 = fac1.createNode(Id.createNodeId("n1_9"), CoordUtils.createCoord(90, 0));
		net1.addNode(n1_0);
		net1.addNode(n1_1);
		net1.addNode(n1_9);
		Link l1_0 = fac1.createLink(Id.createLinkId("l1_0"), n1_0, n1_1);
		Link l1_8 = fac1.createLink(Id.createLinkId("l1_8"), n1_1, n1_9);
		net1.addLink(l1_0);
		net1.addLink(l1_8);
		Set<String> modes = new HashSet<>();
		modes.add("walkct");
		for (Link l : net1.getLinks().values()) {
			l.setAllowedModes(modes);
			l.setFreespeed(20);
			l.setLength(10.);
			l.setCapacity(8. * 1.33);
		}
		l1_8.setLength(80.);

	}

	private void createBigPop(Scenario sc1) {
		Population pop1 = sc1.getPopulation();
		PopulationFactory popFac1 = pop1.getFactory();


		for (int i = 0; i < 1000; i++) {
			Person pers = popFac1.createPerson(Id.create(i, Person.class));
			Plan plan = popFac1.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = popFac1.createActivityFromLinkId("origin",
					Id.create("l1_0", Link.class));
			act0.setEndTime(0);
			plan.addActivity(act0);
			Leg leg = popFac1.createLeg("walkct");
			plan.addLeg(leg);
			Activity act1 = popFac1.createActivityFromLinkId("destination",
					Id.create("l1_8", Link.class));
			plan.addActivity(act1);
			pop1.addPerson(pers);
		}

	}

	public void setupConfig(Scenario sc) {
		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(1);
		Config c = sc.getConfig();

		c.strategy().addParam("Module_1", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_1", ".5");

		c.controler().setMobsim("ctsim");

		c.controler().setLastIteration(0);

		PlanCalcScoreConfigGroup.ActivityParams pre = new PlanCalcScoreConfigGroup.ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
		// running a simulation one gets
		// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		PlanCalcScoreConfigGroup.ActivityParams post = new PlanCalcScoreConfigGroup.ActivityParams("destination");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);

		QSimConfigGroup qsim = sc.getConfig().qsim();
		// qsim.setEndTime(20 * 60);
		c.controler().setMobsim("ctsim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(30 * 3600);
	}

	@Test
	public void testFreespeedTravelTime() {

		//1000 pedestrians walk 80 m; one pedestrian depart every 10s --> free speed scenario
		//free speed = 1.5 m/s --> average travel time must be around 53.333s


		Config c2 = ConfigUtils.createConfig();
		Scenario sc2 = ScenarioUtils.createScenario(c2);
		createSc2(sc2);
		createBigPopWithSpreadedDeparutreTimes(sc2);
		setupConfig(sc2);
		c2.controler().setOutputDirectory(getOutputDirectory() + "/sc2/");
//		AverageTravelTime ttObserver2 = new AverageTravelTime();
		AverageLinkTravelTime ttObserver2 = new AverageLinkTravelTime(Id.createLinkId("l1_8"));
		runScenario(sc2, ttObserver2);
		double tt2 = ttObserver2.ttVariance.getMean();
		double var2 = ttObserver2.ttVariance.getVar();


		assertEquals("correct avg travel time", 53.333, tt2, 0.5);

	}

	private void createBigPopWithSpreadedDeparutreTimes(Scenario sc1) {
		Population pop1 = sc1.getPopulation();
		PopulationFactory popFac1 = pop1.getFactory();


		for (int i = 0; i < 1000; i++) {
			Person pers = popFac1.createPerson(Id.create(i, Person.class));
			Plan plan = popFac1.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = popFac1.createActivityFromLinkId("origin",
					Id.create("l1_0", Link.class));
			act0.setEndTime(10 * i);
			plan.addActivity(act0);
			Leg leg = popFac1.createLeg("walkct");
			plan.addLeg(leg);
			Activity act1 = popFac1.createActivityFromLinkId("destination",
					Id.create("l1_8", Link.class));
			plan.addActivity(act1);
			pop1.addPerson(pers);
		}

	}

	private interface TTObserver extends EventHandler {

	}

	private class AverageLinkTravelTime implements LinkEnterEventHandler, LinkLeaveEventHandler, TTObserver {
		private final Id<Link> linkId;
		private Map<Id<Person>, LinkEnterEvent> departures = new HashMap<>();

		private Variance ttVariance = new Variance();

		public AverageLinkTravelTime(Id<Link> linkId) {
			this.linkId = linkId;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId() == this.linkId) {
				this.departures.put(event.getDriverId(), event);
			}

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (event.getLinkId() == this.linkId) {
				LinkEnterEvent e = departures.remove(event.getDriverId());
				double tt = event.getTime() - e.getTime();
				ttVariance.addVar(tt);
			}
		}

		@Override
		public void reset(int iteration) {

		}
	}

	private class AverageTravelTime implements PersonDepartureEventHandler, PersonArrivalEventHandler, TTObserver {

		private Map<Id<Person>, PersonDepartureEvent> departures = new HashMap<>();

		private Variance ttVariance = new Variance();

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			PersonDepartureEvent e = departures.remove(event.getPersonId());
			double tt = event.getTime() - e.getTime();
			ttVariance.addVar(tt);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			departures.put(event.getPersonId(), event);
		}

		@Override
		public void reset(int iteration) {

		}
	}

}
