/* *********************************************************************** *
 * project: org.matsim.*
 * PhantomAgent2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.calibration_v2;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v3.simulation.floor.Agent2D;

public class PhantomAgent2D extends Agent2D {

	private double lastUpdate;

	private Id currentLinkId = null;

	private final Id id;
	public PhantomAgent2D(Id id) {
		super(null,null,null,null,null);
		this.id = id;
	}

	public void setUpdateTime(double time) {
		this.lastUpdate = time;
	}
	public double getLastUpdate() {
		return this.lastUpdate;
	}

	public Id getCurrentLinkId() {
		return this.currentLinkId;
	}

	public void setCurrentLinkId(Id id) {
		this.currentLinkId = id;
	}

	public Id getId() {
		return this.id;
	}


}
