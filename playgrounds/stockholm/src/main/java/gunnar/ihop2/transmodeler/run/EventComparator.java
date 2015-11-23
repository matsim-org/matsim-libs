package gunnar.ihop2.transmodeler.run;

import java.util.Comparator;

import org.matsim.api.core.v01.events.Event;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class EventComparator implements Comparator<Event> {

	EventComparator() {
	}

	@Override
	public int compare(final Event o1, final Event o2) {
		final int timeComp = Double.compare(o1.getTime(), o2.getTime());
		if (timeComp != 0) {
			return timeComp;
		} else {
			// this really should only for identical events be the same
			final int stringComp = o1.toString().compareTo(o2.toString());
			if (stringComp == 0 && o1 != o2) {
				throw new RuntimeException(o1 + " and " + o2 + " appear to be the same event");
			}
			return stringComp;
		}
	}
}
