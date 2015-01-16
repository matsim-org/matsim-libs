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
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class PTScheduleCreatorDefault extends PTScheduleCreator {

	private CoordinateTransformation transformWGS84toCH1903_LV03 = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03");
	protected final Map<String, Integer> vehiclesUndefined = new HashMap<>();

	public PTScheduleCreatorDefault(TransitSchedule schedule, Vehicles vehicles) {
		super(schedule, vehicles);
	}

	@Override
	public final void createSchedule(String osmFile, String hafasFolder, Network network, String vehicleFile) {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read all vehicles from vehicleFile:
		readVehicles(vehicleFile);
		// 2. Read all stops from HAFAS-BFKOORD_GEO
		readStops(hafasFolder + "/BFKOORD_GEO");
		// 3. Create all lines from HAFAS-Schedule
		readLines(hafasFolder + "/FPLAN");
		// 4. Print undefined vehicles
		printVehiclesUndefined();

		log.info("Creating the schedule based on HAFAS... done.");
	}

	////////////////// Local Helpers /////////////////////

	/**
	 * Reads all the vehicle types from the file specified.
	 *
	 * @param vehicleFile from which the vehicle-specifications will be read. For an example of file-structure see test/input/playground/boescpa/converters/osm/scheduleCreator/TestPTScheduleCreatorDefault/VehicleData.csv.
	 */
	protected void readVehicles(String vehicleFile) {
		try {
			FileReader reader = new FileReader(vehicleFile);
			BufferedReader readsLines = new BufferedReader(reader);
			// read header 1 and 2
			readsLines.readLine();
			readsLines.readLine();
			// start the actual readout:
			String newLine = readsLines.readLine();
			while (newLine != null) {
				String[] newType = newLine.split(";");
				// The first line without a key breaks the readout.
				if (newType.length == 0) {
					break;
				}
				// Create the vehicle:
				Id<VehicleType> typeId = Id.create(newType[0].trim(), VehicleType.class);
				VehicleType vehicleType = vehicleBuilder.createVehicleType(typeId);
				vehicleType.setLength(Double.parseDouble(newType[1]));
				vehicleType.setWidth(Double.parseDouble(newType[2]));
				vehicleType.setAccessTime(Double.parseDouble(newType[3]));
				vehicleType.setEgressTime(Double.parseDouble(newType[4]));
				if ("serial".matches(newType[5])) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.serial);
				} else if ("parallel".matches(newType[5])) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.parallel);
				}
				VehicleCapacity vehicleCapacity = vehicleBuilder.createVehicleCapacity();
				vehicleCapacity.setSeats(Integer.parseInt(newType[6]));
				vehicleCapacity.setStandingRoom(Integer.parseInt(newType[7]));
				vehicleType.setCapacity(vehicleCapacity);
				vehicleType.setPcuEquivalents(Double.parseDouble(newType[8]));
				vehicleType.setDescription(newType[9]);
				vehicles.addVehicleType(vehicleType);
				// Read the next line:
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
		//log.info("Added " + schedule.getFacilities().get(stopId).toString());
	}

	protected void readLines(String FPLAN) {
		try {
			Map<Id<TransitLine>,PtLineFPLAN> linesFPLAN = new HashMap<>();
			PtRouteFPLAN currentRouteFPLAN = null;

			FileReader reader = new FileReader(FPLAN);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine();
			while (newLine != null) {
				if (newLine.charAt(0) == '*') {
					if (newLine.charAt(1) == 'Z') {
						// Initialzeile neue Fahrt
						/*Spalte Typ Bedeutung
						1−2 CHAR *Z
						4−8 INT32 Fahrtnummer
						10−15 CHAR Verwaltung (6-stellig); Die Verwaltungsangabe darf
						keine Leerzeichen enthalten.
						17−21 INT16 leer // Tatsächlich unterscheidet dieser Eintrag noch verschiedene Fahrtvarianten...
						23−25 INT16 Taktanzahl; gibt die Anzahl der noch folgenden Takte
						an.
						27−29 INT16 Taktzeit in Minuten (Abstand zwischen zwei Fahrten).*/
						// Get the appropriate transit line...
						Id<TransitLine> lineId = Id.create(newLine.substring(9, 15).trim(), TransitLine.class);
						PtLineFPLAN lineFPLAN;
						if (linesFPLAN.containsKey(lineId)) {
							lineFPLAN = linesFPLAN.get(lineId);
						} else {
							lineFPLAN = new PtLineFPLAN(lineId);
							linesFPLAN.put(lineId, lineFPLAN);
						}
						// Create the new route in this line...
						int routeNr = 0;
						Id<TransitRoute> routeId = Id.create(newLine.substring(3, 8).trim() + "_" + String.format("%03d", routeNr), TransitRoute.class);
						while (lineFPLAN.getIdRoutesFPLAN().contains(routeId)) {
							routeNr++;
							routeId = Id.create(newLine.substring(3, 8).trim() + "_" + String.format("%03d", routeNr), TransitRoute.class);
						}
						int numberOfDepartures = 0;
						int cycleTime = 0;
						try {
							numberOfDepartures = Integer.parseInt(newLine.substring(22, 25));
							cycleTime = Integer.parseInt(newLine.substring(26, 29));
						} catch (Exception e) {	}
						currentRouteFPLAN = new PtRouteFPLAN(lineId, routeId, numberOfDepartures, cycleTime);
						lineFPLAN.addPtRouteFPLAN(currentRouteFPLAN);
					} else if (newLine.charAt(1) == 'T') {
						// Initialzeile neue freie Fahrt (Linien welche nicht nach Taktfahrplan fahren...)
						log.error("*T-Line in HAFAS discovered. Please implement appropriate read out.");
					} else if (newLine.charAt(1) == 'G') {
						// Verkehrsmittelzeile
						/*Spalte Typ Bedeutung
						1−2 CHAR *G
						4−6 CHAR Verkehrsmittel bzw. Gattung
						8−14 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer,
							ab der die Gattung gilt.
						16−22 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer,
							bis zu der die Gattung gilt.
						24−29 [#]INT32 (optional) Index für das x. Auftreten oder
						Abfahrtszeitpunkt // 26-27 hour, 28-29 minute
						31−36 [#]INT32 (optional) Index für das x. Auftreten oder
						Ankunftszeitpunkt*/
						if (currentRouteFPLAN != null) {
							// Vehicle Id:
							Id<VehicleType> typeId = Id.create(newLine.substring(3, 6).trim(), VehicleType.class);
							VehicleType vehicleType = vehicles.getVehicleTypes().get(typeId);
							if (vehicleType == null) {
								Integer occurances = vehiclesUndefined.get(typeId.toString());
								if (occurances == null) {
									vehiclesUndefined.put(typeId.toString(), 1);
								} else {
									vehiclesUndefined.put(typeId.toString(), occurances + 1);
								}
							}
							currentRouteFPLAN.setUsedVehicle(typeId, vehicleType);
							// First Departure:
							int hourFirstDeparture = Integer.parseInt(newLine.substring(25, 27));
							int minuteFirstDeparture = Integer.parseInt(newLine.substring(27, 29));
							currentRouteFPLAN.setFirstDepartureTime(hourFirstDeparture, minuteFirstDeparture);
						} else {
							log.error("*G-Line before appropriate *Z-Line.");
						}
					}
				} else if (newLine.charAt(0) == '+') { // Regionszeile (Bedarfsfahrten)
					// We don't have this transport mode in  MATSim (yet). => Delete Route and if Line now empty, delete Line.
					log.error("+-Line in HAFAS discovered. Please implement appropriate read out.");
				} else { // Laufwegzeile
					/*Spalte Typ Bedeutung
					1−7 INT32 Haltestellennummer
					9−29 CHAR (optional zur Lesbarkeit) Haltestellenname
					30−35 INT32 Ankunftszeit an der Haltestelle (lt. Ortszeit der
							Haltestelle) // 32-33 hour, 34-35 minute
					37−42 INT32 Abfahrtszeit an Haltestelle (lt. Ortszeit der
					Haltestelle) // 39-40 hour, 41-42 minute
					44−48 INT32 Ab dem Halt gültige Fahrtnummer (optional)
							50−55 CHAR Ab dem Halt gültige Verwaltung (optional)
							57−57 CHAR (optional) "X", falls diese Haltestelle auf dem
					Laufschild der Fahrt aufgeführt wird.*/
					if (currentRouteFPLAN != null) {
						double arrivalTime = 0;
						try {
							arrivalTime = Double.parseDouble(newLine.substring(31, 33)) * 60 * 60 +
									Double.parseDouble(newLine.substring(33, 35)) * 60;
						} catch (Exception e) {	}
						double departureTime = 0;
						try {
							departureTime = Double.parseDouble(newLine.substring(38, 40)) * 60 * 60 +
									Double.parseDouble(newLine.substring(40, 42)) * 60;
						} catch (Exception e) {	}
						Id<TransitStopFacility> stopId = Id.create(newLine.substring(0, 7), TransitStopFacility.class);
						TransitStopFacility stopFacility = schedule.getFacilities().get(stopId);
						currentRouteFPLAN.addStop(stopId, stopFacility, arrivalTime, departureTime);
					} else {
						log.error("Laufweg-Line before appropriate *Z-Line.");
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
			// Create lines:
			for (Id<TransitLine> transitLine : linesFPLAN.keySet()) {
				TransitLine line = linesFPLAN.get(transitLine).createLine();
				if (line != null) {
					schedule.addTransitLine(line);
				}
			}
			// Create vehicles:
			for (TransitLine line : schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						Id<Vehicle> vehicleId = departure.getVehicleId();
						Id<VehicleType> vehicleTypeId = Id.create(vehicleId.toString().split("_")[0], VehicleType.class);
						VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
						vehicles.addVehicle(vehicleBuilder.createVehicle(vehicleId, vehicleType));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void printVehiclesUndefined() {
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			log.warn("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}
	}
}
