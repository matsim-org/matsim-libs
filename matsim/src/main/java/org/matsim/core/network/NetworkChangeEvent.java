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
public final class NetworkChangeEvent {
	
	public enum ChangeType {
		ABSOLUTE_IN_SI_UNITS, FACTOR, OFFSET_IN_SI_UNITS
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ChangeValue that = (ChangeValue) o;

			if (Double.compare(that.value, value) != 0) return false;
			return type == that.type;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = type.hashCode();
			temp = Double.doubleToLongBits(value);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
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
	 * @param links1 a collection of links affected by this event.
	 */
	public void addLinks(Collection<? extends Link> links1) {
		this.links.addAll(links1);
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
	 *            the flow capacity changes in veh/s (!!), i.e. the unit which LinkImpl wants,
	 *            not the unit in network.xml
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NetworkChangeEvent that = (NetworkChangeEvent) o;

		if (Double.compare(that.startTime, startTime) != 0) return false;
		if (!links.equals(that.links)) return false;
		if (flowCapacityChange != null ? !flowCapacityChange.equals(that.flowCapacityChange) : that.flowCapacityChange != null)
			return false;
		if (freespeedChange != null ? !freespeedChange.equals(that.freespeedChange) : that.freespeedChange != null)
			return false;
		return lanesChange != null ? lanesChange.equals(that.lanesChange) : that.lanesChange == null;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = links.hashCode();
		temp = Double.doubleToLongBits(startTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (flowCapacityChange != null ? flowCapacityChange.hashCode() : 0);
		result = 31 * result + (freespeedChange != null ? freespeedChange.hashCode() : 0);
		result = 31 * result + (lanesChange != null ? lanesChange.hashCode() : 0);
		return result;
	}
}
