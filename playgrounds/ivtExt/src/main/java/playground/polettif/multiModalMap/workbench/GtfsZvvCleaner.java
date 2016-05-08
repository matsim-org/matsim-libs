/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.workbench;

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.multiModalMap.tools.ScheduleCleaner;
import playground.polettif.multiModalMap.tools.ScheduleTools;

public class GtfsZvvCleaner {
	
	public static void main(final String[] args) {
		String in = "C:/Users/polettif/Desktop/data/mts/unmapped/fromGtfs/zvv_clean.xml";
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(in);

		ScheduleCleaner.removeNotUsedStopFacilities(schedule);

		ScheduleTools.writeTransitSchedule(schedule, in);
	}
	
}