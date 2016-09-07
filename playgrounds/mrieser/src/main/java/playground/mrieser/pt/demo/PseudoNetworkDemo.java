/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoNetworkDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.demo;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class PseudoNetworkDemo {

	public static void main(final String[] args) {
		String networkFile = null;
		String transitScheduleFile = null;
		if (args.length == 1) {
			transitScheduleFile = args[0];
		} else {
			networkFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/network.xml";
//			networkFile = "../thesis-data/application/network.oevModellZH.xml";
			transitScheduleFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml";
//			transitScheduleFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitScheduleNoLinks.xml";
//			transitScheduleFile = "../thesis-data/examples/berta/schedule.xml";
//			transitScheduleFile = "/Users/cello/Desktop/Mohit/berlinSchedule.xml";
//			transitScheduleFile = "../thesis-data/application/zuerichSchedule.xml";
//			transitScheduleFile = "../thesis-data/application/transitSchedule.oevModellZH.xml";
		}

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;

		Network network = scenario.getNetwork();
//		network.setCapacityPeriod(3600.0);
		if (networkFile != null) {
			new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		}

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReaderV1(scenario).readFile(transitScheduleFile);

		network.getLinks().clear();
		network.getNodes().clear();

		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		NetworkFromTransitSchedule.createNetwork(schedule, network);

		Link link1 = network.getLinks().values().iterator().next();//getLink(scenario.createId("1"));

		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);
		Activity act = population.getFactory().createActivityFromLinkId("home", link1.getId());
		act.setEndTime(4*3600.0);
		plan.addActivity(act);
		Leg leg = population.getFactory().createLeg(TransportMode.walk);
		leg.setTravelTime(15*3600.0);
		leg.setRoute(((PopulationFactory) scenario.getPopulation().getFactory()).getRouteFactories().createRoute(Route.class, link1.getId(), link1.getId()));
		plan.addLeg(leg);
		plan.addActivity(population.getFactory().createActivityFromLinkId("home", link1.getId()));

		final EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);

		final QSim sim = QSimUtils.createDefaultQSim(scenario, events);
		new CreateVehiclesForSchedule(schedule, scenario.getTransitVehicles()).run();
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, sim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		sim.run();
		writer.closeFile();
	}

}
