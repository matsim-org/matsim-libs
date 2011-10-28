/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.fhuelsmann.noise;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

public class NoiseWriter {
	private static final Logger logger = Logger.getLogger(NoiseWriter.class);
	private static String runDirectory = "../../run981/";

	public void writeEvents(final List<NoiseEventImpl> list) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		String outputfile = runDirectory + "noiseEvents.xml";
		EventWriterXML eWriter = new EventWriterXML(outputfile);
		eventsManager.addHandler(eWriter);
		for (NoiseEventImpl event : list) {
			eventsManager.processEvent(event);
		}

		eWriter.closeFile();
		logger.info("Finished writing output to " + outputfile);
	}

}
