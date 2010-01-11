/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.events;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.PersonEventImpl;

import playground.dgrether.events.filters.EventFilter;



/**
 * @author dgrether
 *
 */
public class FilteredEventsManagerImpl extends EventsManagerImpl {

	private List<EventFilter> filters = new ArrayList<EventFilter>();


	public void addFilter(EventFilter filter) {
		this.filters.add(filter);
	}

	/**
	 * Delegates to List.remove() and returns the appropriate value
	 * @param filter
	 * @return the value of List.remove() see interface
	 */
	public boolean removeFilter(EventFilter filter) {
		return this.filters.remove(filter);
	}

	/**
	 * If all filters set in this class are returning true on the
	 * event given as parameter the Events.processEvent() method is called.
	 * Otherwise nothing is done at all.
	 */
	@Override
	public void processEvent(final Event event) {
		if (event instanceof PersonEventImpl) {
			boolean doProcess = true;
			for (EventFilter f : this.filters) {
				if (!f.judge((PersonEventImpl)event)) {
					doProcess = false;
					break;
				}
			}
			if (doProcess) {
				super.processEvent(event);
			}
		}
	}

}
