/* *********************************************************************** *
 * project: org.matsim.*
 * ExperiencedPlansWriterTest.java
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

package org.matsim.withinday.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;

import jakarta.inject.Inject;
import java.io.File;
import java.util.*;

/**
 * @author cdobler
 */
public class ExperiencedPlansWriterTest {

private static final Logger log = LogManager.getLogger(ExperiencedPlansWriterTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testWriteFile() {

		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(this.utils.getOutputDirectory());

		config.qsim().setEndTime(24 * 3600);

		config.controller().setLastIteration(0);
		config.controller().setRoutingAlgorithmType( ControllerConfigGroup.RoutingAlgorithmType.Dijkstra );

		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.scoring().addActivityParams(homeParams);

		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		Population population = scenario.getPopulation();
		population.addPerson(createPerson(scenario, "p01"));
		population.addPerson(createPerson(scenario, "p02"));

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.getConfig().controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new WithinDayModule());
				addControlerListenerBinding().to(WriterInitializer.class);
				addControlerListenerBinding().to(ExecutedPlansServiceImpl.class);
				// only for debugging
				addEventHandlerBinding().toInstance(new EventsPrinter());
			}
		});
		controler.run();

		/*
		 * After running the scenario, load experienced plans file and check whether the
		 * routes match what we expect (route 01 unchanged, route 02 adapted).
		 */
		File file = new File(this.utils.getOutputDirectory() + "/ITERS/it.0/0." +
				ExecutedPlansServiceImpl.EXECUTEDPLANSFILE);
		Assertions.assertTrue(file.exists());

		Config experiencedConfig = ConfigUtils.createConfig();
		experiencedConfig.plans().setInputFile(this.utils.getOutputDirectory() + "/ITERS/it.0/0." +
				ExecutedPlansServiceImpl.EXECUTEDPLANSFILE);

		Scenario experiencedScenario = ScenarioUtils.loadScenario(experiencedConfig);

		Person p01 = experiencedScenario.getPopulation().getPersons().get(Id.create("p01", Person.class));
		Person p02 = experiencedScenario.getPopulation().getPersons().get(Id.create("p02", Person.class));

		Leg leg01 = (Leg) p01.getSelectedPlan().getPlanElements().get(1);
		Leg leg02 = (Leg) p02.getSelectedPlan().getPlanElements().get(1);

		// expect leg from p01 to be unchanged
		Assertions.assertEquals(1, ((NetworkRoute) leg01.getRoute()).getLinkIds().size());

		// expect leg from p02 to be adapted
		Assertions.assertEquals(3, ((NetworkRoute) leg02.getRoute()).getLinkIds().size());
	}

	private static class WriterInitializer implements StartupListener {

		@Inject private Scenario scenario;
		@Inject private ActivityReplanningMap activityReplanningMap;
		@Inject private WithinDayEngine withinDayEngine;

		@Override
		public void notifyStartup(StartupEvent event) {

			/*
			 * Initialize within-day stuff to adapt the second person's route.
			 */
			ActivityEndIdentifierFactory identifierFactory = new ActivityEndIdentifierFactory(activityReplanningMap);
			identifierFactory.addAgentFilterFactory(new FilterFactory());
			ReplannerFactory replannerFactory = new ReplannerFactory(scenario, this.withinDayEngine);
			replannerFactory.addIdentifier(identifierFactory.createIdentifier());
			this.withinDayEngine.addDuringActivityReplannerFactory(replannerFactory);
		}

	}

	private static class Filter implements AgentFilter {

		private final Id<Person> id = Id.create("p02", Person.class);

		// Agents that do not match the filter criteria are removed from the set.
		@Override
		public void applyAgentFilter(Set<Id<Person>> set, double time) {
			Iterator<Id<Person>> iter = set.iterator();
			while (iter.hasNext()) {
				Id<Person> id = iter.next();
				if (!this.applyAgentFilter(id, time)) iter.remove();
			}
		}

		// Returns true if the agent matches the filter criteria, otherwise returns false.
		@Override
		public boolean applyAgentFilter(Id<Person> id, double time) {
			if (id.equals(this.id)) return true;
			return false;
		}
	}

	private static class FilterFactory implements AgentFilterFactory {

		@Override
		public AgentFilter createAgentFilter() {
			return new Filter();
		}
	}

	/*
	 * Replace agent's route "l0 - l1 - l2" with "l0 - l3 - l4 - l5 - l2".
	 */
	private static class Replanner extends WithinDayDuringActivityReplanner {

		public Replanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface) {
			super(id, scenario, internalInterface);
		}

		@Override
		public boolean doReplanning(MobsimAgent withinDayAgent) {

			log.info("Replanning agent " + withinDayAgent.getId());

 			Plan plan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

			Leg leg = (Leg) plan.getPlanElements().get(1);
			NetworkRoute route = (NetworkRoute) leg.getRoute();

			Id<Link> startLinkId = Id.create("l0", Link.class);
			Id<Link> endLinkId = Id.create("l2", Link.class);
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			linkIds.add(Id.create("l3", Link.class));
			linkIds.add(Id.create("l4", Link.class));
			linkIds.add(Id.create("l5", Link.class));
			route.setLinkIds(startLinkId, linkIds, endLinkId);

			return true;
		}
	}

	private static class ReplannerFactory extends WithinDayDuringActivityReplannerFactory {

		private final Scenario scenario;

		public ReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine) {
			super(withinDayEngine);
			this.scenario = scenario;
		}

		@Override
		public WithinDayDuringActivityReplanner createReplanner() {
			Id<WithinDayReplanner> id = super.getId();
			WithinDayDuringActivityReplanner replanner = new Replanner(id, scenario,
					this.getWithinDayEngine().getActivityRescheduler());
			return replanner;
		}
	}

	/*
	 * Network looks like:
	 *          l4
	 *       n4----n5
	 *     l3|     |l5
	 * n0----n1----n2----n3
	 *    l0    l1    l2
	 */
	private void createNetwork(Scenario scenario) {

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node node0 = networkFactory.createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node node1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord(1.0, 0.0));
		Node node2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord(2.0, 0.0));
		Node node3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord(3.0, 0.0));
		Node node4 = networkFactory.createNode(Id.create("n4", Node.class), new Coord(1.0, 1.0));
		Node node5 = networkFactory.createNode(Id.create("n5", Node.class), new Coord(2.0, 1.0));

		Link link0 = networkFactory.createLink(Id.create("l0", Link.class), node0, node1);
		Link link1 = networkFactory.createLink(Id.create("l1", Link.class), node1, node2);
		Link link2 = networkFactory.createLink(Id.create("l2", Link.class), node2, node3);
		Link link3 = networkFactory.createLink(Id.create("l3", Link.class), node1, node4);
		Link link4 = networkFactory.createLink(Id.create("l4", Link.class), node4, node5);
		Link link5 = networkFactory.createLink(Id.create("l5", Link.class), node5, node2);

		link0.setLength(1000.0);
		link1.setLength(1000.0);
		link2.setLength(1000.0);
		link3.setLength(1000.0);
		link4.setLength(1000.0);
		link5.setLength(1000.0);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addLink(link0);
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
	}

	/*
	 * Create a person with default route l0 - l1 - l2
	 */
	private Person createPerson(Scenario scenario, String id) {

		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(id, Person.class));

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l0", Link.class));
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l2", Link.class));

		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);

		RouteFactory routeFactory = new LinkNetworkRouteFactory();
		Id<Link> startLinkId = Id.create("l0", Link.class);
		Id<Link> endLinkId = Id.create("l2", Link.class);
		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(startLinkId, endLinkId);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(Id.create("l1", Link.class));
		route.setLinkIds(startLinkId, linkIds, endLinkId);
		leg.setRoute(route);

		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);

		person.addPlan(plan);

		return person;
	}

	// for debugging
	private static class EventsPrinter implements BasicEventHandler {

		@Override
		public void reset(final int iter) {
		}

		@Override
		public void handleEvent(final Event event) {
			StringBuilder eventXML = new StringBuilder(180);
			eventXML.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(entry.getValue());
				eventXML.append("\" ");
			}
			eventXML.append("/>");

			log.info(eventXML.toString());
		}
	}
}
