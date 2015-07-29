/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.analysis.control;

import org.matsim.core.events.EventsReaderXMLv1;

public class EventReaderThread implements Runnable {
	private final EventsReaderXMLv1 reader;
	private final String eventFile;

	public EventReaderThread(EventsReaderXMLv1 reader, String eventFile) {
		this.reader = reader;
		this.eventFile = eventFile;
	}

	@Override
	public void run() {
		this.reader.parse(this.eventFile);

	}

}
