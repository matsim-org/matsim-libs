/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioPlayer.java
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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

/**
 * Visualizes a transit schedule and simulates the transit vehicle's movements.
 *
 * @author mrieser
 */
public class ScenarioPlayer {

	public static void play(final Scenario scenario, final EventsManager events) {
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		final QSim sim = new QSim(scenario, events);
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.run();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		new MatsimNetworkReader(scenario).readFile("test/scenarios/pt-tutorial/multimodalnetwork.xml");

		TransitSchedule schedule = ((ScenarioImpl) scenario).getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network, scenario).parse("test/scenarios/pt-tutorial/transitschedule.xml");
		new CreateVehiclesForSchedule(schedule, ((ScenarioImpl) scenario).getVehicles()).run();

		final EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML("./transitEvents.xml");
		events.addHandler(writer);

		play(scenario, events);

		writer.closeFile();
	}

}
