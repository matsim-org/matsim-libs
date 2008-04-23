/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEvent.java
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

/**
 * 
 */
package org.matsim.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.utils.misc.Time;

/**
 * A container class for events that represent time-variant changes for link
 * attributes.
 * 
 * @author illenberger
 * 
 */
public class NetworkChangeEvent implements Comparable{

	public static enum ChangeType {
		ABSOLUTE, FACTOR
	}

	public static class ChangeValue {

		private ChangeType type;

		private double value;

		public ChangeValue(ChangeType type, double value) {
			this.type = type;
			this.value = value;
		}

		public ChangeType getType() {
			return type;
		}

		public double getValue() {
			return value;
		}
	}

	// ========================================================================
	// private members
	// ========================================================================

	private List<Link> links = new ArrayList<Link>();
	
	private List<Link> unmodifiableLinks = Collections.unmodifiableList(links);

	private double startTime;

	private ChangeValue flowCapacityChange = null;

	private ChangeValue freespeedChange = null;

	private ChangeValue lanesChange = null;

	// ========================================================================
	// constructor
	// ========================================================================

	/**
	 * Creates an empty network change event with specified start time.
	 * 
	 * @param startTime
	 *            the time at which the event occurs.
	 */
	public NetworkChangeEvent(double startTime) {
		this.startTime = startTime;
	}

	// ========================================================================
	// accessors
	// ========================================================================

	/**
	 * 
	 * @return the time at which the event occurs.
	 */
	public double getStartTime() {
		return startTime;
	}


	/**
	 * 
	 * @param link a link that is affected by this event.
	 */
	public void addLink(Link link) {
		links.add(link);
	}
	
	/**
	 * 
	 * @param link the link to remove.
	 */
	public void removeLink(Link link) {
		links.remove(link);
	}
	
	/**
	 * 
	 * @return a read-only view of the links that are affected by this event.
	 */
	public Collection<Link> getLinks() {
		return unmodifiableLinks;
	}

	/**
	 * 
	 * @return the flow capacity changes or <tt>null</tt> if flow capacity
	 *         should not be changed.
	 */
	public ChangeValue getFlowCapacityChange() {
		return flowCapacityChange;
	}

	/**
	 * 
	 * @param flowCapacityChange
	 *            the flow capacity changes.
	 */
	public void setFlowCapacityChange(ChangeValue flowCapacityChange) {
		this.flowCapacityChange = flowCapacityChange;
	}

	/**
	 * 
	 * @return the free-speed changes or <tt>null</tt> if free-speed should
	 *         not be changed.
	 */
	public ChangeValue getFreespeedChange() {
		return freespeedChange;
	}

	/**
	 * 
	 * @param freespeedChange
	 *            the free-speed changes.
	 */
	public void setFreespeedChange(ChangeValue freespeedChange) {
		this.freespeedChange = freespeedChange;
	}

	/**
	 * 
	 * @return the lanes changes or <tt>null</tt> if the number of lanes
	 *         should not be changed.
	 */
	public ChangeValue getLanesChange() {
		return lanesChange;
	}

	/**
	 * 
	 * @param lanesChange
	 *            the lanes changes.
	 */
	public void setLanesChange(ChangeValue lanesChange) {
		this.lanesChange = lanesChange;
	}

	public int compareTo(Object o) {
		if (this.startTime < ((NetworkChangeEvent) o).getStartTime()) {
			return -1;
		} else if (this.startTime > ((NetworkChangeEvent) o).getStartTime()){
			return 1;
		}
		return 0;
	}
}
