/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesDefineCapAndOpentime.java
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

import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTimeImpl;

public class FacilitiesDefineCapAndOpentime {

	private final double TIME_0000 = Time.parseTime("00:00");
	private final double TIME_0800 = Time.parseTime("08:00");
	private final double TIME_0830 = Time.parseTime("08:30");
	private final double TIME_0900 = Time.parseTime("09:00");
	private final double TIME_1200 = Time.parseTime("12:00");
	private final double TIME_1300 = Time.parseTime("13:00");
	private final double TIME_1330 = Time.parseTime("13:30");
	private final double TIME_1700 = Time.parseTime("17:00");
	private final double TIME_1800 = Time.parseTime("18:00");
	private final double TIME_2000 = Time.parseTime("20:00");
	private final double TIME_2400 = Time.parseTime("24:00");
	
	private final int nof_persons;

	public FacilitiesDefineCapAndOpentime(final int nof_persons) {
		super();
		this.nof_persons = nof_persons;
		if (nof_persons <= 0) {
			throw new RuntimeException("[nof_persons=" + nof_persons + " not allowed]");
		}
	}

	public void run(ActivityFacilitiesImpl facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		int home_cnt = 0;
		int work_cnt = 0;
		int educ_cnt = 0;
		int shop_cnt = 0;
		int leis_cnt = 0;

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<String> at_it = f.getActivityOptions().keySet().iterator();
			while (at_it.hasNext()) {
				String at = at_it.next();
				if (at.equals("home")) { home_cnt++; }
				else if (at.equals("work")) { work_cnt++; }
				else if (at.equals("education")) { educ_cnt++; }
				else if (at.equals("shop")) { shop_cnt++; }
				else if (at.equals("leisure")) { leis_cnt++; }
				else { throw new RuntimeException("[something is wrong!]"); }
			}
		}
		System.out.println("      home_cnt = " + home_cnt);
		System.out.println("      work_cnt = " + work_cnt);
		System.out.println("      educ_cnt = " + educ_cnt);
		System.out.println("      shop_cnt = " + shop_cnt);
		System.out.println("      leis_cnt = " + leis_cnt);

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<? extends ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOptionImpl a = (ActivityOptionImpl) a_it.next();
				if (a.getType().equals("home")) {
					a.setCapacity(this.nof_persons/home_cnt);
					a.addOpeningTime(new OpeningTimeImpl(TIME_0000, TIME_2400));
				}
				else if (a.getType().equals("work")) {
					a.setCapacity(this.nof_persons/work_cnt);
					a.addOpeningTime(new OpeningTimeImpl(TIME_0800, TIME_1800));
				}
				else if (a.getType().equals("education")) {
					a.setCapacity(this.nof_persons/educ_cnt);
					a.addOpeningTime(new OpeningTimeImpl(TIME_0800, TIME_1200));
					a.addOpeningTime(new OpeningTimeImpl(TIME_1300, TIME_1700));
				}
				else if (a.getType().equals("shop")) {
					a.setCapacity(this.nof_persons/shop_cnt);
					a.addOpeningTime(new OpeningTimeImpl(TIME_0830, TIME_1200));
					a.addOpeningTime(new OpeningTimeImpl(TIME_1330, TIME_2000));
					a.addOpeningTime(new OpeningTimeImpl(TIME_0900, TIME_1700));
				}
				else if (a.getType().equals("leisure")) {
					a.setCapacity(this.nof_persons/leis_cnt);
					a.addOpeningTime(new OpeningTimeImpl(TIME_1700, TIME_2400));
					a.addOpeningTime(new OpeningTimeImpl(TIME_2000, TIME_2400));
				}
				else { throw new RuntimeException("[something is wrong!]"); }
			}
		}

		System.out.println("    done.");
	}
}
