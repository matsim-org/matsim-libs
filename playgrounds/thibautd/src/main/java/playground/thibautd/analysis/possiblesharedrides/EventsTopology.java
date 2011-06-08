/* *********************************************************************** *
 * project: org.matsim.*
 * EventsTopology.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEvent;

/**
 * Defines a topology on events.
 * Neigborhood is defined by a LinkTopology and a time window.
 *
 * @author thibautd
 */
public class EventsTopology {
	private static final Logger log =
		Logger.getLogger(EventsTopology.class);


	// should be made unmuttable
	private final List<? extends LinkEvent> events;
	private final LinkTopology linkTopology;
	private final Comparator<Event> timeComparator = new TimeComparator();
	private final double timeWindowRadius;
	private boolean lastExplorationWasExhaustive = false;
	private final int nEvents;
	
	public EventsTopology(
			final List<? extends LinkEvent> events,
			final double timeWindowRadius,
			final LinkTopology linkTopology) {
		log.info("constructing event topology...");
		// not safe, clone events!
		this.events = events;
		this.nEvents = events.size();
		Collections.sort(events, this.timeComparator); 
		this.linkTopology = linkTopology;
		this.timeWindowRadius = timeWindowRadius;
		log.info("constructing event topology... DONE");
	}

	/*
	 * =========================================================================
	 * public methods
	 * =========================================================================
	 */
	/**
	 * get neighbors based on the link topology and the default time
	 * window
	 */
	public List<LinkEvent> getNeighbors(final LinkEvent event) {
		List<? extends LinkEvent> temporalNeighbors = getTemporalNeighbors(event);
		List<Id> spatialNeighborhood = 
			this.linkTopology.getNeighbors(event.getLinkId());
		List<LinkEvent> output = new ArrayList<LinkEvent>();

		for (LinkEvent currentEvent : temporalNeighbors) {
			if (spatialNeighborhood.contains(currentEvent.getLinkId())) {
				output.add(currentEvent);
			}
		}

		return output;
	}

	/**
	 * @return all events in the specified time window at the given link
	 */
	public List<LinkEvent> getEventsInTimeWindow(
			final Id linkId,
			final double timeWindowCenter,
			final double timeWindowRadius) {
		List<? extends LinkEvent> neighbors = getTemporalNeighbors(timeWindowCenter, timeWindowRadius);
		List<LinkEvent> output = new ArrayList<LinkEvent>();

		// only return events for the good link
		for (LinkEvent event : neighbors) {
			if (event.getLinkId().equals(linkId)) {
				output.add(event);
			}
		}

		return output;
	}

	public int getNumberOfEvents() {
		return this.events.size();
	}

	public boolean wasLastExplorationExhaustive() {
		return this.lastExplorationWasExhaustive;
	}

	/*
	 * =========================================================================
	 * helpers
	 * =========================================================================
	 */
	private List<? extends LinkEvent> getTemporalNeighbors(final LinkEvent event) {
		return getTemporalNeighbors(event.getTime(), this.timeWindowRadius);
	}

	/**
	 * returns all the events in the specified time widow by performing
	 * binary search in the (sorted) event list.
	 */
	private List<? extends LinkEvent>  getTemporalNeighbors(
			final double timeWindowCenter,
			final double timeWindowRadius) {
		int lowIndex = 0;
		int upperIndex = this.events.size();
		int upperIndexUpperBound = upperIndex;
		int lowerIndexUpperBound = lowIndex;
		int midIndex;
		double searchedLowValue = timeWindowCenter - timeWindowRadius;
		double searchedUpperValue = timeWindowCenter + timeWindowRadius;
		double currentValue;

		//search lower index by binary search in the full list
		while (lowIndex < upperIndex - 1) {
			midIndex = (upperIndex + lowIndex) / 2;
			currentValue = this.events.get(midIndex).getTime();

			if (currentValue < searchedLowValue) {
				lowIndex = midIndex;
			}
			else if (currentValue > searchedLowValue) {
				upperIndex = midIndex;
				// use this exploration to restrict following one
				if (currentValue > searchedUpperValue) {
					upperIndexUpperBound = midIndex;
				}
				else {
					lowerIndexUpperBound = Math.max(midIndex, lowerIndexUpperBound);
				}
			}
			else {
				//log.warn("exact equality found for time"+searchedLowValue+", may not be handled correctly");
				//lowIndex = upperIndex = midIndex;
				lowIndex = (lowIndex + midIndex) / 2;
				upperIndex = (upperIndex + midIndex) / 2;
			}
		}

		//search the upper index in the remaining list.
		while (lowerIndexUpperBound < upperIndexUpperBound - 1) {
			midIndex = (upperIndexUpperBound + lowerIndexUpperBound) / 2;
			currentValue = this.events.get(midIndex).getTime();

			if (currentValue < searchedUpperValue) {
				lowerIndexUpperBound = midIndex;
			}
			else if (currentValue > searchedUpperValue) {
				upperIndexUpperBound = midIndex;
			}
			else {
				//log.warn("exact equality found for time"+searchedUpperValue+", may not be handled correctly");
				//lowerIndexUpperBound = upperIndexUpperBound = midIndex;
				lowerIndexUpperBound = (lowerIndexUpperBound + midIndex) / 2;
				upperIndexUpperBound = (upperIndexUpperBound + midIndex) / 2;
			}
		}

		this.lastExplorationWasExhaustive = ((lowIndex == 0) && (upperIndexUpperBound == this.nEvents));

		//log.debug("getting events("+lowIndex+", "+upperIndexUpperBound+")");
		return this.events.subList(lowIndex, upperIndexUpperBound);
	}

	private class TimeComparator implements Comparator<Event> {
		public TimeComparator() {}

		@Override
		public int compare(Event event1, Event event2) {
			double time1 = event1.getTime();
			double time2 = event2.getTime();

			if (time1 < time2) {
				return -1;
			}
			if (time1 > time2) {
				return 1;
			}
			return 0;
		}
	}
}

