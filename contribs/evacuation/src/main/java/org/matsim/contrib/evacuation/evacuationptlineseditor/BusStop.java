/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.evacuationptlineseditor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class BusStop {
	Id<Link> id;
	protected String hh = "0";
	protected String mm = "1";
	protected Object numDepSpinnerValue = new Integer(1);
	protected Object numVehSpinnerValue = new Integer(1);
	protected boolean circCheckSelected = false;
	protected Object capSpinnerValue = new Integer(1);

	@Override
	public String toString() {
		return this.id + " " + this.hh + " " + this.mm;
	}

}
