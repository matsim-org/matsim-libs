/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;

/**
 * @author michalm
 */
public class DetourTimesProvider {
	class DetourTimesSet {
		// times[0] is a special entry; times[i] corresponds to stop i-1, for 1 <= i <= stopCount
		public final Double[] timesToPickup;// times[0] start->pickup
		public final Double[] timesFromPickup;// times[0] pickup->dropoff
		public final Double[] timesToDropoff;// times[0] NaN
		public final Double[] timesFromDropoff;// times[0] NaN

		public DetourTimesSet(Double[] timesToPickup, Double[] timesFromPickup, Double[] timesToDropoff,
				Double[] timesFromDropoff) {
			this.timesToPickup = timesToPickup;
			this.timesFromPickup = timesFromPickup;
			this.timesToDropoff = timesToDropoff;
			this.timesFromDropoff = timesFromDropoff;
		}
	}

	private final double stopDuration;
	private final DetourTimeEstimator detourTimeEstimator;

	public DetourTimesProvider(DetourTimeEstimator detourTimeEstimator, double stopDuration) {
		this.detourTimeEstimator = detourTimeEstimator;
		this.stopDuration = stopDuration;
	}

	public DetourTimesSet getDetourTimesSet(DrtRequest drtRequest, Entry vEntry) {
		ArrayList<Link> links = new ArrayList<>(vEntry.stops.size() + 1);
		links.add(null);// special link
		for (Stop s : vEntry.stops) {
			links.add(s.task.getLink());
		}

		double earliestPickupTime = drtRequest.getEarliestStartTime();// over-optimistic

		// calc backward TTs from pickup to ends of all stop + start
		links.set(0, vEntry.start.link);
		Double[] timesToPickup = estimateTimesBackwards(drtRequest.getFromLink(), links, earliestPickupTime);

		// calc forward TTs from pickup to beginnings of all stops + dropoff
		links.set(0, drtRequest.getToLink());
		Double[] timesFromPickup = estimateTimesForwards(drtRequest.getFromLink(), links, earliestPickupTime);

		double pickupToDropoffTime = timesFromPickup[0];// only if no other passengers on board (optimistic)
		double minTravelTime = pickupToDropoffTime;
		double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration; // over-optimistic

		// calc backward TTs from dropoff to ends of all stops
		links.set(0, drtRequest.getToLink());// TODO change to null (after nulls are supported by OneToManyPathSearch)
		Double[] timesToDropoff = estimateTimesBackwards(drtRequest.getToLink(), links, earliestDropoffTime);
		timesToDropoff[0] = null;

		// calc forward TTs from dropoff to beginnings of all stops
		Double[] timesFromDropoff = estimateTimesForwards(drtRequest.getToLink(), links, earliestDropoffTime);
		timesFromDropoff[0] = null;

		return new DetourTimesSet(timesToPickup, timesFromPickup, timesToDropoff, timesFromDropoff);
	}

	private Double[] estimateTimesBackwards(Link fromLink, List<Link> toLinks, double startTime) {
		Double[] times = new Double[toLinks.size()];
		for (int i = 0; i < times.length; i++) {
			times[i] = detourTimeEstimator.estimateTime(toLinks.get(i), fromLink);
		}
		return times;
	}

	private Double[] estimateTimesForwards(Link fromLink, List<Link> toLinks, double startTime) {
		Double[] times = new Double[toLinks.size()];
		for (int i = 0; i < times.length; i++) {
			times[i] = detourTimeEstimator.estimateTime(fromLink, toLinks.get(i));
		}
		return times;
	}
}
