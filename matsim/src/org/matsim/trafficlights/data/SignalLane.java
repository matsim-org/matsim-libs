/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.trafficlights.data;

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalLane {

	private Id id;

	private Id linkId;

	private double length = Double.NaN;

	private boolean isMixedLane;

	public SignalLane(Id laneId, Id linkId) {
		this.id = laneId;
		this.linkId = linkId;
	}

	public Id getId() {
		return this.id;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public double getLength() {
		return this.length;
	}

	public void setLength(double l) {
		this.length = l;
	}

	public void setMixedLane(boolean mixed) {
		this.isMixedLane = mixed;
	}

	public boolean isMixedLane() {
		return this.isMixedLane;
	}

}
