/* *********************************************************************** *
 * project: org.matsim.*
 * RouteProviderComparator.java
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

package org.matsim.withinday.routeprovider;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two RouteProviders by the return value of their getPriority() method.
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author dgrether
 */
public class RouteProviderComparator implements Comparator<RouteProvider>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(RouteProvider rp1, RouteProvider rp2) {
		if (rp1.getPriority() > rp2.getPriority()) {
			return 1;
		}
		if (rp1.getPriority() < rp2.getPriority()) {
			return -1;
		}
		return 0;
	}
}
