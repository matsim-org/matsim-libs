package playground.yu.visum.filter;

import org.matsim.events.BasicEvent;
import org.matsim.events.handler.BasicEventHandler;

/**
 * @author  ychen
 */
public class EventFilterAlgorithm implements BasicEventHandler, EventFilterI {
	private EventFilterI nextFilter = null;

	private int count = 0;

	/*----------------------IMPLEMENTS METHODS--------------------*/
	public void setNextFilter(EventFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}

	public void count() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

	public boolean judge(BasicEvent event) {
		return true;
	}

	public void handleEvent(BasicEvent event) {
		count();
		this.nextFilter.handleEvent(event);
	}

	public void reset(int iteration) {
	}
}
