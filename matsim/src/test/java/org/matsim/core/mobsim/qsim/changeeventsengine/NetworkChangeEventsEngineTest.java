
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsEngineTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

	/**
 * @author mrieser / Simunto GmbH
 */
public class NetworkChangeEventsEngineTest {

	 @Test
	 void testActivation_inactive() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		NetworkFactory nf = scenario.getNetwork().getFactory();
		Node node1 = nf.createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = nf.createNode(Id.create(2, Node.class), new Coord(100, 100));
		Link link1 = scenario.getNetwork().getFactory().createLink(Id.create(1, Link.class), node1, node2);
		link1.setFreespeed(20);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link1);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		QSim qsim = new QSimBuilder(config).useDefaults().build(scenario, eventsManager);

		NetworkChangeEventsEngine engine = new NetworkChangeEventsEngine(scenario.getNetwork(), new MessageQueue());
		qsim.addMobsimEngine(engine);

		engine.onPrepareSim();
		for (int i = 0; i < 30; i++) {
			engine.doSimStep(i);
		}
		NetworkChangeEvent changeEvent = new NetworkChangeEvent(37);
		changeEvent.addLink(link1);
		changeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 50));

		try {
			engine.addNetworkChangeEvent(changeEvent);
			Assertions.fail("Expected exception due to links not being time dependent, but got none.");
		} catch (Exception expected) {
		}
	}

	 @Test
	 void testActivation_timedepOnly_freespeed() {
		Config config = ConfigUtils.createConfig();
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.createScenario(config);

		NetworkFactory nf = scenario.getNetwork().getFactory();
		Node node1 = nf.createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = nf.createNode(Id.create(2, Node.class), new Coord(100, 100));
		Link link1 = scenario.getNetwork().getFactory().createLink(Id.create(1, Link.class), node1, node2);
		link1.setFreespeed(20);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link1);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		QSim qsim = new QSimBuilder(config).useDefaults().build(scenario, eventsManager);

		NetworkChangeEventsEngine engine = new NetworkChangeEventsEngine(scenario.getNetwork(), new MessageQueue());
		engine.setInternalInterface(new DummyInternalInterfaceImpl(qsim));

		engine.onPrepareSim();
		for (int i = 0; i < 30; i++) {
			engine.doSimStep(i);
		}
		NetworkChangeEvent changeEvent = new NetworkChangeEvent(37);
		changeEvent.addLink(link1);
		changeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 50));
		engine.addNetworkChangeEvent(changeEvent);
		Assertions.assertEquals(20, link1.getFreespeed(30), 0, "it should still be 20 now.");
		for (int i = 30; i < 40; i++) {
			engine.doSimStep(i);
		}
		Assertions.assertEquals(50, link1.getFreespeed(40), 0, "it should be 50 now.");
	}

	 @Test
	 void testActivation_timedepOnly_capacity() {
		Config config = ConfigUtils.createConfig();
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.createScenario(config);

		NetworkFactory nf = scenario.getNetwork().getFactory();
		Node node1 = nf.createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = nf.createNode(Id.create(2, Node.class), new Coord(100, 100));
		Link link1 = scenario.getNetwork().getFactory().createLink(Id.create(1, Link.class), node1, node2);
		link1.setCapacity(20);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link1);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		QSim qsim = new QSimBuilder(config).useDefaults().build(scenario, eventsManager);

		NetworkChangeEventsEngine engine = new NetworkChangeEventsEngine(scenario.getNetwork(), new MessageQueue());
		engine.setInternalInterface(new DummyInternalInterfaceImpl(qsim));

		engine.onPrepareSim();
		for (int i = 0; i < 30; i++) {
			engine.doSimStep(i);
		}
		NetworkChangeEvent changeEvent = new NetworkChangeEvent(37);
		changeEvent.addLink(link1);
		changeEvent.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, 2));
		engine.addNetworkChangeEvent(changeEvent);
		Assertions.assertEquals(20, link1.getCapacity(30), 0, "it should still be 20 now.");
		for (int i = 30; i < 40; i++) {
			engine.doSimStep(i);
		}
		Assertions.assertEquals(40, link1.getCapacity(40), 0, "it should be 40 now.");
	}

	private static class DummyInternalInterfaceImpl implements InternalInterface {

		private final QSim qsim;

		DummyInternalInterfaceImpl(QSim qsim) {
			this.qsim = qsim;
		}

		@Override
		public QSim getMobsim() {
			return this.qsim;
		}

		@Override
		public void arrangeNextAgentState(MobsimAgent agent) {
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent agent) {
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			return null;
		}

		@Override
		public List<DepartureHandler> getDepartureHandlers(){
			throw new RuntimeException( "not implemented" );
		}

	}

}
