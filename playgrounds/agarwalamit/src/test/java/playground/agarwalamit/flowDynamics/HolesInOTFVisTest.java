/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
package playground.agarwalamit.flowDynamics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author amit
 */
public class HolesInOTFVisTest {

	private final boolean useOTFVis = true;
	
	@Test 
	public void test(){

		SimpleNetwork net = new SimpleNetwork();

		Scenario sc = net.scenario;

		for (int i=0;i<20;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			Leg leg  = net.population.getFactory().createLeg(TransportMode.car);
			a1.setEndTime(948+i*2);

			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);

		}

		final List<PersonStuckEvent> stuckEvents = new ArrayList<PersonStuckEvent>();
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonStuckEventHandler() {
			
			@Override
			public void reset(int iteration) {
			}
			
			@Override
			public void handleEvent(PersonStuckEvent event) {
				stuckEvents.add(event);
			}
		});
		
		QSim qSim = QSimUtils.createDefaultQSim(sc, manager);
		
		if ( useOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}
		qSim.run();
		
		Assert.assertEquals("There should not be any stuck events.", 0, stuckEvents.size(), MatsimTestUtils.EPSILON);
	}

	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(0.05);
			
			config.qsim().setStuckTime(24*3600); // in order to let agents wait instead of forced entry.
			config.qsim().setEndTime(01*3600);

			config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
			config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

			network = (NetworkImpl) scenario.getNetwork();

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), new Coord(-100., -100.0));
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), new Coord(0.0, 0.0));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), new Coord(1000.0, 0.0));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), new Coord(1000.0, 100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList(TransportMode.car,TransportMode.walk));

			link1 = network.createAndAddLink(Id.createLinkId("1"), node1, node2, 10000, 25, 3600, 1, null, "22"); 
			link2 = network.createAndAddLink(Id.createLinkId("2"), node2, node3, 1000, 15, 360, 1, null, "22");	//flow capacity is 1 PCU per min.
			link3 = network.createAndAddLink(Id.createLinkId("3"), node3, node4, 10000, 25, 3600, 1, null, "22");

			population = scenario.getPopulation();
		}
	}
}
