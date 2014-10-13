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
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class IncrementEventWriterXML implements EventWriter, BasicEventHandler {
	private BufferedWriter out = null;
	private final Map<String,Double>typeToIncrementMap = new HashMap<>();
	int eventCounter=0;
	double eventIncrement = 1e-9;
	public IncrementEventWriterXML(final String filename) {
		init(filename);
	}
	{
		typeToIncrementMap.put(TransitDriverStartsEvent.EVENT_TYPE, 0.001);
		typeToIncrementMap.put(PersonDepartureEvent.EVENT_TYPE, 0.002);
		typeToIncrementMap.put(PersonEntersVehicleEvent.EVENT_TYPE, 0.003);
		typeToIncrementMap.put(VehicleArrivesAtFacilityEvent.EVENT_TYPE, 0.005);
		typeToIncrementMap.put(VehicleDepartsAtFacilityEvent.EVENT_TYPE, 0.006);
	}
	/**
	 * Constructor so you can pass System.out or System.err to the writer to see the result on the console.
	 * 
	 * @param stream
	 */
	public IncrementEventWriterXML(final PrintStream stream ) {
		this.out = new BufferedWriter(new OutputStreamWriter(stream));
		try {
			this.out.write("<events>\n");
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

	void init(final String outfilename) {
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
		try {
			this.out.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				this.out.append(entry.getKey());
				this.out.append("=\"");
				if(typeToIncrementMap.keySet().contains(event.getEventType()) && entry.getKey().equals("time")){
					double time = Double.parseDouble(encodeAttributeValue(entry.getValue()));
					time += typeToIncrementMap.get(event.getEventType());
					this.out.append(encodeAttributeValue(Double.toString(time)));
				}else{
					
					this.out.append(encodeAttributeValue(entry.getValue()));
				}
				this.out.append("\" ");
			}
			this.out.append(" />\n");
		} catch (IOException e) {
			e.printStackTrace();
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
			StringBuilder bf = new StringBuilder();
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
