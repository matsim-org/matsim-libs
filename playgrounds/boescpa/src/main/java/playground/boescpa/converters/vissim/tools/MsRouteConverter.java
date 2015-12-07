/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import playground.boescpa.lib.tools.coordUtils.CoordAnalyzer;
import playground.boescpa.lib.tools.SHPFileUtils;

/**
 * Provides a matsim-events specific implementation of RouteConverter.
 *
 * @author boescpa
 */
public class MsRouteConverter extends AbstractRouteConverter {

	/**
	 * Go trough events and look at all linkleaveevents in area and mode car (and all arrivalevents).
	 * Store events in a hashmap with agent as key and trips (start time, end time, route (array of links)) as values.
	 * 	if for an agent no entry, create new entry = start new trip...
	 * 	if arrivalevent and agent found in current trips and trip a car trip,
	 * 		then assign trip to trip-collection.
	 *		then remove trip from current_trip collection.
	 *
	 * @param path2EventsFile
	 * @param path2MATSimNetwork
	 * @param path2VissimZoneShp
	 * @return
	 */
	@Override
	public List<Trip> routes2Trips(String path2EventsFile, String path2MATSimNetwork, String path2VissimZoneShp) {
		final List<Trip> trips = new ArrayList<Trip>();
		final Map<Id<Person>,Trip> currentTrips = new HashMap<Id<Person>,Trip>();
		final GeographicEventAnalyzer geographicEventAnalyzer = new GeographicEventAnalyzer(path2MATSimNetwork, path2VissimZoneShp);
		final EventsManager events = EventsUtils.createEventsManager();
		final BufferedWriter out = IOUtils.getBufferedWriter(path2EventsFile + "_onlyTrips.txt");
		events.addHandler(new RouteConverterEventHandler() {
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				if (geographicEventAnalyzer.eventInArea(event)) {
					Trip currentTrip = currentTrips.get(Id.createPersonId(event.getVehicleId().toString()));
					if (currentTrip == null) {
						Id<Trip> tripId = Id.create(event.getVehicleId().toString() + "_" + String.valueOf(event.getTime()), Trip.class);
						currentTrip = new Trip(tripId, event.getTime());
						currentTrips.put(Id.createPersonId(event.getVehicleId().toString()),currentTrip);
					}
					currentTrip.links.add(event.getLinkId());
					currentTrip.endTime = event.getTime();
				}
			}
			@Override
			public void handleEvent(PersonArrivalEvent event) {
				Trip currentTrip = currentTrips.get(event.getPersonId());
				if (currentTrip != null) {
					if (event.getLegMode().matches("car")) {
						try {
							out.write(currentTrip.toString());
							out.newLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					currentTrips.remove(event.getPersonId());
				}
			}
			@Override
			public void reset(int iteration) {}
		});
		if (path2EventsFile.endsWith(".xml.gz")) { // if events-File is in the newer xml-format
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(path2EventsFile);
		}
		else {
			throw new IllegalArgumentException("Given events-file not of known format.");
		}
		try {
			out.close();
			BufferedReader in = IOUtils.getBufferedReader(path2EventsFile + "_onlyTrips.txt");
			String line = in.readLine();
			while (line != null) {
				trips.add(new Trip(line));
				line = in.readLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return trips;
	}

	private interface RouteConverterEventHandler extends LinkLeaveEventHandler, PersonArrivalEventHandler {};

	private final class GeographicEventAnalyzer {
		private final CoordAnalyzer coordAnalyzer;
		private final Network network;
		private GeographicEventAnalyzer(String path2MATSimNetwork, String path2VissimZoneShp) {
			// read network
			MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
			NetworkReader.readFile(path2MATSimNetwork);
			this.network = scenario.getNetwork();
			// read zones
			Set<SimpleFeature> features = new HashSet<SimpleFeature>();
			features.addAll(ShapeFileReader.getAllFeatures(path2VissimZoneShp));
			SHPFileUtils util = new SHPFileUtils();
			Geometry cuttingArea = util.mergeGeometries(features);
			this.coordAnalyzer = new CoordAnalyzer(cuttingArea);
		}
		private boolean eventInArea(LinkLeaveEvent event) {
			Link link = network.getLinks().get(event.getLinkId());
			return coordAnalyzer.isLinkAffected(link);
		}
	}
}
