/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsFileReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.events;

import java.util.Stack;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.PersonEventImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * Events file reader that handles xyzAzimuth, link leave, link enter, departure
 * and arrival events its a lot of copy and paste code from EventsReaderXMLv1
 * The xyzAzimuth part is to be merged into EventsReaderXMLv1 some day
 * 
 * @author laemmel
 * 
 */
@Deprecated
public class XYZEventsFileReader extends MatsimXmlParser {

	private final EventsManager events;
	private final XYZAzimuthEventsFactoryImpl builder;

	public XYZEventsFileReader(EventsManager events) {
		this.events = events;
		this.builder = new XYZAzimuthEventsFactoryImpl(events.getFactory());
		setValidating(false);// events-files have no DTD, thus they cannot
		// validate
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (EventsReaderXMLv1.EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	/**
	 * @param atts
	 */
	private void startEvent(Attributes atts) {
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");
		if (LinkLeaveEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.getFactory().createLinkLeaveEvent(time, new IdImpl(atts.getValue(LinkLeaveEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(LinkLeaveEventImpl.ATTRIBUTE_LINK))));
		} else if (LinkEnterEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.getFactory().createLinkEnterEvent(time, new IdImpl(atts.getValue(LinkEnterEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(LinkEnterEventImpl.ATTRIBUTE_LINK))));
		} else if (ActivityEndEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.getFactory().createActivityEndEvent(time, new IdImpl(atts.getValue(ActivityEndEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(ActivityEndEventImpl.ATTRIBUTE_LINK)), atts.getValue(ActivityEndEventImpl.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityEndEventImpl.ATTRIBUTE_FACILITY)), atts.getValue(ActivityEndEventImpl.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.getFactory().createActivityStartEvent(time, new IdImpl(atts.getValue(ActivityStartEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(ActivityStartEventImpl.ATTRIBUTE_LINK)), atts.getValue(ActivityStartEventImpl.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityStartEventImpl.ATTRIBUTE_FACILITY)), atts.getValue(ActivityStartEventImpl.ATTRIBUTE_ACTTYPE)));
		} else if (AgentArrivalEventImpl.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(AgentArrivalEventImpl.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(this.builder.getFactory().createAgentArrivalEvent(time, new IdImpl(atts.getValue(AgentArrivalEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(AgentArrivalEventImpl.ATTRIBUTE_LINK)), mode));
		} else if (AgentDepartureEventImpl.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(AgentDepartureEventImpl.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(this.builder.getFactory().createAgentDepartureEvent(time, new IdImpl(atts.getValue(AgentDepartureEventImpl.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(AgentDepartureEventImpl.ATTRIBUTE_LINK)), mode));
		} else if (XYZAzimuthEventImpl.EVENT_TYPE.equals(eventType)) {
			String x = atts.getValue(XYZAzimuthEventImpl.ATTRIBUTE_X);
			String y = atts.getValue(XYZAzimuthEventImpl.ATTRIBUTE_Y);
			String z = atts.getValue(XYZAzimuthEventImpl.ATTRIBUTE_Z);
			String azimuth = atts.getValue(XYZAzimuthEventImpl.ATTRIBUTE_AZIMUTH);
			String id = atts.getValue(PersonEventImpl.ATTRIBUTE_PERSON);
			Event e = this.builder.createXYZAzimuthEvent(x, y, z, azimuth, id, time);
			this.events.processEvent(e);
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

	}
}
