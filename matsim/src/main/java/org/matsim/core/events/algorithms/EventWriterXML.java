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
import java.util.Map;

public class EventWriterXML implements EventWriter, BasicEventHandler {

	private static final Logger LOG = LogManager.getLogger(EventWriterXML.class);
	private final BufferedWriter out;

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
			this.out.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				this.out.append(entry.getKey());
				this.out.append("=\"");
				this.out.append(encodeAttributeValue(entry.getValue()));
				this.out.append("\" ");
			}
			this.out.append(" />\n");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	// the following method was taken from MatsimXmlWriter in order to correctly encode attributes, but
	// to forego the overhead of using the full MatsimXmlWriter.
	/**
	 * Encodes the given string in such a way that it no longer contains
	 * characters that have a special meaning in xml.
	 *
	 * @see <a href="http://www.w3.org/International/questions/qa-escapes#use">http://www.w3.org/International/questions/qa-escapes#use</a>
	 * @param attributeValue
	 * @return String with some characters replaced by their xml-encoding.
	 */
	private String encodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		int len = attributeValue.length();
		boolean encode = false;
		for (int pos = 0; pos < len; pos++) {
			char ch = attributeValue.charAt(pos);
			if (ch == '<') {
				encode = true;
				break;
			} else if (ch == '>') {
				encode = true;
				break;
			} else if (ch == '\"') {
				encode = true;
				break;
			} else if (ch == '&') {
				encode = true;
				break;
			}
		}
		if (encode) {
			StringBuilder bf = new StringBuilder(attributeValue.length() + 30);
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					bf.append("&lt;");
				} else if (ch == '>') {
					bf.append("&gt;");
				} else if (ch == '\"') {
					bf.append("&quot;");
				} else if (ch == '&') {
					bf.append("&amp;");
				} else {
					bf.append(ch);
				}
			}

			return bf.toString();
		}
		return attributeValue;

	}

}
