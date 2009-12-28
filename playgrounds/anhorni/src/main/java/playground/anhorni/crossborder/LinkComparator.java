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

package playground.anhorni.crossborder;

import java.io.Serializable;
import java.util.Comparator;

public class LinkComparator implements Comparator<MyLink>, Serializable {
	private static final long serialVersionUID = 1L;

	
	// Capacity has to be compared. But volume is assigned proportional to capacity.
	// Thus volume can be used.
	public int compare(final MyLink o1, final MyLink o2) {
		return Double.compare(o2.getVolume(), o1.getVolume());
	}
}
