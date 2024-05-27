/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.core.mobsim.hermes;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class HermesRoundaboutTest {

	public static final Coord D_START = new Coord(-60, 0);
	public static final Coord C_START = new Coord(0, 60);
	public static final Coord B_START = new Coord(60, 0);
	public static final Coord A_START = new Coord(0, -60);


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testRoundaboutBehavior(){
		ScenarioImporter.flush();
		final Config config = createConfig();
		config.controller().setMobsim("hermes");
		config.eventsManager().setOneThreadPerHandler(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		buildRoundaboutNetwork(scenario);
		buildPopulation(scenario);
		final int[] eventsCount = new int[2];
		Controler controler = new Controler(scenario);
		controler.getEvents().addHandler((PersonArrivalEventHandler) event -> eventsCount[0]++);
		controler.getEvents().addHandler((LinkLeaveEventHandler) event -> eventsCount[1]++);
		controler.run();
		//400 agents with 3 legs each (incl. access/egress) in 3 iterations
		Assert.equals(3 * 400 * 3, eventsCount[0]);
		//400 agents each traveling on 7 links in 3 iterations
		Assert.equals(7 * 400 * 3, eventsCount[1]);
	}

	private Config createConfig() {

		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(2);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink); //standard accessEgressMode is walk

		final ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
		homeParams.setTypicalDuration(1);
		config.scoring().addActivityParams(homeParams);

		final ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work");
		workParams.setTypicalDuration(1);
		config.scoring().addActivityParams(workParams);

		ReplanningConfigGroup.StrategySettings replanning = new ReplanningConfigGroup.StrategySettings();
		replanning.setStrategyName("ReRoute");
		replanning.setWeight(1.0);
		config.replanning().addStrategySettings(replanning);

		return config;
	}

	private void buildPopulation(Scenario scenario) {
		List<Tuple<Coord, Coord>> startEndRelations = List.of(new Tuple<>(A_START, D_START), new Tuple<>(B_START, A_START), new Tuple<>(C_START, B_START), new Tuple<>(D_START, C_START));

		VehicleType av = VehicleUtils.createVehicleType(Id.create("av", VehicleType.class));
		av.setFlowEfficiencyFactor(2.0);
		av.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(av);
		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		scenario.getVehicles().addVehicleType(car);

		final PopulationFactory factory = scenario.getPopulation().getFactory();
		int a = 0;
		for (var startEndRelation : startEndRelations) {
			for (int i = 0; i < 100; i++) {
				Person p = factory.createPerson(Id.createPersonId(a + "_" + i));
				scenario.getPopulation().addPerson(p);
				Plan plan = factory.createPlan();
				p.addPlan(plan);
				Activity h = factory.createActivityFromCoord("home", startEndRelation.getFirst());
				h.setEndTime(8 * 3600 + i);
				Leg l = factory.createLeg(TransportMode.car);
				Activity w = factory.createActivityFromCoord("work", startEndRelation.getSecond());
				plan.addActivity(h);
				plan.addLeg(l);
				plan.addActivity(w);
				Vehicle vehicle;
				if (a == 3) {
					// a single branch gets a super flowy AV
					vehicle = VehicleUtils.createVehicle(Id.createVehicleId(p.getId()), av);

				} else {
					vehicle = VehicleUtils.createVehicle(Id.createVehicleId(p.getId()), car);
				}
				scenario.getVehicles().addVehicle(vehicle);
				VehicleUtils.insertVehicleIdsIntoAttributes(p, Map.of(TransportMode.car, vehicle.getId()));
			}
			a++;
		}
	}

	private void buildRoundaboutNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		//the roundabout
		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"),new Coord(0,-20));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"),new Coord(20,0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"),new Coord(0,20));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("d"),new Coord(-20,0));
		Link ab = NetworkUtils.createAndAddLink(network,Id.createLinkId("a_b"),a,b,32,8,1100,1);
		Link bc = NetworkUtils.createAndAddLink(network,Id.createLinkId("b_c"),b,c,32,8,1100,1);
		Link cd = NetworkUtils.createAndAddLink(network,Id.createLinkId("c_d"),c,d,32,8,1100,1);
		Link da = NetworkUtils.createAndAddLink(network,Id.createLinkId("d_a"),d,a,32,8,1100,1);
		// in / outlinks
		Node a1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("a1"),new Coord(0,-40));
		Node a2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("a2"), A_START);
		Link a1a =  NetworkUtils.createAndAddLink(network,Id.createLinkId("a1_a"),a1,a,20,8,1100,1);
		Link aa1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("a_a1"),a,a1,20,8,1100,1);
		Link a1a2 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("a1_a2"),a1,a2,2000,8,720000,1);
		Link a2a1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("a2_a1"),a2,a1,2000,8,720000,1);

		Node b1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("b1"),new Coord(40,0));
		Node b2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("b2"), B_START);
		Link b1b =  NetworkUtils.createAndAddLink(network,Id.createLinkId("b1_b"),b1,b,20,8,1100,1);
		Link bb1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("b_b1"),b,b1,20,8,1100,1);
		Link b1b2 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("b1_b2"),b1,b2,2000,8,720000,1);
		Link b2b1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("b2_b1"),b2,b1,2000,8,720000,1);

		Node c1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("c1"),new Coord(0,40));
		Node c2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("c2"), C_START);
		Link c1c =  NetworkUtils.createAndAddLink(network,Id.createLinkId("c1_c"),c1,c,20,8,1100,1);
		Link cc1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("c_c1"),c,c1,20,8,1100,1);
		Link c1c2 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("c1_c2"),c1,c2,2000,8,720000,1);
		Link c2c1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("c2_c1"),c2,c1,2000,8,720000,1);

		Node d1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("d1"),new Coord(-40,0));
		Node d2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("d2"), D_START);
		Link d1d =  NetworkUtils.createAndAddLink(network,Id.createLinkId("d1_d"),d1,d,20,8,1100,1);
		Link dd1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("d_d1"),d,d1,20,8,1100,1);
		Link d1d2 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("d1_d2"),d1,d2,2000,8,720000,1);
		Link d2d1 =  NetworkUtils.createAndAddLink(network,Id.createLinkId("d2_d1"),d2,d1,2000,8,720000,1);



	}
}
