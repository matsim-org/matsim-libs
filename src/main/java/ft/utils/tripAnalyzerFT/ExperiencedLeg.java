/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package ft.utils.tripAnalyzerFT;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

// Coords unavailable in events

/**
 * @author gleich
 */
public class ExperiencedLeg {
    private final Id<Person> agent;
    //	private final Coord from;
//	private final Coord to;
    private final Id<Link> fromLinkId;
    private final Id<Link> toLinkId;
    private final double startTime;
    private final double endTime;

    private final String mode;
    private final double waitTime;
    private final double grossWaitTime;
    private final double inVehicleTime;
    private final double distance;
    private final Id<TransitStopFacility> ptFromStop;
    private final Id<TransitStopFacility> ptToStop;

    private final Id<TransitRoute> transitRouteId;

    ExperiencedLeg(Id<Person> agent,
//			Coord from, Coord to, 
                   Id<Link> fromLink, Id<Link> toLink, double startTime,
                   double endTime, String mode, double waitTime, double grossWaitTime, double inVehicleTime,
                   double distance, Id<TransitRoute> transitRouteId,
                   Id<TransitStopFacility> ptFromStop, Id<TransitStopFacility> ptToStop) {
        this.agent = agent;
//		this.from = from;
//		this.to = to;
        this.fromLinkId = fromLink;
        this.toLinkId = toLink;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mode = mode;
        this.waitTime = waitTime;
        this.grossWaitTime = grossWaitTime;
        this.inVehicleTime = inVehicleTime;
        this.distance = distance;
        this.transitRouteId = transitRouteId;
        this.ptFromStop = ptFromStop;
        this.ptToStop = ptToStop;
    }

    double getDistance() {
        return distance;
    }

    Id<Person> getAgent() {
        return agent;
    }

    //	Coord getFrom() {
//		return from;
//	}
//	Coord getTo() {
//		return to;
//	}
    Id<Link> getFromLinkId() {
        return fromLinkId;
    }

    Id<Link> getToLinkId() {
        return toLinkId;
    }

    double getStartTime() {
        return startTime;
    }

    double getEndTime() {
        return endTime;
    }

    String getMode() {
        return mode;
    }

    double getWaitTime() {
        return waitTime;
    }

    double getGrossWaitTime() {
        return grossWaitTime;
    }

    double getInVehicleTime() {
        return inVehicleTime;
    }

    Id<TransitRoute> getTransitRouteId() {
        return transitRouteId;
    }

    Id<TransitStopFacility> getPtFromStop() {
        return ptFromStop;
    }

    Id<TransitStopFacility> getPtToStop() {
        return ptToStop;
    }

}
