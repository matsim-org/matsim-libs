package playground.yu.visum.filter;

import org.matsim.core.api.experimental.events.Event;

/**
 * @author ychen
 */
public abstract class EventFilterA extends Filter implements EventFilterI {

	/*
	 * -------------------------MEMBER VARIABLES----------------
	 */
	private EventFilterI nextFilter = null;

	/*
	 * ------------------------SETTER------------------------------
	 */

	/**
	 * sets the next EventFilterA-Object
	 *
	 * @param nextFilter -
	 *            The nextFilter to set.
	 */
	public void setNextFilter(EventFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}

	/*
	 * ------------------------IMPLEMENTS METHODS-----------------------
	 */
	public abstract boolean judge(Event event);

	public void handleEvent(Event event) {
		if (judge(event)) {
			count();
			this.nextFilter.handleEvent(event);
		}
	}

	/**
	 * @return Returns the result.
	 */
	protected boolean isResult() {
		return false; // subclass may overwrite this to return true
	}
}
