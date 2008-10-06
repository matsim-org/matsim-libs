/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReaderXMLv1.java
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

package org.matsim.events;

import java.util.Stack;

import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

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
		double time = Double.parseDouble(atts.getValue("time"));
		String vehId = atts.getValue("agent");
		int legNumber = optionalParseInt(atts.getValue("leg"));
		String linkId = atts.getValue("link");
		int activity = optionalParseInt(atts.getValue("activity"));
		String acttype = atts.getValue("act_type");
		int flag = getFlagFromName(atts.getValue("type"));
		String desc = "";
		if ("agentUtility".equals(atts.getValue("type"))) {
			desc = atts.getValue("amount");
		}
		EventsReaderTXTv1.createEvent(this.events, time, vehId, legNumber, linkId, flag, desc, activity, acttype);
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
		AGENT_UTILITY("agentUtility", 9),
		POSITION("position", 999);

		public String name;
		public int value;
		Flags (final String name, final int value) {this.name = name; this.value = value;}
	}

	private static int getFlagFromName(final String flagname) {
		for (Flags flag : Flags.values()) {
			if (flagname.equals(flag.name)) {
				return flag.value;
			}
		}
		throw new RuntimeException("flagname `" + flagname + "' is not known!");
	}

}
