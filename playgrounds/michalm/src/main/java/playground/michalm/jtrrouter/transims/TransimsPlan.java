/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.jtrrouter.transims;

import java.io.PrintWriter;

import playground.michalm.jtrrouter.*;

public class TransimsPlan extends Plan implements Comparable<TransimsPlan> {
	private final int startTime;
	private final int stopTime;
	private final int time;

	public TransimsPlan(int id, Route route, int startTime, int stopTime, int time) {
		super(id, route);

		this.startTime = startTime;
		this.stopTime = stopTime;
		this.time = time;
	}

	public int compareTo(TransimsPlan p) {
		return startTime - p.startTime;
	}

	// {$travelerId} 0 1 1
	// {$startTime} {$startParkingId} 2 {$stopParkingId} 2
	// {$time} {$stopTime} 1 0 0
	// 1 0
	// {$tokenCount}
	// {$carId} 0
	// {$nodes}
	public void write(PrintWriter writer) {
		int id = getId();
		Route route = getRoute();

		TransimsFlow inFlow = (TransimsFlow)route.getInFlow();
		TransimsFlow outFlow = (TransimsFlow)route.getOutFlow();

		writer.println(id + " 0 1 1");
		writer.println(startTime + " " + inFlow.inParking + " 2 " + outFlow.outParking + " 2");
		writer.println(time + " " + stopTime + " 1 0 0");
		writer.println("1 0");
		writer.println(route.getNodeCount() + 2);
		writer.println(id + " 0");
		writer.println(route.getNodes());
		writer.println();
	}
}