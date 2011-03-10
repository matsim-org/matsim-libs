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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
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

	public static void play(final ScenarioImpl scenario, final EventsManagerImpl events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
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
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("test/input/playground/marcel/pt/config.xml");
		ScenarioImpl scenario = sl.getScenario();

		NetworkImpl network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();

		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network, scenario).parse("test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml");
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		final EventsManagerImpl events = new EventsManagerImpl();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);

		play(scenario, events);

		writer.closeFile();
	}

}
