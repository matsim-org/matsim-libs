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

package playground.polettif.boescpa.converters.osm.scheduleCreator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtLineFPLAN;
import playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN;

import java.io.*;
import java.util.*;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
@Deprecated
public class PTScheduleCreatorDefaultV2 {

	protected static Logger log = Logger.getLogger(PTScheduleCreatorDefaultV2.class);

	protected final TransitSchedule schedule;
	protected final TransitScheduleFactory scheduleBuilder;
	protected final Vehicles vehicles;
	protected final VehiclesFactory vehicleBuilder;
	private CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	protected final Map<String, Integer> vehiclesUndefined = new HashMap<>();
	private final Set<Integer> bitfeldNummern = new HashSet<>();
	private final Map<String, String> operators = new HashMap<>();

	public PTScheduleCreatorDefaultV2(TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.scheduleBuilder = this.schedule.getFactory();
		this.vehicles = vehicles;
		this.vehicleBuilder = this.vehicles.getFactory();
	}

	public final void createSchedule(String osmFile, String hafasFolder, Network network, String vehicleFile) {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read all vehicles from vehicleFile:
		readVehicles(vehicleFile);
		// 2. Read all stops from HAFAS-BFKOORD_GEO
		readStops(hafasFolder + "/BFKOORD_GEO");
		// 3. Read all operators from BETRIEB_DE
		readOperators(hafasFolder + "/BETRIEB_DE");
		// 4. Read all ids for work-day-routes from HAFAS-BITFELD
		readDays(hafasFolder + "/FPLAN", hafasFolder + "/BITFELD");
		// 5. Create all lines from HAFAS-Schedule
		readLines(hafasFolder + "/FPLAN");
		// 6. Print undefined vehicles
		printVehiclesUndefined();
		// 7. Clean schedule
		removeNonUsedStopFacilities();
		uniteSameRoutesWithJustDifferentDepartures();
		cleanDepartures();
		cleanVehicles();

		log.info("Creating the schedule based on HAFAS... done.");
	}

	////////////////// Local Helpers /////////////////////

