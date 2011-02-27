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

package org.matsim.counts.algorithms.graphs.helper;

import java.io.Serializable;
import java.util.Comparator;

public class MyComparator implements Comparator<Comp>, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public int compare(final Comp o1, final Comp o2) {
		return Double.compare(o1.getXval(), o2.getXval());
	}
}
