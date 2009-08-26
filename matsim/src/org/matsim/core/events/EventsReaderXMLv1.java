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

package org.matsim.core.events;

import java.util.Stack;

import org.matsim.core.api.experimental.events.EventsBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class EventsReaderXMLv1 extends MatsimXmlParser {

	static public final String EVENT = "event";
	static public final String EVENTS = "events";

	private final Events events;
	private EventsBuilder builder;

	public EventsReaderXMLv1(final Events events) {
		this.events = events;
		this.builder = events.getBuilder();
		this.setValidating(false);// events-files have no DTD, thus they cannot validate
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
			this.events.processEvent(this.builder.createLinkLeaveEvent(time,
					new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK))));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createLinkEnterEvent(time,
					new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK))));
		} else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityEndEvent(time,
					new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK)),
					atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityStartEvent(time,
					new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_LINK)),
					atts.getValue(ActivityStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (AgentArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentArrivalEvent(time,
					new IdImpl(atts.getValue(AgentArrivalEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentArrivalEvent.ATTRIBUTE_LINK))));
		} else if (AgentDepartureEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentDepartureEvent(time,
					new IdImpl(atts.getValue(AgentDepartureEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentDepartureEvent.ATTRIBUTE_LINK))));
		} else if (AgentWait2LinkEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentWait2LinkEvent(time,
					new IdImpl(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_LINK))));
		} else if (AgentStuckEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentStuckEvent(time,
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_LINK))));
		} else if (AgentMoneyEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentMoneyEvent(time,
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_PERSON)),
					Double.parseDouble(atts.getValue(AgentMoneyEvent.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createPersonEntersVehicleEvent(time,
					new IdImpl(atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE))));			
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createPersonLeavesVehicleEvent(time,
					new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE))));
		} else if (BasicVehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createVehicleArrivesAtFacilityEvent(time,
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY))));			
		} else if (BasicVehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createVehicleDepartsAtFacilityEvent(time,
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY))));			
		}
	}

}
