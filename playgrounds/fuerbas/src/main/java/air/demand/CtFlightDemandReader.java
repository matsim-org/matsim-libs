/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package air.demand;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author treczka
 * @author dgrether
 */
class CtFlightDemandReader {

	
	private static final Logger log = Logger.getLogger(CtFlightDemandReader.class);

	public List<FlightODRelation> readFlightODDataPerMonth(String inputFile) throws IOException {
		log.info("reading flight demand from " + inputFile);
		List<FlightODRelation> gelesenedaten = new ArrayList<FlightODRelation>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(inputFile);
			String line = null;
			while ((line = br.readLine()) != null) {
				// Einlesen der Eingangsdaten und Runterbrechen auf Tagesebene der Verkehrszahlen
				String[] result = line.split(";"); // nimmt die Zeile an jedem ; auseinander
				
				String from = result[0]; // result[0] => Startflughafen; in Arraylist schreiben
				String to = result[1]; // result[1] => Zielflughafen; in Arraylist schreiben
				int noTrips = Integer.parseInt(result[2]); // result[2] => monatl. Passagierzahl; geteilt durch 30 fuer
																														// Tageswert (Sep 2010 => 30Tage)
				FlightODRelation od = new FlightODRelation(from, to, new Double(noTrips));
				gelesenedaten.add(od);
			}
		} catch (FileNotFoundException e) {
			System.err.println("File not Found...");
			e.printStackTrace();
		}
		// System.out.println(gelesenedaten); //Visuelle Ausgabe der Daten in einer Zeile zur Prüfung
		return gelesenedaten;

	}
}
