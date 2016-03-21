/****************************************************************************/
// SUMO, Simulation of Urban MObility; see http://sumo.dlr.de/
// Copyright (C) 2001-2016 DLR (http://www.dlr.de/) and contributors
/****************************************************************************/
//
//   This file is part of SUMO.
//   SUMO is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
/****************************************************************************/

package org.matsim.contrib.hybridsim.run;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.hybridsim.simulation.HybridMobsimProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by laemmel on 09.03.16.
 */
public class Example {

	private static final Logger log = Logger.getLogger(Example.class);

	public static void main(String[] args) throws IOException, InterruptedException {


		Config c = ConfigUtils.createConfig();
		c.controler().setLastIteration(0);
		c.controler().setWriteEventsInterval(1);




		final Scenario sc = ScenarioUtils.createScenario(c);
		enrichConfig(c);
		createNetwork(sc);
		createPopulation(sc);

		final Controler controller = new Controler(sc);
		controller.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		final EventsManager eventsManager = EventsUtils.createEventsManager();

		Injector mobsimProviderInjector = Guice.createInjector(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(Scenario.class).toInstance(sc);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(HybridNetworkFactory.class).toInstance(new HybridNetworkFactory());
				bind(QNetworkFactory.class).to(HybridNetworkFactory.class); ;
			}

		});
		final Provider<Mobsim> mobsimProvider = mobsimProviderInjector.getInstance(HybridMobsimProvider.class);
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(mobsimProvider);
				bindEventsManager().toInstance(eventsManager);
			}
		});
		controller.run();


	}

	private static void enrichConfig(Config c) {
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
		c.planCalcScore().addActivityParams(pre);
		c.planCalcScore().addActivityParams(post);

		c.planCalcScore().setLateArrival_utils_hr(0.);
		c.planCalcScore().setPerforming_utils_hr(0.);
	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		PopulationFactory fac = pop.getFactory();
		for (int i = 0; i < 20; i++) {
			Person pers = fac.createPerson(Id.createPersonId(i));
			pop.addPerson(pers);
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity a0 = fac.createActivityFromLinkId("origin",Id.createLinkId(0));
			a0.setEndTime(i);
			plan.addActivity(a0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity a1 = fac.createActivityFromLinkId("destination",Id.createLinkId(3));
			plan.addActivity(a1);
		}
		for (int i = 20; i < 40; i++) {
			Person pers = fac.createPerson(Id.createPersonId(i));
			pop.addPerson(pers);
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity a0 = fac.createActivityFromLinkId("origin",Id.createLinkId("3r"));
			a0.setEndTime(i-20);
			plan.addActivity(a0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity a1 = fac.createActivityFromLinkId("destination",Id.createLinkId("0r"));
			plan.addActivity(a1);
		}
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveLaneWidth(0.71);
		((NetworkImpl)net).setEffectiveCellSize(0.26);
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.createNodeId(0), CoordUtils.createCoord(93.85, 110.1));
		net.addNode(n0);
		Node n1 = fac.createNode(Id.createNodeId(1), CoordUtils.createCoord(103.85, 110.1));
		net.addNode(n1);
		Node n2 = fac.createNode(Id.createNodeId(2), CoordUtils.createCoord(113.85, 110.1));
		net.addNode(n2);
		Node n3 = fac.createNode(Id.createNodeId(3), CoordUtils.createCoord(200, 110.1));
		net.addNode(n3);
		Node n4 = fac.createNode(Id.createNodeId(4), CoordUtils.createCoord(210, 110.1));
		net.addNode(n4);
		Node n5 = fac.createNode(Id.createNodeId(5), CoordUtils.createCoord(220, 110.1));
		net.addNode(n5);
		Link l0 = fac.createLink(Id.createLinkId(0), n0, n1);
		net.addLink(l0);
		Link l1 = fac.createLink(Id.createLinkId(1), n1, n2);
		net.addLink(l1);
		Link lJPS0 = fac.createLink(Id.createLinkId("JPS0"), n2, n3);
		net.addLink(lJPS0);
		Link l2 = fac.createLink(Id.createLinkId("2"), n3, n4);
		net.addLink(l2);
		Link l3 = fac.createLink(Id.createLinkId("3"), n4, n5);
		net.addLink(l3);
		Link l3r = fac.createLink(Id.createLinkId("3r"), n5, n4);
		net.addLink(l3r);
		Link l2r = fac.createLink(Id.createLinkId("2r"), n4, n3);
		net.addLink(l2r);
		Link lJPS0r = fac.createLink(Id.createLinkId("JPS0r"), n3, n2);
		net.addLink(lJPS0r);
		Link l1r = fac.createLink(Id.createLinkId("1r"), n2, n1);
		net.addLink(l1r);
		Link l0r = fac.createLink(Id.createLinkId("0r"), n1, n0);
		net.addLink(l0r);

		for (Link l : net.getLinks().values()) {
			l.setFreespeed(1.34);
			l.setCapacity(1.8886);
			l.setNumberOfLanes(1.12);
			l.setLength(CoordUtils.calcEuclideanDistance(l.getFromNode().getCoord(),l.getToNode().getCoord()));
		}
		Set<String> ext = new HashSet<>();
		ext.add("car"); ext.add("ext"); ext.add("2ext");
		lJPS0.setAllowedModes(ext); lJPS0r.setAllowedModes(ext);
		Set<String> ext2 = new HashSet<>();
		ext2.add("car"); ext2.add("ext2");
		l1r.setAllowedModes(ext2);
		l2.setAllowedModes(ext2);



	}
}
