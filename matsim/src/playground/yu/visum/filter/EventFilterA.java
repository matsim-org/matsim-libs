package playground.yu.visum.filter;

import org.matsim.events.BasicEvent;

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
	/*
	 * (non-Javadoc)
	 *
	 * @see org.matsim.demandmodeling.filters.filter.EventFilterI#judge(org.matsim.demandmodeling.events.BasicEvent)
	 */
	public abstract boolean judge(BasicEvent event);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.matsim.demandmodeling.filters.filter.EventFilterI#handleEvent(org.matsim.demandmodeling.events.BasicEvent)
	 */
	public void handleEvent(BasicEvent event) {
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
