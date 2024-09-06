/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSummary.java
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

package org.matsim.facilities.algorithms;

import java.util.Iterator;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;

public class FacilitiesSummary {

	public FacilitiesSummary() {
		super();
	}

	public void run(ActivityFacilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		int f_cnt = 0;
		int act_cnt = 0;
//		Coord min_coord = new Coord(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
//		Coord max_coord = new Coord(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		double min_coordX = Double.POSITIVE_INFINITY;
		double min_coordY = Double.POSITIVE_INFINITY;
		double max_coordX = Double.NEGATIVE_INFINITY;
		double max_coordY = Double.NEGATIVE_INFINITY;
		//            home,work,education,shop,leisure
		int caps[] = {0   ,   0,        0,   0,      0};
		int unlimit_cap_cnt = 0;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			f_cnt++;
			if (f.getCoord().getX() > max_coordX) { max_coordX=f.getCoord().getX(); }
			if (f.getCoord().getY() > max_coordY) { max_coordY=f.getCoord().getY(); }
			if (f.getCoord().getX() < min_coordX) { min_coordX=f.getCoord().getX(); }
			if (f.getCoord().getY() < min_coordY) { min_coordY=f.getCoord().getY(); }

			Iterator<? extends ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOptionImpl a = (ActivityOptionImpl) a_it.next();
				act_cnt++;
				if (a.getCapacity() != Integer.MAX_VALUE) {
					if (a.getType().equals("home")) {
						caps[0] += a.getCapacity();
					}
					else if (a.getType().equals("work")) {
						caps[1] += a.getCapacity();
					}
					else if (a.getType().equals("education")) {
						caps[2] += a.getCapacity();
					}
					else if (a.getType().equals("shop")) {
						caps[3] += a.getCapacity();
					}
					else if (a.getType().equals("leisure")) {
						caps[4] += a.getCapacity();
					}
					else {
						throw new RuntimeException("ERROR: in " + this.getClass().getName() +
															 " in run(Facilities facilities):" +
															 " do not know type = " + a.getType());
					}
				}
				else {
					unlimit_cap_cnt++;
				}
			}
		}
		System.out.println("      Number of Facilities:             " + f_cnt);
		System.out.println("      Number of Activities:             " + act_cnt);
		System.out.println("      Min Coord:                        " + min_coordX + " " + min_coordY );
		System.out.println("      Max Coord:                        " + max_coordX + " " + max_coordY ) ;
		System.out.println("      total home cap:                   " + caps[0]);
		System.out.println("      total work cap:                   " + caps[1]);
		System.out.println("      total education cap:              " + caps[2]);
		System.out.println("      total shop cap:                   " + caps[3]);
		System.out.println("      total leisure cap:                " + caps[4]);
		System.out.println("      total acts with unlimited cap:    " + unlimit_cap_cnt);

		System.out.println("    done.");
	}
}
