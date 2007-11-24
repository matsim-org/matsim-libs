/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSensorManager.java
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

package org.matsim.trafficmonitoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerLinkLeaveI;


/**
 * This class can be used as EventHandler of the demandmodeling.events.Event.class.
 * If added there as Handler it counts the traffic on each link which was set in the
 * appropriate method of this class.
 *
 * @author dgrether
 *
 */
public class LinkSensorManager implements EventHandlerLinkLeaveI {
	/**
	 * zero
	 */
	private static final Integer ZERO = Integer.valueOf(0);
	/**
	 * maps the ids of the links to integers, which represent the amount of traffic on
	 * the link
	 */
	private Map<String, Integer> linkCountMap;
	/**
	 * Initializes the fields
	 */
	public LinkSensorManager() {
		this.linkCountMap = new HashMap<String, Integer>();
	}
	/**
	 * Adds a traffic sensor to the link with the given id.
	 * @param id
	 */
	public void addLinkSensor(final String id) {
		this.linkCountMap.put(id, ZERO);
	}
	/**
	 * Reads the current traffic count for the Link which id is given as parameter.
	 * When the amount of traffic is read the traffic count is set to zero, i.e. the
	 * number of cars traveling over this link is the number of cars since the last
	 * call of this method.
	 * @param id
	 * @return the  number of cars for link id since the last time the link traffic was queried by this method
	 */
	public int getLinkTraffic(final String id) {
		if (this.linkCountMap.containsKey(id)) {
			int i = this.linkCountMap.get(id).intValue();
			this.linkCountMap.put(id, ZERO);
			return i;
		}
		throw new IllegalArgumentException();
	}
	/**
	 * For each LinkLeaveEvent the corresponding traffic count value is incremented.
	 * @see org.matsim.events.handler.EventHandlerLinkLeaveI#handleEvent(org.matsim.events.EventLinkLeave)
	 */
	public void handleEvent(final EventLinkLeave event) {
		if (this.linkCountMap.containsKey(event.linkId)) {
			int i = this.linkCountMap.get(event.linkId).intValue();
			i++;
			this.linkCountMap.put(event.linkId, Integer.valueOf(i));
		}

	}
	/**
	 * Resets traffic count data for all links to zero
	 * @see org.matsim.events.handler.EventHandlerI#reset(int)
	 */
	public void reset(final int iteration) {

		for (String id : this.linkCountMap.keySet()) {
			this.linkCountMap.put(id, ZERO);
		}
	}

}
