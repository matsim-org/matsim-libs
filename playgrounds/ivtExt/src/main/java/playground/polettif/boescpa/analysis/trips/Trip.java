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

package playground.polettif.boescpa.analysis.trips;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * Provides a representation of trips.
 *
 * @author boescpa
 */
public class Trip implements Cloneable{

    private final static String dl = ";"; // delimiter used for the trip as string representation

	public final Id<Person> agentId;
	public final double startTime;
	public final Id<Link> startLinkId;
	public final double startXCoord;
	public final double startYCoord;
	public final double endTime;
	public final Id<Link> endLinkId;
	public final double endXCoord;
	public final double endYCoord;
	public final String mode;
	public final String purpose;
	public final double duration;
	public final double distance;

	public Trip(Id<Person> agentId,
				double startTime, Id<Link> startLinkId, double startXCoord, double startYCoord,
				double endTime, Id<Link> endLinkId, double endXCoord, double endYCoord,
				String mode, String purpose, double duration, double distance) {

		this.agentId = agentId;
		this.startTime = startTime;
		this.startLinkId = startLinkId;
		this.startXCoord = startXCoord;
		this.startYCoord = startYCoord;
		this.endTime = endTime;
		this.endLinkId = endLinkId;
		this.endXCoord = endXCoord;
		this.endYCoord = endYCoord;
		this.mode = mode;
		this.purpose = purpose;
		this.duration = duration;
		this.distance = distance;
	}

	public static String getHeader() {
		return "agentId"+dl+"startTime"+dl+"startLink"+dl+"startXCoord"+dl+"startYCoord"+dl+"endTime"+dl+
                "endLink"+dl+"endXCoord"+dl+"endYCoord"+dl+"mode"+dl+"purpose"+dl+"duration"+dl+"distance";
	}

	@Override
	public String toString() {
		return agentId + dl + startTime + dl + startLinkId + dl + startXCoord + dl
				+ startYCoord + dl + endTime + dl + endLinkId + dl + endXCoord + dl + endYCoord
				+ dl + mode + dl + purpose + dl + duration + dl + distance;
	}

    public static Trip parseTrip(String tripString) {
        String[] tripLine = tripString.split(dl);
        return new Trip(
                Id.create(tripLine[0], Person.class), //agentId
                Double.parseDouble(tripLine[1]), // startTime
                Id.create(tripLine[2], Link.class), // startLinkId
                Double.parseDouble(tripLine[3]), // startXCoord
                Double.parseDouble(tripLine[4]), // startYCoord
                Double.parseDouble(tripLine[5]), // endTime
                Id.create(tripLine[6], Link.class), // endLinkId
                Double.parseDouble(tripLine[7]), // endXCoord
                Double.parseDouble(tripLine[8]), // endYCoord
                tripLine[9], // mode
                tripLine[10], // purpose
                Double.parseDouble(tripLine[11]), // duration
                Double.parseDouble(tripLine[12])); // distance
    }

    public Trip clone() {
        return new Trip(
                Id.createPersonId(this.agentId.toString()),
                this.startTime,
                Id.createLinkId(this.startLinkId.toString()),
                this.startXCoord,
                this.startYCoord,
                this.endTime,
                Id.createLinkId(this.endLinkId.toString()),
                this.endXCoord,
                this.endYCoord,
                this.mode,
                this.purpose,
                this.duration,
                this.distance
        );
    }
}
