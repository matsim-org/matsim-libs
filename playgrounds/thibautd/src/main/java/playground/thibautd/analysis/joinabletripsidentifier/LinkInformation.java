/* *********************************************************************** *
 * project: org.matsim.*
 * LinkInformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.joinabletripsidentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * Remembers all pertinent information associated to a link.
 * This data structure is meant to be placed in a QuadTree, at the coordinates
 * of the two extremities of the related link, for easy retrieval of information.
 * @author thibautd
 */
public class LinkInformation {

	private static final Comparator<Entry> departureComparator = new DepartureTimeComparator();
	private static final Comparator<Entry> arrivalComparator = new ArrivalTimeComparator();

	private final Id linkId;
	private final Coord coord;

	private final List<Entry> departures =
		new ArrayList<Entry>();
	private final List<Entry> arrivals =
		new ArrayList<Entry>();

	private double lastDepartureTime = Double.NEGATIVE_INFINITY;
	private boolean departuresAreSorted = true;
	private double lastArrivalTime = Double.NEGATIVE_INFINITY;
	private boolean arrivalsAreSorted = true;

	/**
	 * @param id the link id
	 */
	public LinkInformation(final Id id, final Coord coord) {
		linkId = id;
		this.coord = coord;
	}

	public void handleDeparture(
			final Id tripId,
			final double departureTime,
			final double arrivalTime) {

		if (departureTime < lastDepartureTime) departuresAreSorted = false;
		lastDepartureTime = departureTime;


		if (arrivalTime < lastArrivalTime) arrivalsAreSorted = false;
		lastArrivalTime = arrivalTime;

		Entry entry = new Entry(tripId, departureTime, arrivalTime);
		departures.add(entry);
	}

	public void handleArrival(
			final Id tripId,
			final double departureTime,
			final double arrivalTime) {

		if (departureTime < lastDepartureTime) departuresAreSorted = false;
		lastDepartureTime = departureTime;


		if (arrivalTime < lastArrivalTime) arrivalsAreSorted = false;
		lastArrivalTime = arrivalTime;

		Entry entry = new Entry(tripId, departureTime, arrivalTime);
		arrivals.add(entry);
	}

	public Id getLinkId() {
		return linkId;
	}

	public Coord getCoord() {
		return coord;
	}

	/**
	 * @return an unmutable and sorted by time list of the departure
	 * events on this link.
	 */
	public List<Entry> getDepartures() {
		if (!departuresAreSorted) {
			Collections.sort(departures, departureComparator);
			departuresAreSorted = true;
		}

		return Collections.unmodifiableList(departures);
	}

	/**
	 * @return an unmutable and sorted by time list of the arrival
	 * events on this link.
	 */
	public List<Entry> getArrivals() {
		if (!arrivalsAreSorted) {
			Collections.sort(arrivals, arrivalComparator);
			arrivalsAreSorted = true;
		}

		return Collections.unmodifiableList(arrivals);
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested class
	// /////////////////////////////////////////////////////////////////////////
	public static class Entry {
		private final Id tripId;
		private final double departureTime;
		private final double arrivalTime;

		private Entry(final Id id, final double departureTime, final double arrivalTime) {
			this.tripId = id;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
		}

		public Id getTripId() {
			return this.tripId;
		}

		public double getDepartureTime() {
			return this.departureTime;
		}

		public double getArrivalTime() {
			return this.arrivalTime;
		}

		/**
		 * Tests wether the Entry corresponds to the same trip
		 *
		 * @return true if the parameter is of type Entry and its trip Id is the
		 * same as the one of this instance.
		 */
		@Override
		public boolean equals(final Object other) {
			return (other instanceof Entry) && 
				( ((Entry) other).tripId.equals(tripId) );
		}

		@Override
		public int hashCode() {
			return tripId.hashCode();
		}

		@Override
		public String toString() {
			return "[tripId: "+tripId+"; dep: "+departureTime+"; arr: "+arrivalTime+"]";
		}
	}
}

class DepartureTimeComparator implements Comparator<LinkInformation.Entry> {
	@Override
	public int compare(final LinkInformation.Entry e1, final LinkInformation.Entry e2) {
		return Double.compare(e1.getDepartureTime(), e2.getDepartureTime());
	}
}

class ArrivalTimeComparator implements Comparator<LinkInformation.Entry> {
	@Override
	public int compare(final LinkInformation.Entry e1, final LinkInformation.Entry e2) {
		return Double.compare(e1.getArrivalTime(), e2.getArrivalTime());
	}
}
