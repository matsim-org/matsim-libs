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

import org.matsim.utils.identifiers.IdI;


/**
 * @author dgrether
 *
 */
public class SignalLane {

	private IdI id;

	private IdI linkId;

	private double length = Double.NaN;

	private boolean isMixedLane;

	public SignalLane(IdI laneId, IdI linkId) {
		this.id = laneId;
		this.linkId = linkId;
	}

	public IdI getId() {
		return this.id;
	}

	public IdI getLinkId() {
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
