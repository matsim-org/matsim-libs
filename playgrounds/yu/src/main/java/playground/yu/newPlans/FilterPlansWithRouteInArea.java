/* *********************************************************************** *
 * project: org.matsim.*
 * FilterPlansWithRouteInArea.java
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

package playground.yu.newPlans;

import playground.mrieser.MyRuns;

public class FilterPlansWithRouteInArea {
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		MyRuns.filterPlansWithRouteInArea(
				new String[] { "../data/ivtch/make100pctZrh30kmPlans.xml" },
				683518.0, 246836.0, 30000.0);
	}
}
