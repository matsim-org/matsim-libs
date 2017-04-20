/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.michalm.poznan.demand.taxi;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.michalm.demand.taxi.ServedRequest;

public class PoznanServedRequestsReader {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	private final CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	private final List<PoznanServedRequest> requests;

	public PoznanServedRequestsReader(List<PoznanServedRequest> requests) {
		this.requests = requests;
	}

	public void readFile(String file) {
		try (Scanner scanner = new Scanner(new File(file))) {
			// ID Przyjęte Wydane Skąd-dług Skąd-szer Dokąd-dług Dokąd-szer Id.taxi
			scanner.nextLine();// skip the header line

			while (scanner.hasNext()) {
				// 2014_02_000001 01-02-2014 00:00:26 01-02-2014 00:00:22 16.964106 52.401409 16.898370 52.428270 329
				Id<ServedRequest> id = Id.create(scanner.next(), ServedRequest.class);
				Date accepted = getNextDate(scanner);
				Date assigned = getNextDate(scanner);
				Coord from = getNextCoord(scanner);
				Coord to = getNextCoord(scanner);
				Id<String> taxiId = Id.create(scanner.next(), String.class);
				requests.add(new PoznanServedRequest(id, accepted, assigned, from, to, taxiId));
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Date getNextDate(Scanner scanner) {
		String day = scanner.next();
		String time = scanner.next();
		return parseDate(day + " " + time);
	}

	private Coord getNextCoord(Scanner scanner) {
		double x = scanner.nextDouble();
		double y = scanner.nextDouble();
		return ct.transform(new Coord(x, y));
	}

	public static Date parseDate(String date) {
		try {
			return DATE_FORMAT.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}