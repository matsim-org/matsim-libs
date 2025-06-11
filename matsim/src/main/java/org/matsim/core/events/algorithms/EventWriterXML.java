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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class EventWriterXML implements EventWriter, BasicEventHandler {

	private static final Logger LOG = LogManager.getLogger(EventWriterXML.class);
	private final BufferedWriter out;

	/**
	 * Cache for StringBuilder instances to avoid creating a new one for each event.
	 */
	private final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(StringBuilder::new);

	public EventWriterXML(final String outfilename) {
		this.out = IOUtils.getBufferedWriter(outfilename);
		this.writeHeader();
	}

	/**
	 * Constructor so you can pass System.out or System.err to the writer to see the result on the console.
	 *
	 * @param stream
	 */
	public EventWriterXML(final OutputStream stream ) {
		this.out = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
		this.writeHeader();
	}

	private void writeHeader() {
		try {
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void closeFile() {
		try {
			this.out.write("</events>");
			// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
			// effects anywhere else.  kai, oct'12
			// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
			// checksums of event files.  Removed that change again.  kai, oct'12
			this.out.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void reset(final int iter) {
	}

	@Override
	public void handleEvent(final Event event) {
		try {
			StringBuilder b = stringBuilder.get();

			b.setLength(0);
			event.writeAsXML(b);
			this.out.append(b);

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
