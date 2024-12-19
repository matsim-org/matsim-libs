package org.matsim.api.core.v01.events;

import java.util.Comparator;

/**
 * Comparator for events. Events are ordered by time and {@link Event#getEventOrder()}.
 */
public class EventComparator implements Comparator<Event> {

	/**
	 * Compares two events by time and {@link Event#getEventOrder()}.
	 */
	public static int compareEvents(Event o1, Event o2) {
		int cmp = Double.compare(o1.getTime(), o2.getTime());
		if (cmp == 0) {
			return Double.compare(o1.getEventOrder(), o2.getEventOrder());
		}

		return cmp;
	}

	@Override
	public int compare(Event o1, Event o2) {
		return compareEvents(o1, o2);
	}
}
