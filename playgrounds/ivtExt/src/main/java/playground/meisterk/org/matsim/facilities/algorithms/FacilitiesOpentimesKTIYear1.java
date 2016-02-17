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

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.*;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;

public class FacilitiesOpentimesKTIYear1 {

	private TreeMap<String, OpeningTimeImpl> openingTimes = new TreeMap<String, OpeningTimeImpl>();
	
	private final static Logger log = Logger.getLogger(FacilitiesOpentimesKTIYear1.class);

	public void run(ActivityFacilitiesImpl facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		this.loadOpeningTimes();

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {

				ActivityOptionImpl a = (ActivityOptionImpl) a_it.next();
				String actType = a.getType();

				// delete all existing open times info
				SortedSet<OpeningTime> o = a.getOpeningTimes();
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
		openingTimes.put("work", new OpeningTimeImpl(Time.parseTime("7:00"), Time.parseTime("18:00")));
		openingTimes.put("shop", new OpeningTimeImpl(Time.parseTime("8:00"), Time.parseTime("20:00")));
		openingTimes.put("education", new OpeningTimeImpl(Time.parseTime("7:00"), Time.parseTime("18:00")));
		openingTimes.put("leisure", new OpeningTimeImpl(Time.parseTime("6:00"), Time.parseTime("24:00")));
	}
}
