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

import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.utils.geometry.CoordImpl;

public class FacilitiesSummary {

	public FacilitiesSummary() {
		super();
	}

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		int f_cnt = 0;
		int act_cnt = 0;
		CoordImpl min_coord = new CoordImpl(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		CoordImpl max_coord = new CoordImpl(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
		//            home,work,education,shop,leisure
		int caps[] = {0   ,   0,        0,   0,      0};
		int unlimit_cap_cnt = 0;
		for (Facility f : facilities.getFacilities().values()) {
			f_cnt++;
			if (f.getCenter().getX() > max_coord.getX()) { max_coord.setX(f.getCenter().getX()); }
			if (f.getCenter().getY() > max_coord.getY()) { max_coord.setY(f.getCenter().getY()); }
			if (f.getCenter().getX() < min_coord.getX()) { min_coord.setX(f.getCenter().getX()); }
			if (f.getCenter().getY() < min_coord.getY()) { min_coord.setY(f.getCenter().getY()); }

			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOption a = a_it.next();
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
		System.out.println("      Min Coord:                        " + min_coord.toString());
		System.out.println("      Max Coord:                        " + max_coord.toString());
		System.out.println("      total home cap:                   " + caps[0]);
		System.out.println("      total work cap:                   " + caps[1]);
		System.out.println("      total education cap:              " + caps[2]);
		System.out.println("      total shop cap:                   " + caps[3]);
		System.out.println("      total leisure cap:                " + caps[4]);
		System.out.println("      total acts with unlimited cap:    " + unlimit_cap_cnt);

		System.out.println("    done.");
	}
}
