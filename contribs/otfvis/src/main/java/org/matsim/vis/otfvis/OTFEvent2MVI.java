/* *********************************************************************** *
 * project: org.matsim.*
 * OTFEvent2MVI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.SnapshotGenerator;

public class OTFEvent2MVI {

	public static void convert(Scenario scenario, String eventFileName, String outFileName, double interval_s) {
		OTFFileWriter otfFileWriter = new OTFFileWriter(scenario, outFileName);
		EventsManager events = EventsUtils.createEventsManager();
		SnapshotGenerator visualizer = new SnapshotGenerator(scenario.getNetwork(), interval_s, scenario.getConfig().qsim());
		visualizer.addSnapshotWriter(otfFileWriter);
		events.addHandler(visualizer);
		new MatsimEventsReader(events).readFile(eventFileName);
		visualizer.finish();
		otfFileWriter.finish();
	}

}
