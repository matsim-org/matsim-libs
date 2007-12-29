/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterTXT.java
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
import java.io.IOException;

import org.matsim.events.BasicEvent;
import org.matsim.events.handler.BasicEventHandlerI;
import org.matsim.utils.io.IOUtils;

public class EventWriterTXT implements BasicEventHandlerI {
	private BufferedWriter out = null;

	public EventWriterTXT(final String filename) {
		init(filename);
	}

	public void closefile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void init(final String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			String eventsTxtFile = "T_GBL\t";
			eventsTxtFile +=  "VEH_ID\t";
			eventsTxtFile +=  "LEG_NR\t";
			eventsTxtFile +=  "LINK_ID\t";
			eventsTxtFile +=  "FROM_NODE_ID\t";
			eventsTxtFile +=  "EVENT_FLAG\t";
			eventsTxtFile +=  "DESCRIPTION\n";
			this.out.write(eventsTxtFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(final int iter) {
		closefile();
	}

	private void writeLine(final String line) {
		try {
			this.out.write(line);
			this.out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(final BasicEvent event) {
		writeLine(event.toString());
	}
}
