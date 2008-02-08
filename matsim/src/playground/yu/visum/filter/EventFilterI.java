/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.events.BasicEvent;

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
	 * judges whether the BasicEvent
	 * (org.matsim.demandmodeling.events.BasicEvent) will be selected or not
	 * 
	 * @param event -
	 *            which is being judged
	 * @return true if the Person meets the criterion of the EventFilterA
	 */
	boolean judge(BasicEvent event);

	/**
	 * sends the person to the next EventFilterA
	 * (org.matsim.playground.filters.filter.EventFilter) or other behavior
	 * 
	 * @param event -
	 *            an event being handled
	 */
	void handleEvent(BasicEvent event);

	/**
	 * sets the next Filter, who will handle BasicEvent-object.
	 * 
	 * @param nextFilter -
	 *            the next Filter, who will handle BasicEvent-object.
	 */
	void setNextFilter(EventFilterI nextFilter);
}
