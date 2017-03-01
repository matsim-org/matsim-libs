/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.michalm.barcelona.demand;

import java.io.*;

public class BarcelonaServedRequestsWriter {
	private final Iterable<BarcelonaServedRequest> requests;

	public BarcelonaServedRequestsWriter(Iterable<BarcelonaServedRequest> requests) {
		this.requests = requests;
	}

	@SuppressWarnings("deprecation")
	public void writeFile(String file) {
		try (PrintWriter pw = new PrintWriter(new File(file))) {
			pw.println("id,HOUR_start,MINUTE_start,travel time,distance");

			for (BarcelonaServedRequest r : requests) {
				pw.printf("%s,%d,%d,%d,%.1f\n", r.id, //
						r.startTime.getHours(), r.startTime.getMinutes(), //
						r.travelTime, r.distance);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
