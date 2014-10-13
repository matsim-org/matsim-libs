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

package playground.pieter.singapore.utils.events.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class TrimEventWriterHITS implements EventWriter, BasicEventHandler {
	private BufferedWriter out = null;

	public void reset(int iteration) {
		closeFile();
	}

	public void closeFile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void handleEvent(Event event) {
		StringBuilder eventXML = new StringBuilder(180);
		Map<String, String> attr = event.getAttributes();
		String pax_idx = attr.get("person");
		// the filter, in this case, show the first 2/3/4 of each instantiation
		// of a HITS person
		if (pax_idx == null
				|| (pax_idx.endsWith("_1") || pax_idx.endsWith("_2")
						|| pax_idx.endsWith("_3") || pax_idx.endsWith("_4")
						|| pax_idx.endsWith("_5") || pax_idx.startsWith("pt"))) {

			eventXML.append("\t<event ");
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
		}
	}

	public TrimEventWriterHITS(final String filename, ArrayList<String> filter) {
		init(filename);
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
