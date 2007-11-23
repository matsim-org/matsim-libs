/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
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

package org.matsim.events.algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.events.BasicEvent;
import org.matsim.events.handler.BasicEventHandlerI;
import org.xml.sax.Attributes;

public class EventWriterXML implements BasicEventHandlerI {
	private BufferedWriter out = null;

	public EventWriterXML(final String filename) {
		init(filename);
	}

	public void closefile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void init(final String outfilename)
	{
		closefile();

		try {
			this.out = new BufferedWriter( new FileWriter (outfilename));
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(final int iter) {
		closefile();
	}

	public void handleEvent(final BasicEvent event) {
		StringBuilder eventXML = new StringBuilder("\t<event ");
		Attributes attr = event.getAttributes();
		for (int i = 0; i< attr.getLength(); i++) {
			eventXML.append(attr.getQName(i));
			eventXML.append("=\"");
			eventXML.append(attr.getValue(i));
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
