/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReaderXMLv1.java
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

package org.matsim.events;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EventsReaderXMLv1 extends MatsimXmlParser {

	static public final String EVENT = "event";
	static public final String EVENTS = "events";

	private final Events events;

	public EventsReaderXMLv1(final Events events) {
		this.events = events;
		this.setValidating(false);// events-files have not DTD, thus they cannot validate
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
	}


	private int optionalParseInt(final String input) {
		if (input == null) return -1;
		int result = -1;
		try {
			result = Integer.parseInt(input);
		} catch (NumberFormatException e ) {
			return -1;
		}
		return result;
	}

	private void startEvent(final Attributes atts) {
		int time = Integer.parseInt(atts.getValue("time"));
		String vehId = atts.getValue("agent");
		int legNumber = optionalParseInt(atts.getValue("leg"));
		String linkId = atts.getValue("link");
		int activity = optionalParseInt(atts.getValue("activity"));
		String acttype = atts.getValue("act_type");
		int nodeId = 0; //Integer.parseInt(atts.getValue("fromNode"));
		int flag = getFlagFromName(atts.getValue("type"));
		String desc = "";
		EventsReaderTXTv1.createEvent(this.events, time, vehId, legNumber, linkId, nodeId, flag, desc, activity, acttype);
	}

	/**
	 * Parses the specified events file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//////////////////////////////////////////////////////////////////////
	// static methods
	//////////////////////////////////////////////////////////////////////
	private enum Flags {
		ARRIVAL("arrival", 0),
		LEAVE_LINK("left link", 2),
		STUCK("stuckAndAbort", 3),
		WAIT_TO_LINK("wait2link", 4),
		ENTER_LINK("entered link", 5),
		DEPARTURE("departure", 6),
		ACTSTART("actstart", 7),
		ACTEND("actend", 8),
		POSITION("position", 999);

		public String name;
		public int value;
		Flags (final String name, final int value) {this.name = name; this.value = value;};
	}

	private static int getFlagFromName(final String flagname) {
		if (flagname.equals(Flags.ARRIVAL.name)) {
			return Flags.ARRIVAL.value;
		} else if (flagname.equals(Flags.DEPARTURE.name)) {
			return Flags.DEPARTURE.value;
		} else if (flagname.equals(Flags.ENTER_LINK.name)) {
			return Flags.ENTER_LINK.value;
		} else if (flagname.equals(Flags.LEAVE_LINK.name)) {
			return Flags.LEAVE_LINK.value;
		} else if (flagname.equals(Flags.POSITION.name)) {
			return Flags.POSITION.value;
		} else if (flagname.equals(Flags.STUCK.name)) {
			return Flags.STUCK.value;
		} else if (flagname.equals(Flags.WAIT_TO_LINK.name)) {
			return Flags.WAIT_TO_LINK.value;
		} else if (flagname.equals(Flags.ACTSTART.name)) {
			return Flags.ACTSTART.value;
		} else if (flagname.equals(Flags.ACTEND.name)) {
			return Flags.ACTEND.value;
		} else {
			Gbl.errorMsg("flagname `" + flagname + "' is not known!");
			return -1;
		}
	}

	//////////////////////////////////////////////////////////////////////

}