	/**
	 * Reads all the vehicle types from the file specified.
	 *
	 * @param vehicleFile from which the vehicle-specifications will be read. For an example of file-structure see test/input/playground/boescpa/converters/osm/scheduleCreator/TestPTScheduleCreatorDefault/VehicleData.csv.
	 */
	protected void readVehicles(String vehicleFile) {
		log.info("  Read vehicles...");
		try {
			BufferedReader readsLines = new BufferedReader(new FileReader(vehicleFile));
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
		log.info("  Read vehicles... done.");
	}

	protected void readStops(String BFKOORD_GEOFile) {
		log.info("  Read transit stops...");
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BFKOORD_GEOFile), "latin1"));
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
				Coord coord = this.transformation.transform(new Coord(xCoord, yCoord));
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

	private void readOperators(String BETRIEB_DE) {
		log.info("  Read operators...");
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BETRIEB_DE), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				String abbrevationOperator = newLine.split("\"")[1].replace(" ","");
				newLine = readsLines.readLine();
				if (newLine == null) break;
				String operatorId = newLine.substring(8, 14).trim();
				operators.put(operatorId, abbrevationOperator);
				// read the next operator:
				newLine = readsLines.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void readDays(String FPLAN, String BITFELD) {
		log.info("  Read bitfeld numbers...");
		final int posMaxFVals = find4DayBlockWithMostFVals(FPLAN, BITFELD);
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BITFELD), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−6 INT32 Bitfeldnummer
				8−103 CHAR Bitfeld (Binärkodierung der Tage, an welchen Fahrt, in Hexadezimalzahlen notiert.)*/
				int bitfeldnummer = Integer.parseInt(newLine.substring(0, 6));
				String bitfeld = newLine.substring(7, 103);
				/* As we assume that the posMaxFVals describes a 4-day block with either Monday-Tuesday-Wednesday-Thursday or
				Tuesday-Wednesday-Thursday-Friday and because we don't want a Monday to be the reference day, we select those
				lines which have the second bit on one. The second stands for the 4 in the hexadecimal calculation, else we
				want all hexadecimal values which include a 4, that is 4, 5, 6, 7, 12 (C), 13 (D), 14 (E) and 15 (F).*/
				int matches = (bitfeld.charAt(posMaxFVals) == '4')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '5')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '6')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '7')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'C')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'D')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'E')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'F')? 1 : 0;
				if (matches >= 1) {
					this.bitfeldNummern.add(bitfeldnummer);
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.bitfeldNummern.add(0);
		log.info("  Read bitfeld numbers... done.");
	}

	/**
	 * Returns the 4-day bitfeld block that has the most F-values. The assumption is that this block is either a
	 * Monday-Tuesday-Wednesday-Thursday or a Tuesday-Wednesday-Thursday-Friday block because all other blocks have
	 * at least one Weekend-Day and therefore are less like to produce an F (an F means traveling at all four days).
	 *
	 * @param BITFELD
	 * @return
	 */
	private int find4DayBlockWithMostFVals(String FPLAN, String BITFELD) {
		Map<Integer, Integer> departuresPerBitfeld = new HashMap<>();
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(FPLAN), "latin1"));
			String newLine = readsLines.readLine();
			int numberOfDepartures = 0;
			while (newLine != null) {
				if (newLine.charAt(0) == '*') {
					if (newLine.charAt(1) == 'Z') {
						try {
							numberOfDepartures = Integer.parseInt(newLine.substring(22, 25)) + 1;
						} catch (Exception e) {
							numberOfDepartures = 1;
						}
					}
					if (newLine.charAt(1) == 'A' && newLine.charAt(3) == 'V') {
						if (newLine.substring(22, 28).trim().length() > 0) {
							int bitfeldNumber = Integer.parseInt(newLine.substring(22, 28));
							int bitfeldValue = numberOfDepartures;
							if (departuresPerBitfeld.containsKey(bitfeldNumber)) {
								bitfeldValue += departuresPerBitfeld.get(bitfeldNumber);
							}
							departuresPerBitfeld.put(bitfeldNumber, bitfeldValue);
						}
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] bitfeldStats = new int[96];
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BITFELD), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−6 INT32 Bitfeldnummer
				8−103 CHAR Bitfeld (Binärkodierung der Tage, an welchen Fahrt, in Hexadezimalzahlen notiert.)*/
				int bitFeldValue = 1;
				if (departuresPerBitfeld.containsKey(Integer.parseInt(newLine.substring(0, 6)))) {
					bitFeldValue = departuresPerBitfeld.get(Integer.parseInt(newLine.substring(0, 6)));
				}
				String bitfeld = newLine.substring(7, 103);
				for (int i = 0; i < bitfeld.length(); i++) {
					if (bitfeld.charAt(i) == 'F') {
						bitfeldStats[i] += bitFeldValue;
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int maxFNumber = 0;
		int posMaxFNumber = -1;
		for (int i = 0; i < bitfeldStats.length; i++) {
			if (bitfeldStats[i] > maxFNumber) {
				maxFNumber = bitfeldStats[i];
				posMaxFNumber = i;
			}
		}
		log.info("Selected HAFAS-4day-block: " + posMaxFNumber);
		return posMaxFNumber;
	}

	protected void readLines(String FPLAN) {
		log.info("  Read transit lines...");
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
							if (!this.bitfeldNummern.contains(localBitfeldnr)) {
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
		log.info("  Read transit lines... done.");
	}

	protected void printVehiclesUndefined() {
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			log.warn("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}
	}

	private void removeNonUsedStopFacilities() {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for (Id<TransitStopFacility> facilityId : this.schedule.getFacilities().keySet()) {
			if (!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(this.schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for (TransitStopFacility facility : unusedStopFacilites) {
			this.schedule.removeStopFacility(facility);
		}
	}

	private void cleanVehicles() {
		final Set<Id<Vehicle>> usedVehicles = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					usedVehicles.add(departure.getVehicleId());
				}
			}
		}
		final Set<Id<Vehicle>> vehicles2Remove = new HashSet<>();
		for (Id<Vehicle> vehicleId : this.vehicles.getVehicles().keySet()) {
			if (!usedVehicles.contains(vehicleId)) {
				vehicles2Remove.add(vehicleId);
			}
		}
		for (Id<Vehicle> vehicleId : vehicles2Remove) {
			if (!usedVehicles.contains(vehicleId)) {
				this.vehicles.removeVehicle(vehicleId);
			}
		}
	}

	private void cleanDepartures() {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				final Set<Double> departureTimes = new HashSet<>();
				final List<Departure> departuresToRemove = new ArrayList<>();
				for (Departure departure : route.getDepartures().values()) {
					double dt = departure.getDepartureTime();
					if (departureTimes.contains(dt)) {
						departuresToRemove.add(departure);
					} else {
						departureTimes.add(dt);
					}
				}
				for (Departure departure2Remove : departuresToRemove) {
					route.removeDeparture(departure2Remove);
				}
			}
		}
	}

	protected void uniteSameRoutesWithJustDifferentDepartures() {
		long totalNumberOfDepartures = 0;
		long departuresWithChangedSchedules = 0;
		long totalNumberOfStops = 0;
		long stopsWithChangedTimes = 0;
		double changedTotalTimeAtStops = 0.;
		List<Double> timeChanges = new ArrayList<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			// Collect all route profiles
			final Map<String, List<TransitRoute>> routeProfiles = new HashMap<>();
			for (TransitRoute route : line.getRoutes().values()) {
				totalNumberOfDepartures += route.getDepartures().size();
				totalNumberOfStops += route.getDepartures().size() * route.getStops().size();
				String routeProfile = route.getStops().get(0).getStopFacility().getId().toString();
				for (int i = 1; i < route.getStops().size(); i++) {
					//routeProfile = routeProfile + "-" + route.getStops().get(i).toString() + ":" + route.getStops().get(i).getDepartureOffset();
					routeProfile = routeProfile + "-" + route.getStops().get(i).getStopFacility().getId().toString();
				}
				List profiles = routeProfiles.get(routeProfile);
				if (profiles == null) {
					profiles = new ArrayList();
					routeProfiles.put(routeProfile, profiles);
				}
				profiles.add(route);
			}
			// Check profiles and if the same, add latter to former.
			for (List<TransitRoute> routesToUnite : routeProfiles.values()) {
				TransitRoute finalRoute = routesToUnite.get(0);
				for (int i = 1; i < routesToUnite.size(); i++) {
					TransitRoute routeToAdd = routesToUnite.get(i);
					// unite departures
					for (Departure departure : routeToAdd.getDepartures().values()) {
						finalRoute.addDeparture(departure);
					}
					line.removeRoute(routeToAdd);
					// make analysis
					int numberOfDepartures = routeToAdd.getDepartures().size();
					boolean departureWithChangedDepartureTimes = false;
					for (int j = 0; j < finalRoute.getStops().size(); j++) {
						double changedTotalTimeAtStop =
								Math.abs(finalRoute.getStops().get(j).getArrivalOffset() - routeToAdd.getStops().get(j).getArrivalOffset())
								+ Math.abs(finalRoute.getStops().get(j).getDepartureOffset() - routeToAdd.getStops().get(j).getDepartureOffset());
						if (changedTotalTimeAtStop > 0) {
							stopsWithChangedTimes += numberOfDepartures;
							changedTotalTimeAtStops += changedTotalTimeAtStop*numberOfDepartures;
							for (int k = 0; k < numberOfDepartures; k++) {
								timeChanges.add(changedTotalTimeAtStop);
							}
							departureWithChangedDepartureTimes = true;
						}
					}
					if (departureWithChangedDepartureTimes) {
						departuresWithChangedSchedules += numberOfDepartures;
					}
				}
			}
		}
		log.info("Total Number of Departures: " + totalNumberOfDepartures);
		log.info("Number of Departures with changed schedule: " + departuresWithChangedSchedules);
		log.info("Total Number of Stops: " + totalNumberOfStops);
		log.info("Number of Stops with changed departure or arrival times: " + stopsWithChangedTimes);
		log.info("Total time difference caused by changed departure or arrival times: " + changedTotalTimeAtStops);
		log.info("Average time difference caused by changed times: " + (changedTotalTimeAtStops/stopsWithChangedTimes));
		log.info("Average time difference over all stops caused by changed times: " + (changedTotalTimeAtStops/totalNumberOfStops));
		//writeChangedTimes(timeChanges);
	}

	private void writeChangedTimes(List<Double> timeChanges) {
		BufferedWriter bw = null;
		try {
			// here absolute path hard-coded (not very elegant but as it is only for analysis purposes...)
			bw = new BufferedWriter(new FileWriter("c:\\changedTimes.csv"));
			for (double timeDelta : timeChanges) {
				bw.write(timeDelta + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing changedTimes.csv", e);
		} finally {
			if (bw != null) {
				try { bw.close(); }
				catch (IOException e) { System.out.print("Could not close stream."); }
			}
		}
	}

}
