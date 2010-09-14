/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSetCapacity.java
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

package playground.balmermi.census2000.modules;

import java.util.Iterator;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;

/**
 * <p>
 * <b>MATSim-FUSION Module</b>
 * </p>
 *
 * <p>
 * For each given activity of each given facility, the capacity will be set to 1
 * if no capacity is defined.
 * </p>
 * <p>
 * Log messages:<br>
 * for each <code>home</code> and <code>work</code> activity which the
 * capacity is set to 1, one log line will be written.
 * No log lines are written for other activity types.
 * </p>
 *
 * @author Michael Balmer
 */
public class FacilitiesSetCapacity {

	private static final String WORK = "work";
	private static final String HOME = "home";

	public FacilitiesSetCapacity() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	public void run(ActivityFacilitiesImpl facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> act_it = f.getActivityOptions().values().iterator();
			while (act_it.hasNext()) {
				ActivityOptionImpl activity = (ActivityOptionImpl) act_it.next();
				if ((activity.getCapacity() <= 0) || (activity.getCapacity() == Integer.MAX_VALUE)) {
					activity.setCapacity((double) 1);
					if (HOME.equals(activity.getType())) {
						System.out.println("      Fac id=" + f.getId() + ": home cap undefined. Setting to one.");
					}
					if (WORK.equals(activity.getType())) {
						System.out.println("      Fac id=" + f.getId() + ": work cap undefined. Setting to one.");
					}
				}
			}
		}
		System.out.println("    done.");
	}
}
