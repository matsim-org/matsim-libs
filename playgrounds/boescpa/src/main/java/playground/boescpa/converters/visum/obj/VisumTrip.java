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

package playground.boescpa.converters.visum.obj;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.boescpa.lib.tools.tripReader.Trip;
import playground.gregor.sim2d_v4.run.Vis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Provides a representation of visum trips.
 *
 * @author boescpa
 */
public class VisumTrip extends Trip {

	// TODO-boescpa Write tests...

	private static Scenario scenarioUtils = ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public VisumTrip(Id agentId, double startTime, Id startLinkId, double startXCoord, double startYCoord, double endTime, Id endLinkId, double endXCoord, double endYCoord, String mode, String purpose, double duration, long distance) {
		super(agentId, startTime, startLinkId, startXCoord, startYCoord, endTime, endLinkId, endXCoord, endYCoord, mode, purpose, duration, distance);
	}

	public VisumTrip(String[] tripLine) {
		this(
				scenarioUtils.createId(tripLine[1]), //agentId
				Double.parseDouble(tripLine[2]), // startTime
				scenarioUtils.createId(tripLine[3]), // startLinkId
				Double.parseDouble(tripLine[4]), // startXCoord
				Double.parseDouble(tripLine[5]), // startYCoord
				Double.parseDouble(tripLine[6]), // endTime
				scenarioUtils.createId(tripLine[7]), // endLinkId
				Double.parseDouble(tripLine[8]), // endXCoord
				Double.parseDouble(tripLine[9]), // endYCoord
				tripLine[10], // mode
				tripLine[11], // purpose
				Double.parseDouble(tripLine[12]), // duration
				Long.parseLong(tripLine[13]) // distance
		);
	}

	public boolean isModeType(String mode) {
		return mode.equals(this.mode);
	}

	public boolean isWithinZone(Zone interestingArea) {
		boolean withinZone = false;

		// Check startPoint:
		withinZone = interestingArea.isWithinZone(startXCoord, startYCoord);

		// Check endPoint:
		if (!withinZone) {
			withinZone = interestingArea.isWithinZone(endXCoord, endYCoord);
		}

		return withinZone;
	}

	public boolean isWithinHour(int hour) {
		int time = hour*3600;
		boolean withinTime = false;

		// Check startTime:
		withinTime = startTime >= time && startTime <= time + 3600;

		// Check endTime:
		if (!withinTime) {
			withinTime = endTime >= time && endTime <= time + 3600;
		}

		return withinTime;
	}

	public boolean isOriginZone(Zone zone) {
		return zone.isWithinZone(startXCoord, startYCoord);
	}

	public boolean isDestinZone(Zone zone) {
		return zone.isWithinZone(endXCoord, endYCoord);
	}

	public double distanceToCentroid(Zone centroid, boolean origOrDest) {
		// Returns for the current trip the distance to the centroid.
		// origOrDest: If true, then distance to origin, if false, then distance to destination.

		if (origOrDest) {
			// <=> origOrDest = true => Origin
			return centroid.getDistToCentroid(this.startXCoord, this.startYCoord);
		}
		else {
			// <=> origOrDest = false => Destination
			return centroid.getDistToCentroid(this.endXCoord, this.endYCoord);
		}
	}
}
