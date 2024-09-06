/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.integration.invertednetworks;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;

/**
 * Creates a simple test network, properties:
 * <ul>
 * 	<li>The link (2)->(3) may have only one mode pt, while the rest also allows car.</li>
 * 	<li>A lane with turning move restrictions to Link (2)->(5) only can be attached to link (1)->(2).</li>
 * 	<li>A signal with turning move restrictions to Link only can be attached to link (1)->(2).</li>
 * </ul>
 *
 * <pre>
 *				(4)
 *    			^
 *    			|
 *				(3)<-(6)
 *    			^			^
 *    			|				|
 *				(2)->(5)
 *    			^
 *    			|
 *				(1)
 *				  |
 *				(0)
 * </pre>
 *
 * @author dgrether
 */
public class InvertedNetworkRoutingTestFixture {
	public final MutableScenario scenario;

	public InvertedNetworkRoutingTestFixture(boolean doCreateModes, boolean doCreateLanes, boolean doCreateSignals) {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.controller().setLastIteration(0);
		config.controller().setLinkToLinkRoutingEnabled(true);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		config.controller().setMobsim("qsim");
		config.global().setNumberOfThreads(1);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10000.0);
		config.qsim().setStartTime(0.0);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		stratSets.setWeight(1.0);
		config.replanning().addStrategySettings(stratSets);
		final double traveling = -1200.0;
		config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		ActivityParams params = new ActivityParams("home");
		params.setTypicalDuration(24.0 * 3600.0);
		config.scoring().addActivityParams(params);
		config.qsim().setUseLanes(doCreateLanes);

		this.scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		createNetwork();
		if (doCreateLanes){
			config.qsim().setUseLanes(true);
			createLanes();
		}
		if (doCreateModes){
			createModes();
		}
		createPopulation();
	}

	private void createLanes() {
		Lanes ld = scenario.getLanes();
		LanesFactory f = ld.getFactory();
		LanesToLinkAssignment l2l = f.createLanesToLinkAssignment(Id.create(12, Link.class));
		ld.addLanesToLinkAssignment(l2l);
		Lane l = f.createLane(Id.create(121, Lane.class));
		l.setStartsAtMeterFromLinkEnd(300);
		l.addToLaneId(Id.create(122, Lane.class));
		l2l.addLane(l);
		l = f.createLane(Id.create(122, Lane.class));
		l.setStartsAtMeterFromLinkEnd(150);
		l.addToLinkId(Id.create(25, Link.class));
		l2l.addLane(l);
	}

	private void createModes() {
		Network network = this.scenario.getNetwork();
		Set<String> ptOnly = new HashSet<String>();
		ptOnly.add(TransportMode.pt);
		Set<String> carPt = new HashSet<String>();
		carPt.add(TransportMode.car);
		carPt.add(TransportMode.pt);
		network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(carPt);
		network.getLinks().get(Id.create(23, Link.class)).setAllowedModes(ptOnly);
		network.getLinks().get(Id.create(34, Link.class)).setAllowedModes(carPt);
		network.getLinks().get(Id.create(25, Link.class)).setAllowedModes(carPt);
		network.getLinks().get(Id.create(56, Link.class)).setAllowedModes(carPt);
		network.getLinks().get(Id.create(63, Link.class)).setAllowedModes(carPt);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
		NetworkFactory f = network.getFactory();
		Node n;
		Link l;
		double y = -300;
		n = f.createNode(Id.create(0, Node.class), new Coord((double) 0, y));
		network.addNode(n);
		n = f.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		network.addNode(n);
		n = f.createNode(Id.create(2, Node.class), new Coord((double) 0, (double) 300));
		network.addNode(n);
		n = f.createNode(Id.create(3, Node.class), new Coord((double) 0, (double) 600));
		network.addNode(n);
		n = f.createNode(Id.create(4, Node.class), new Coord((double) 0, (double) 900));
		network.addNode(n);
		n = f.createNode(Id.create(5, Node.class), new Coord((double) 0, (double) 300));
		network.addNode(n);
		n = f.createNode(Id.create(6, Node.class), new Coord((double) 0, (double) 600));
		network.addNode(n);
		l = f.createLink(Id.create(1, Link.class), network.getNodes().get(Id.create(0, Node.class)), network.getNodes().get(Id.create(1, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(12, Link.class), network.getNodes().get(Id.create(1, Node.class)), network.getNodes().get(Id.create(2, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(23, Link.class), network.getNodes().get(Id.create(2, Node.class)), network.getNodes().get(Id.create(3, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(20.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(34, Link.class), network.getNodes().get(Id.create(3, Node.class)), network.getNodes().get(Id.create(4, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(25, Link.class), network.getNodes().get(Id.create(2, Node.class)), network.getNodes().get(Id.create(5, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(56, Link.class), network.getNodes().get(Id.create(5, Node.class)), network.getNodes().get(Id.create(6, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
		l = f.createLink(Id.create(63, Link.class), network.getNodes().get(Id.create(6, Node.class)), network.getNodes().get(Id.create(3, Node.class)));
		l.setLength(300.0);
		l.setFreespeed(10.0);
		l.setCapacity(3600.0);
		network.addLink(l);
	}

	private void createPopulation() {
		Population pop = this.scenario.getPopulation();
		PopulationFactory f = pop.getFactory();
		Person p = f.createPerson(Id.create(1, Person.class));
		pop.addPerson(p);
		Plan plan = f.createPlan();
		p.addPlan(plan);
		Activity act = f.createActivityFromLinkId("home", Id.create(1, Link.class));
		act.setEndTime(2000.0);
		plan.addActivity(act);
		Leg leg = f.createLeg(TransportMode.car);
		plan.addLeg(leg);
		act = f.createActivityFromLinkId("home", Id.create(34, Link.class));
		plan.addActivity(act);
	}


}
