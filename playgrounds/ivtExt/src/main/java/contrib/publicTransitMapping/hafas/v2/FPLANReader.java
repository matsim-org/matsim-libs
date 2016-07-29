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

package contrib.publicTransitMapping.hafas.v2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the transit lines from a given FPLAN file.
 *
 * @author polettif
 */
public class FPLANReader {
	protected static Logger log = Logger.getLogger(FPLANReader.class);

	/**
	 * Only reads the PtRoutes and leaves line/route
	 * separation to a later process
	 *
	 * @return the list of FPLANRoutes
	 */
	public static List<FPLANRoute> parseFPLAN(Set<Integer> bitfeldNummern, Map<String, String> operators, String FPLANfile) throws IOException {
		List<FPLANRoute> hafasRoutes = new ArrayList<>();

			FPLANRoute currentFPLANRoute = null;

			Counter counter = new Counter("FPLAN line # ");
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(FPLANfile), "latin1"));
			String newLine = readsLines.readLine();
			while(newLine != null) {
				if(newLine.charAt(0) == '*') {

					/**
					 Initialzeile neue Fahrt
					 1−2 	CHAR 	*Z
					 4−8 	INT32 	Fahrtnummer
					 10−15 	CHAR 	Verwaltung (6-stellig); Die Verwaltungsangabe darf keine Leerzeichen enthalten.
					 17−21 	INT16 	leer // Tatsächlich unterscheidet dieser Eintrag noch verschiedene Fahrtvarianten...
					 23−25 	INT16 	Taktanzahl; gibt die Anzahl der noch folgenden Takte an.
					 27−29 	INT16 	Taktzeit in Minuten (Abstand zwischen zwei Fahrten).
					 */
					if(newLine.charAt(1) == 'Z') {
						// get operator
						String operator = operators.get(newLine.substring(9, 15).trim());

						// get the fahrtnummer
						String fahrtnummer = newLine.substring(3, 8).trim();

						int numberOfDepartures = 0;
						int cycleTime = 0;
						try {
							numberOfDepartures = Integer.parseInt(newLine.substring(22, 25));
							cycleTime = Integer.parseInt(newLine.substring(26, 29));
						} catch (Exception e) {
						}
						currentFPLANRoute = new FPLANRoute(operator, fahrtnummer, numberOfDepartures, cycleTime);
						hafasRoutes.add(currentFPLANRoute);
					}

					/**
					 Verkehrsmittelzeile
					 1−2 	CHAR 		*G
					 4−6 	CHAR 		Verkehrsmittel bzw. Gattung
					 8−14 	[#]INT32 	(optional) Laufwegsindex oder Haltestellennummer,
					 					ab der die Gattung gilt.
					 16−22 	[#]INT32 	(optional) Laufwegsindex oder Haltestellennummer,
					 					bis zu der die Gattung gilt.
					 24−29 [#]INT32 	(optional) Index für das x. Auftreten oder Abfahrtszeitpunkt // 26-27 hour, 28-29 minute
					 31−36 [#]INT32 	(optional) Index für das x. Auftreten oder Ankunftszeitpunkt
					 */
					else if(newLine.charAt(1) == 'G') {
						if(currentFPLANRoute != null) {
							// Vehicle Id:
							Id<VehicleType> typeId = Id.create(newLine.substring(3, 6).trim(), VehicleType.class);
							currentFPLANRoute.setVehicleTypeId(typeId);

							// First Departure:
							int hourFirstDeparture = Integer.parseInt(newLine.substring(25, 27));
							int minuteFirstDeparture = Integer.parseInt(newLine.substring(27, 29));
							currentFPLANRoute.setFirstDepartureTime(hourFirstDeparture, minuteFirstDeparture);
						}
					}

					/**
					 1-5 	CHAR 		*A VE
					 7-13 	[#]INT32 	(optional) Laufwegsindex oder Haltestellennummer, ab der die Verkehrstage im Laufweg gelten.
					 15-21 	[#]INT32 	(optional) Laufwegsindex oder Haltestellennummer, bis zu der die Verkehrstage im Laufweg gelten.
					 23-28 	INT16 		(optional) Verkehrstagenummer für die Tage, an denen die Fahrt stattfindet. Fehlt diese Angabe, so verkehrt diese Fahrt täglich (entspricht dann 000000).
					 30-35 	[#]INT32 	(optional) Index für das x. Auftreten oder Abfahrtszeitpunkt.
					 37-42 	[#]INT32 	(optional) Index für das x. Auftreten oder Ankunftszeitpunkt.
					 */
					else if(newLine.charAt(1) == 'A' && newLine.charAt(3) == 'V' && newLine.charAt(4) == 'E') {
						if(currentFPLANRoute != null) {
							int localBitfeldnr = 0;
							if(newLine.substring(22, 28).trim().length() > 0) {
								localBitfeldnr = Integer.parseInt(newLine.substring(22, 28));
							}
							if(!bitfeldNummern.contains(localBitfeldnr)) {
								// Linie gefunden, die nicht werktäglich verkehrt... => Ignorieren wir...
								hafasRoutes.remove(currentFPLANRoute);
								currentFPLANRoute = null;
							}
						}
					}

					/**
					 1-2 CHAR *L
					 4-11 CHAR Liniennummer
					 */
					else if(newLine.charAt(1) == 'L') {
						if(currentFPLANRoute != null) {
							currentFPLANRoute.setRouteDescription(newLine.substring(3, 11).trim());
						}
					}

					/**
					 Initialzeile neue freie Fahrt (Linien welche nicht nach Taktfahrplan fahren)
					 */
					else if(newLine.charAt(1) == 'T') {
						log.error("*T-Line in HAFAS discovered. Please implement appropriate read out.");
					}
				}

				/**
				 Regionszeile (Bedarfsfahrten)
				 We don't have this transport mode in  MATSim (yet). => Delete Route and if Line now empty, delete Line.
				 */
				else if(newLine.charAt(0) == '+') {
					log.error("+-Line in HRDF discovered. Please implement appropriate read out.");
				}

				/**
				 Laufwegzeile
				 1−7 	INT32 Haltestellennummer
				 9−29 	CHAR (optional zur Lesbarkeit) Haltestellenname
				 30−35 	INT32 Ankunftszeit an der Haltestelle (lt. Ortszeit der Haltestelle) // 32-33 hour, 34-35 minute
				 37−42 	INT32 Abfahrtszeit an Haltestelle (lt. Ortszeit der Haltestelle) // 39-40 hour, 41-42 minute
				 44−48 	INT32 Ab dem Halt gültige Fahrtnummer (optional)
				 50−55 	CHAR Ab dem Halt gültige Verwaltung (optional)
				 57−57 	CHAR (optional) "X", falls diese Haltestelle auf dem Laufschild der Fahrt aufgeführt wird.
				 */
				else {
					boolean arrivalTimeNegative = newLine.charAt(29) == '-';
					boolean departureTimeNegative = newLine.charAt(36) == '-';

					if(currentFPLANRoute != null) {
						double arrivalTime = 0;
						try {
							arrivalTime = Double.parseDouble(newLine.substring(31, 33)) * 3600 +
									Double.parseDouble(newLine.substring(33, 35)) * 60;
						} catch (Exception e) {
						}
						double departureTime = 0;
						try {
							departureTime = Double.parseDouble(newLine.substring(38, 40)) * 3600 +
									Double.parseDouble(newLine.substring(40, 42)) * 60;
						} catch (Exception e) {
						}

						// only add if stop is not "Durchfahrt" or "Diensthalt"
						if(!(arrivalTimeNegative && departureTimeNegative)) {
							currentFPLANRoute.addRouteStop(newLine.substring(0, 7), arrivalTime, departureTime);
						}
					} /*else {
						log.error("Laufweg-Line before appropriate *Z-Line.");
					}*/
				}

				newLine = readsLines.readLine();
				counter.incCounter();
			}
			readsLines.close();
			counter.printCounter();

		return hafasRoutes;
	}

}
