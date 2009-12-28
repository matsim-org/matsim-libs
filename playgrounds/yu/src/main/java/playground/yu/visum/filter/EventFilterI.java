/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.core.api.experimental.events.Event;

/**
 * This interface extends interface:
 * org.matsim.playground.filters.filter.FilterI, and offers some important
 * functions for org.matsim.playground.filters.filter.EventFilter.
 * 
 * @author ychen
 * 
 */
public interface EventFilterI extends FilterI {
	/**
	 * judges whether the Event
	 * (org.matsim.events.Event) will be selected or not
	 * 
	 * @param event -
	 *            which is being judged
	 * @return true if the Person meets the criterion of the EventFilterA
	 */
	boolean judge(Event event);

	/**
	 * sends the person to the next EventFilterA
	 * (org.matsim.playground.filters.filter.EventFilter) or other behavior
	 * 
	 * @param event -
	 *            an event being handled
	 */
	void handleEvent(Event event);

	/**
	 * sets the next Filter, who will handle Event-object.
	 * 
	 * @param nextFilter -
	 *            the next Filter, who will handle Event-object.
	 */
	void setNextFilter(EventFilterI nextFilter);
}
