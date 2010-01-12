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

package playground.mmoyo.analysis.counts;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.xml.sax.SAXException;

import playground.mrieser.OTFDemo;
import playground.mrieser.pt.analysis.RouteOccupancy;
import playground.mrieser.pt.analysis.VehicleTracker;

public class OccupancyCounts {

	private static final String SERVERNAME = "OcuppancyCounter";

	public static void play(final ScenarioImpl scenario, final EventsManager events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, (EventsManagerImpl) events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = args[0]; 
		String transitLineStrId = args[1];
		String transitRouteStrId1 = args[2];
		String transitRouteStrId2 = args[3];
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = sl.getScenario();

		NetworkLayer network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();

		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		//TransitSchedule schedule = scenario.getTransitSchedule();
		//new TransitScheduleReaderV1(schedule, network).parse(scheduleFile);
		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();

		//////////////////
		
		EventsManagerImpl events = new EventsManagerImpl();
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRoute route1 = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(transitLineStrId)).getRoutes().get(new IdImpl(transitRouteStrId1));
		TransitRoute route2 = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(transitLineStrId)).getRoutes().get(new IdImpl(transitRouteStrId2));
		RouteOccupancy analysis1 = new RouteOccupancy(route1, vehTracker);
		RouteOccupancy analysis2 = new RouteOccupancy(route2, vehTracker);
		events.addHandler(analysis1);
		events.addHandler(analysis2);

		TransitQueueSimulation sim = new TransitQueueSimulation(scenario, events);

		sim.run();

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		int inVehicle = 0;
		for (TransitRouteStop stop : route1.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis1.getNumberOfEnteringPassengers(stopId);
			int leave = analysis1.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		inVehicle = 0;
		for (TransitRouteStop stop : route2.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis2.getNumberOfEnteringPassengers(stopId);
			int leave = analysis2.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}
	}

}
