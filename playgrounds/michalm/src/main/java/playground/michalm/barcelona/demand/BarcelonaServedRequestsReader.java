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

package playground.michalm.barcelona.demand;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.michalm.demand.taxi.ServedRequest;

public class BarcelonaServedRequestsReader {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	private final CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM31N);

	private final List<BarcelonaServedRequest> requests;

	public BarcelonaServedRequestsReader(List<BarcelonaServedRequest> requests) {
		this.requests = requests;
	}

	public void readFile(String file) {
		int inconsistentTimeCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// id,FROM-y,FROM-x,TO-y,TO-x,DATE_start,HOUR_start,MINUTE_start,DATE_end,HOUR_end,MINUTE_end,travel
			// time,distance
			br.readLine();// skip the header line

			String line = null;
			while ((line = br.readLine()) != null) {
				Scanner scanner = new Scanner(line);
				scanner.useDelimiter(",");
				scanner.useLocale(Locale.UK);

				// 30146,41.223824,2.063528,41.214885,2.062071,08/01/2009,19,7,08/01/2009,19,15,8,2.1
				Id<ServedRequest> id = Id.create(scanner.next(), ServedRequest.class);
				Coord from = getNextCoord(scanner);
				Coord to = getNextCoord(scanner);
				Date start = getNextDate(scanner);
				Date end = getNextDate(scanner);
				int travelTime = scanner.nextInt();// min
				double distance = scanner.nextDouble();// km

				if (travelTime * 60_000 != end.getTime() - start.getTime()) {
					inconsistentTimeCount++;
				}

				requests.add(new BarcelonaServedRequest(id, from, to, start, end, travelTime, distance));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.out.println("#BarcelonaServedRequests read: " + requests.size());
		if (inconsistentTimeCount > 0) {
			System.out.println("#inconsistentTimeCount: " + inconsistentTimeCount);
		}

	}

	private Date getNextDate(Scanner scanner) {
		// DATE_start,HOUR_start,MINUTE_start,DATE_end,HOUR_end,MINUTE_end
		String day = scanner.next();
		String hour = scanner.next();
		String minute = scanner.next();
		return parseDate(day + " " + hour + ":" + minute);
	}

	private Coord getNextCoord(Scanner scanner) {
		// FROM-y,FROM-x,TO-y,TO-x (all WGS84)
		double y = degMin2Deg(scanner.nextDouble());
		double x = degMin2Deg(scanner.nextDouble());
		return ct.transform(new Coord(x, y));
	}

	private static double degMin2Deg(double v) {
		double deg = (int)v;
		double min = v - deg;
		return deg + min * 5 / 3;
	}

	public static Date parseDate(String date) {
		try {
			return DATE_FORMAT.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}