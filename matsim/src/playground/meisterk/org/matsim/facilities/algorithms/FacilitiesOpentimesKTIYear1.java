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
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.OpeningTime;

public class FacilitiesOpentimesKTIYear1 {

	private TreeMap<String, OpeningTime> openingTimes = new TreeMap<String, OpeningTime>();
	
	private final static Logger log = Logger.getLogger(FacilitiesOpentimesKTIYear1.class);

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		this.loadOpeningTimes();

		for (Facility f : facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {

				Activity a = a_it.next();
				String actType = a.getType();

				// delete all existing open times info
				TreeMap<String, TreeSet<OpeningTime>> o = a.getOpentimes();
				o.clear();

				if (openingTimes.containsKey(actType)) {
					a.addOpentime(openingTimes.get(actType));
				} else {
					log.warn("For activity type " + actType + " no opening time is defined");
				}
			}
		}
		System.out.println("    done.");
	}

	private void loadOpeningTimes() {
		openingTimes.put("work", new OpeningTime("wkday", "7:00", "18:00"));
		openingTimes.put("shop", new OpeningTime("wkday", "8:00", "20:00"));
		openingTimes.put("education", new OpeningTime("wkday", "7:00", "18:00"));
		openingTimes.put("leisure", new OpeningTime("wkday", "6:00", "24:00"));
	}
}
