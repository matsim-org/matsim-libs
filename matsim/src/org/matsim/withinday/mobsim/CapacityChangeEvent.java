/* *********************************************************************** *
 * project: org.matsim.*
 * CapacityChangeEvent.java
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

package org.matsim.withinday.mobsim;

import org.matsim.mobsim.queuesim.QueueLink;


/**
 * @author crommel
 * @author dgrether
 *
 */
public class CapacityChangeEvent implements Comparable<CapacityChangeEvent> {

	private double time;

	private QueueLink link;

	private double capactiyScaleFactor;

	public CapacityChangeEvent(final double time, final QueueLink link,
			final double capacityFactor) {
		this.time = time;
		this.link = link;
		this.capactiyScaleFactor = capacityFactor;
	}

	/**
	 * Orders events by time
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final CapacityChangeEvent other) {
		if (this.time > other.time) {
			return 1;
		}
		else if (this.time < other.time) {
			return -1;
		}
		return 0;
	}


	/**
	 * @return the time
	 */
	public double getTime() {
		return this.time;
	}


	/**
	 * @return the link
	 */
	public QueueLink getLink() {
		return this.link;
	}


	/**
	 * @return the capacity
	 */
	public double getCapacityScaleFactor() {
		return this.capactiyScaleFactor;
	}
}
