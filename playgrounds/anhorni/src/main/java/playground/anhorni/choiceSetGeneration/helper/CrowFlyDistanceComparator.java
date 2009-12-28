/* *********************************************************************** *
 * project: org.matsim.*
 * MyComparator.java
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

package playground.anhorni.choiceSetGeneration.helper;

import java.io.Serializable;
import java.util.Comparator;

import org.matsim.api.core.v01.Coord;

public class CrowFlyDistanceComparator implements Comparator<ZHFacility>, Serializable {
	private static final long serialVersionUID = 1L;
	private Coord coord;
	
	public CrowFlyDistanceComparator(Coord coord) {
		this.coord = coord;
	}
	
	public int compare(final ZHFacility f0, final ZHFacility f1) {
		return Double.compare(f0.getCrowFlyDistance(this.coord), f1.getCrowFlyDistance(this.coord));
	}
}
