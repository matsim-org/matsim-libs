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

package playground.mmoyo.demo.berlin.player2;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.marcel.OTFDemo;
import playground.marcel.pt.queuesim.TransitQueueSimulation;
import playground.marcel.pt.routes.ExperimentalTransitRouteFactory;
import playground.marcel.pt.utils.CreateVehiclesForSchedule;

/**
 * @author mrieser
 */

//Copy of Scenario player at playgound.marcel.pt.demo
public class ScenarioPlayer {

	private static final String SERVERNAME = "ScenarioPlayer";

	public static void play(final ScenarioImpl scenario, final EventsImpl events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		ScenarioLoader sl = new ScenarioLoader("src/playground/mmoyo/demo/berlin/player2/config.xml");
		ScenarioImpl scenario = sl.getScenario();

		NetworkLayer network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();

		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).parse("../Shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz");
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		final EventsImpl events = new EventsImpl();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		EventWriterTXT writertxt = new EventWriterTXT("./output/testEvents.txt");
		events.addHandler(writer);
		events.addHandler(writertxt);

		play(scenario, events);

		writer.closeFile();
		writertxt.closeFile();
	}

}
