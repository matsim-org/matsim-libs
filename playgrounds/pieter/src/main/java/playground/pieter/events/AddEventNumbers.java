/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.events;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class AddEventNumbers implements EventWriter, BasicEventHandler {
	private BufferedWriter out = null;
	private long eventCounter =0;

	public void reset(int iteration) {
		closeFile();
	}

	public void closeFile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				this.out.close();
				this.out = null;
				Logger.getLogger(this.getClass()).info("Wrote "+eventCounter+" events to file");
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void handleEvent(Event event) {
		
		StringBuilder eventXML = new StringBuilder(180);
		Map<String, String> attr = event.getAttributes();


			eventXML.append("\t<event ");
			eventXML.append("eventNumber=\"");
			eventXML.append(eventCounter);
			eventXML.append("\" ");
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(entry.getValue());
				eventXML.append("\" ");
			}
			eventXML.append(" />\n");
			try {
				this.out.write(eventXML.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			eventCounter++;
		
	}

	public AddEventNumbers(final String outputFilename) {
		init(outputFilename);
	}

	void init(final String outfilename) {
		closeFile();
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args){
		
	}

}
