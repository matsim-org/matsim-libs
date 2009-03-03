/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOpentimesKTIYear1.java
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

package playground.meisterk.org.matsim.facilities.algorithms;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.OpeningTime;

public class FacilitiesOpentimesKTIYear1 {

	private TreeMap<String, OpeningTime> openingTimes = new TreeMap<String, OpeningTime>();
	
	private final static Logger log = Logger.getLogger(FacilitiesOpentimesKTIYear1.class);

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		this.loadOpeningTimes();

		for (Facility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {

				ActivityOption a = a_it.next();
				String actType = a.getType();

				// delete all existing open times info
				Map<DayType, SortedSet<BasicOpeningTime>> o = a.getOpentimes();
				o.clear();

				if (openingTimes.containsKey(actType)) {
					a.addOpeningTime(openingTimes.get(actType));
				} else {
					log.warn("For activity type " + actType + " no opening time is defined");
				}
			}
		}
		System.out.println("    done.");
	}

	private void loadOpeningTimes() {
		openingTimes.put("work", new OpeningTimeImpl(DayType.wkday, "7:00", "18:00"));
		openingTimes.put("shop", new OpeningTimeImpl(DayType.wkday, "8:00", "20:00"));
		openingTimes.put("education", new OpeningTimeImpl(DayType.wkday, "7:00", "18:00"));
		openingTimes.put("leisure", new OpeningTimeImpl(DayType.wkday, "6:00", "24:00"));
	}
}
