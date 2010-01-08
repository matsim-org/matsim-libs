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

package playground.mmoyo.demo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.EventsManagerFactoryImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.mrieser.OTFDemo;
/**
 * @author mrieser
 */

//Copy of Scenario player at playgound.marcel.pt.demo
public class ScenarioPlayer {

	private static final String SERVERNAME = "ScenarioPlayer";

	public static void play(final ScenarioImpl scenario, final EventsManager events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, (EventsManagerImpl) events);
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
		String configFile = args[0]; 
		String scheduleFile = args[1];
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = sl.getScenario();

		NetworkLayer network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();

		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).parse(scheduleFile);
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		final EventsManager events = (new EventsManagerFactoryImpl()).createEventsManager() ;
		EventWriterXML writer = new EventWriterXML("../playgrounds/mmoyo/output/testEvents.xml");
		EventWriterTXT writertxt = new EventWriterTXT("../playgrounds/mmoyo/output/testEvents.txt");
		events.addHandler(writer);
		events.addHandler(writertxt);

		play(scenario, events);

		writer.closeFile();
		writertxt.closeFile();
	}

}
