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

package org.matsim.core.events.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Map;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class EventWriterXML implements EventWriter, BasicEventHandler {
	private BufferedWriter out = null;

	public EventWriterXML(final String filename) {
		init(filename);
	}
	/**Constructor so you can pass System.out or System.err to the writer to see the result on the console.
	 * 
	 * @param stream
	 */
	public EventWriterXML(final PrintStream stream ) {
		this.out = new BufferedWriter(new OutputStreamWriter(stream)) ;
		try {
			this.out.write("<events>\n") ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void closeFile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
				// effects anywhere else.  kai, oct'12
				// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
				// checksums of event files.  Removed that change again.  kai, oct'12
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void init(final String outfilename) {
		closeFile();

		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(final int iter) {
		closeFile();
	}

	@Override
	public void handleEvent(final Event event) {
		StringBuilder eventXML = new StringBuilder(180);
		eventXML.append("\t<event ");
		Map<String, String> attr = event.getAttributes();
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
