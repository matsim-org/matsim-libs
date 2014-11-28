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

package playground.boescpa.converters.osm.scheduleCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class PTScheduleCreatorDefault extends PTScheduleCreator {

	private CoordinateTransformation transformWGS84toCH1903_LV03 = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03");

	public PTScheduleCreatorDefault(TransitSchedule schedule) {
		super(schedule);
	}

	@Override
	public final void createSchedule(String osmFile, String hafasFolder, Network network) {
		log.info("Creating the schedule...");
		createPTLines(hafasFolder);
		complementPTStations(osmFile);
		log.info("Creating the schedule... done.");
	}

	/**
	 * Create all pt-lines (stops, schedule, but no routes) of all types of public transport
	 * using the HAFAS-schedule.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFolder
	 */
	private void createPTLines(String hafasFolder) {
		log.info("Creating pt lines from HAFAS file...");

		// TODO-boescpa Implement createPTLines...
		// work with this.schedule...

		// 1. Read all stops from HAFAS-BFKOORD_GEO
		readStops(hafasFolder + "/BFKOORD_GEO");
		// 2. Create all lines from HAFAS-Schedule
		//		1. Stops

		//		2. Schedule


		log.info("Creating pt lines from HAFAS file... done.");
	}

	/**
	 * Check and correct pt-Station-coordinates with osm-knowledge.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param osmFile
	 */
	private void complementPTStations(String osmFile) {
		log.info("Correcting pt station coordinates based on OSM...");

		// TODO-boescpa Implement complementPTStations...
		// work with this.schedule...

		log.info("Correcting pt station coordinates based on OSM... done.");
	}

	////////////////// Local Helper-Methods /////////////////////

	protected void readStops(String BFKOORD_GEOFile) {
		try {
			FileReader reader = new FileReader(BFKOORD_GEOFile);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−7 INT32 Nummer der Haltestelle
				9−18 FLOAT X-Koordinate
				20−29 FLOAT Y-Koordinate
				31−36 INT16 Z-Koordinate (optional)
				38ff CHAR Kommentarzeichen "%"gefolgt vom Klartext des Haltestellennamens (optional zur besseren Lesbarkeit)*/
				Id<TransitStopFacility> stopId = Id.create(newLine.substring(0, 7), TransitStopFacility.class);
				double xCoord = Double.parseDouble(newLine.substring(8, 18));
				double yCoord = Double.parseDouble(newLine.substring(19, 29));
				Coord coord = this.transformWGS84toCH1903_LV03.transform(new CoordImpl(xCoord, yCoord));
				String stopName = newLine.substring(39, newLine.length());
				createStop(stopId, coord, stopName);
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createStop(Id<TransitStopFacility> stopId, Coord coord, String stopName) {
		TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(stopId, coord, false);
		stopFacility.setName(stopName);
		this.schedule.addStopFacility(stopFacility);
	}

	private void createLine(Id<TransitLine> lineId, List<TransitRoute> routes) {
		TransitLine line = scheduleBuilder.createTransitLine(lineId);
		for (TransitRoute route : routes) {
			line.addRoute(route);
		}
		schedule.addTransitLine(line);
	}

	private TransitRoute createRoute(Id<TransitRoute> routeId, List<TransitRouteStop> stops, List<Departure> departures) {
		TransitRoute transitRoute = scheduleBuilder.createTransitRoute(routeId, null, stops, "pt");
		for (Departure departure : departures) {
			transitRoute.addDeparture(departure);
		}
		return transitRoute;
	}

	private TransitRouteStop createRouteStop(TransitStopFacility stopFacility, double arrivalDelay, double departureDelay, boolean awaitDepartureTime) {
		TransitRouteStop routeStop = scheduleBuilder.createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
		routeStop.setAwaitDepartureTime(awaitDepartureTime);
		return routeStop;
	}

	private Departure createDeparture(Id<Departure> departureId, double departureTime, Id<Vehicle> vehicleId) {
		Departure departure = scheduleBuilder.createDeparture(departureId, departureTime);
		departure.setVehicleId(vehicleId);
		return departure;
	}
}
