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

package org.matsim.core.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimComparator;

import java.io.Serializable;
import java.util.*;

/**
 * A container class for events that represent time-variant changes for link
 * attributes (in SI units).
 * 
 * @author illenberger
 * 
 */
public class NetworkChangeEvent {

	public static enum ChangeType {
		ABSOLUTE, FACTOR
	}

	public static class ChangeValue {

		private final ChangeType type;

		private final double value;

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

	private final List<Link> links = new ArrayList<>();
	
	private final List<Link> unmodifiableLinks = Collections.unmodifiableList(links);

	private final double startTime;

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
	/*package*/ NetworkChangeEvent(double startTime) {
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
	 * @param links a collection of links affected by this event.
	 */
	public void addLinks(Collection<? extends Link> links) {
		this.links.addAll(links);
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

	public static class StartTimeComparator implements Comparator<NetworkChangeEvent>, Serializable, MatsimComparator {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(NetworkChangeEvent o1, NetworkChangeEvent o2) {
			return Double.compare(o1.getStartTime(), o2.getStartTime());
		}
	}
}
