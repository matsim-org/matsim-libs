/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author  jbischoff
 *
 */
public class TravelTimeStatistics   {

	public static void main(String[] args) {
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw026.100pct/ITERS/it.150/vw026.100pct.150.events.xml.gz";
		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw027.100pct/ITERS/it.180/vw026.100pct.180.events.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();
		TTEventHandler tt = new TTEventHandler();
		events.addHandler(tt);
		new MatsimEventsReader(events).readFile(inputFile);
		tt.printOutput();
	}
	

}
