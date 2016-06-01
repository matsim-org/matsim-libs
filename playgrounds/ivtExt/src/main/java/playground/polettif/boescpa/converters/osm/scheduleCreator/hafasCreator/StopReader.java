/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.boescpa.converters.osm.scheduleCreator.TransitStopCreator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Reads all stops from HAFAS-BFKOORD_GEO and adds them to the provided TransitSchedule.
 *
 * @author boescpa
 */
public class StopReader implements TransitStopCreator {
	protected static Logger log = Logger.getLogger(StopReader.class);

	private final CoordinateTransformation transformation;
	private TransitSchedule schedule;
	private TransitScheduleFactory scheduleBuilder;

	protected StopReader(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}

	@Override
	public void createTransitStops(TransitSchedule schedule, String pathToInputFiles) {
		this.schedule = schedule;
		this.scheduleBuilder = this.schedule.getFactory();
		readStops(pathToInputFiles + "/HAFAS/BFKOORD_GEO");
	}

	private void readStops(String pathToBFKOORD_GEOFile) {
		log.info("  Read transit stops...");
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(pathToBFKOORD_GEOFile), "latin1"));
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
				Coord coord = new Coord(xCoord, yCoord);
				if (this.transformation != null) {
					coord = this.transformation.transform(coord);
				}
				String stopName = newLine.substring(39, newLine.length());
				createStop(stopId, coord, stopName);
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("  Read transit stops... done.");
	}

	private void createStop(Id<TransitStopFacility> stopId, Coord coord, String stopName) {
		TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(stopId, coord, false);
		stopFacility.setName(stopName);
		this.schedule.addStopFacility(stopFacility);
		//log.info("Added " + schedule.getFacilities().get(stopId).toString());
	}
}
