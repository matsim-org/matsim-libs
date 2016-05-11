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

package playground.polettif.publicTransitMapping.hafas.hafasCreator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import playground.polettif.publicTransitMapping.hafas.HRDFDefinitions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the transit lines from a given FPLAN file.
 *
 * @author boescpa
 */
public class FPLANReader {
	protected static Logger log = Logger.getLogger(FPLANReader.class);

	protected static Map<String, Integer> readLines(
			TransitSchedule schedule, Vehicles vehicles, Set<Integer> bitfeldNummern, Map<String, String> operators, String FPLAN) {

		Map<String, Integer> vehiclesUndefined = new HashMap<>();
		VehiclesFactory vehicleBuilder = vehicles.getFactory();

		try {
			Map<Id<TransitLine>,PtLineFPLAN> linesFPLAN = new HashMap<>();
			PtRouteFPLAN currentRouteFPLAN = null;

			Counter counter = new Counter("FPLAN line # ");
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(FPLAN), "latin1"));
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
						Id<TransitLine> lineId = Id.create(operators.get(newLine.substring(9, 15).trim()), TransitLine.class);
						PtLineFPLAN lineFPLAN;
						if (linesFPLAN.containsKey(lineId)) {
							lineFPLAN = linesFPLAN.get(lineId);
						} else {
							lineFPLAN = new PtLineFPLAN(lineId);
							linesFPLAN.put(lineId, lineFPLAN);
						}
						// Create the new route in this line...
						int routeNr = 0;
						Id<TransitRoute> routeId =
								Id.create(newLine.substring(3, 8).trim() + "_" + String.format("%03d", routeNr), TransitRoute.class);
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
						boolean addToSchedule = true;
						if (currentRouteFPLAN != null) {
							// Vehicle Id:
							Id<VehicleType> typeId = Id.create(newLine.substring(3, 6).trim(), VehicleType.class);
							VehicleType vehicleType = vehicles.getVehicleTypes().get(typeId);
							if (vehicleType == null) {
								String typeIdstr = typeId.toString();

								vehicleType = vehicleBuilder.createVehicleType(Id.create(typeId.toString(), VehicleType.class));

								// using default values for vehicle type
								vehicleType.setLength(HRDFDefinitions.Vehicles.valueOf(typeIdstr).length);
								vehicleType.setWidth(HRDFDefinitions.Vehicles.valueOf(typeIdstr).width);
								vehicleType.setAccessTime(HRDFDefinitions.Vehicles.valueOf(typeIdstr).accessTime);
								vehicleType.setEgressTime(HRDFDefinitions.Vehicles.valueOf(typeIdstr).egressTime);
								vehicleType.setDoorOperationMode(HRDFDefinitions.Vehicles.valueOf(typeIdstr).doorOperation);
								vehicleType.setPcuEquivalents(HRDFDefinitions.Vehicles.valueOf(typeIdstr).pcuEquivalents);

								VehicleCapacity vehicleCapacity = vehicleBuilder.createVehicleCapacity();
								vehicleCapacity.setSeats(HRDFDefinitions.Vehicles.valueOf(typeIdstr).capacitySeats);
								vehicleCapacity.setStandingRoom(HRDFDefinitions.Vehicles.valueOf(typeIdstr).capacityStanding);
								vehicleType.setCapacity(vehicleCapacity);

								vehicles.addVehicleType(vehicleType);
							}
							currentRouteFPLAN.setUsedVehicle(typeId, vehicleType);
							// First Departure:
							int hourFirstDeparture = Integer.parseInt(newLine.substring(25, 27));
							int minuteFirstDeparture = Integer.parseInt(newLine.substring(27, 29));
							currentRouteFPLAN.setFirstDepartureTime(hourFirstDeparture, minuteFirstDeparture);
						} /*else {
							log.error("*G-Line before appropriate *Z-Line.");
						}*/
					} else if (newLine.charAt(1) == 'A' && newLine.charAt(3) == 'V' && newLine.charAt(4) == 'E') {
						/*Spalte Typ Bedeutung
						1-5 CHAR *A VE
						7-13 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer, ab der die Verkehrstage im Laufweg gelten.
						15-21 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer, bis zu der die Verkehrstage im Laufweg gelten.
						23-28 INT16 (optional) Verkehrstagenummer für die Tage, an denen die Fahrt stattfindet. Fehlt diese Angabe, so verkehrt diese Fahrt täglich (entspricht dann 000000).
						30-35 [#]INT32 (optional) Index für das x. Auftreten oder Abfahrtszeitpunkt.
						37-42 [#]INT32 (optional) Index für das x. Auftreten oder Ankunftszeitpunkt.*/
						if (currentRouteFPLAN != null) {
							int localBitfeldnr = 0;
							if (newLine.substring(22, 28).trim().length() > 0) {
								localBitfeldnr = Integer.parseInt(newLine.substring(22, 28));
							}
							if (!bitfeldNummern.contains(localBitfeldnr)) {
								// Linie gefunden, die nicht werk-täglich verkehrt... => Ignorieren wir...
								linesFPLAN.get(currentRouteFPLAN.getLineId()).removePtRouteFPLAN(currentRouteFPLAN);
								currentRouteFPLAN = null;
							}
						}
					} else if (newLine.charAt(1) == 'L') {
						/*Spalte Typ Bedeutung
						1-2 CHAR *L
						4-11 CHAR Liniennummer*/
						if (currentRouteFPLAN != null) {
							currentRouteFPLAN.setLineDescription(newLine.substring(3, 11).trim());
						}
					} else if (newLine.charAt(1) == 'T') {
						// Initialzeile neue freie Fahrt (Linien welche nicht nach Taktfahrplan fahren...)
						log.error("*T-Line in HAFAS discovered. Please implement appropriate read out.");
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
							arrivalTime = Double.parseDouble(newLine.substring(31, 33)) * 3600 +
									Double.parseDouble(newLine.substring(33, 35)) * 60;
						} catch (Exception e) {	}
						double departureTime = 0;
						try {
							departureTime = Double.parseDouble(newLine.substring(38, 40)) * 3600 +
									Double.parseDouble(newLine.substring(40, 42)) * 60;
						} catch (Exception e) {	}
						Id<TransitStopFacility> stopId = Id.create(newLine.substring(0, 7), TransitStopFacility.class);
						TransitStopFacility stopFacility = schedule.getFacilities().get(stopId);
						currentRouteFPLAN.addStop(stopId, stopFacility, arrivalTime, departureTime);
					} /*else {
						log.error("Laufweg-Line before appropriate *Z-Line.");
					}*/
				}
				newLine = readsLines.readLine();
				counter.incCounter();
			}
			readsLines.close();
			counter.printCounter();
			// Create lines:
			for (Id<TransitLine> transitLineId : linesFPLAN.keySet()) {
				TransitLine line = linesFPLAN.get(transitLineId).createTransitLine();
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

		return vehiclesUndefined;
	}
}
