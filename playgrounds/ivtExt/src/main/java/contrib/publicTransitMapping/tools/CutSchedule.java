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

package contrib.publicTransitMapping.tools;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import contrib.publicTransitMapping.tools.ScheduleCleaner;
import contrib.publicTransitMapping.tools.ScheduleTools;


public class CutSchedule {
	
	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule("mts/fromHafas/ch.xml.gz");

//		ScheduleCleaner.cutSchedule(schedule, Collections.singleton(Id.create(8503000, TransitStopFacility.class)));
		Coord effretikon = new Coord(2693780.0, 1253409.0);
		Coord zurichHB = new Coord(2682830.0, 1248125.0);
		double radius = 5000;

		ScheduleCleaner.cutSchedule(schedule, zurichHB, radius);

		ScheduleTools.writeTransitSchedule(schedule, "mts/fromHafas/test.xml.gz");
	}
}