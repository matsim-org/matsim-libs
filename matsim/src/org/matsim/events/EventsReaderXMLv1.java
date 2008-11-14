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

import org.matsim.basic.v01.IdImpl;
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

	private void startEvent(final Attributes atts) {
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");

		if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkLeaveEvent(time,
					atts.getValue(LinkLeaveEvent.ATTRIBUTE_AGENT),
					atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LEG))));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkEnterEvent(time,
					atts.getValue(LinkEnterEvent.ATTRIBUTE_AGENT),
					atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(LinkEnterEvent.ATTRIBUTE_LEG))));
		} else if (ActEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActEndEvent(time,
					atts.getValue(ActEndEvent.ATTRIBUTE_AGENT),
					atts.getValue(ActEndEvent.ATTRIBUTE_LINK),
					0,
					atts.getValue(ActEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActStartEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActStartEvent(time,
					atts.getValue(ActStartEvent.ATTRIBUTE_AGENT),
					atts.getValue(ActStartEvent.ATTRIBUTE_LINK),
					0,
					atts.getValue(ActStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (AgentArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new AgentArrivalEvent(time,
					atts.getValue(AgentArrivalEvent.ATTRIBUTE_AGENT),
					atts.getValue(AgentArrivalEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(AgentArrivalEvent.ATTRIBUTE_LEG))));
		} else if (AgentDepartureEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new AgentDepartureEvent(time,
					atts.getValue(AgentDepartureEvent.ATTRIBUTE_AGENT),
					atts.getValue(AgentDepartureEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(AgentDepartureEvent.ATTRIBUTE_LEG))));
		} else if (AgentWait2LinkEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new AgentWait2LinkEvent(time,
					atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_AGENT),
					atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_LEG))));
//		} else if (DepartureAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
//			this.events.processEvent(new DepartureAtFacilityEvent(time, null)); // FIXME [MR] replace null
//		} else if (ArrivalAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
//			this.events.processEvent(new ArrivalAtFacilityEvent(time, null)); // FIXME [MR] replace null
		} else if (AgentStuckEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new AgentStuckEvent(time,
					atts.getValue(AgentStuckEvent.ATTRIBUTE_AGENT),
					atts.getValue(AgentStuckEvent.ATTRIBUTE_LINK),
					Integer.parseInt(atts.getValue(AgentStuckEvent.ATTRIBUTE_LEG))));
		} else if (AgentUtilityEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new AgentUtilityEvent(time,
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_AGENT)),
					Double.parseDouble(atts.getValue(AgentUtilityEvent.ATTRIBUTE_AMOUNT))));
		}
	}

}
