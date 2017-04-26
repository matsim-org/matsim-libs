/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.stockholm;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TaxiTracesReader {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	private final List<TaxiTrace> traces;

	public TaxiTracesReader(List<TaxiTrace> traces) {
		this.traces = traces;
	}

	public void readFile(String file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			// id,time,x,y,status
			// scanner.nextLine();//skip the header line

			for (String line; (line = reader.readLine()) != null;) {
				String[] tokens = line.split(",");
				// 11553,2014-10-06 00:00:00,17.988514,59.320038,0
				String taxiId = tokens[0];
				Date time = parseDate(tokens[1]);
				Coord coord = getNextCoord(tokens[2], tokens[3]);
				boolean hired = tokens[4].equals("1");
				traces.add(new TaxiTrace(taxiId, time, coord, hired));
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Coord getNextCoord(String x, String y) {
		double xDouble = Double.parseDouble(x);
		double yDouble = Double.parseDouble(y);
		return ct.transform(new Coord(xDouble, yDouble));
	}

	static Date parseDate(String date) {
		try {
			return DATE_FORMAT.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
