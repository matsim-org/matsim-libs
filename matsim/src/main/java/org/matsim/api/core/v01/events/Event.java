/* *********************************************************************** *
 * project: org.matsim.*
 * Event.java
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

package org.matsim.api.core.v01.events;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.core.utils.io.XmlUtils;

import static org.matsim.core.utils.io.XmlUtils.writeEncodedAttributeValue;

public abstract class Event {

	public final static String ATTRIBUTE_TIME = "time";
	public final static String ATTRIBUTE_TYPE = "type";
	public static final String ATTRIBUTE_X = "x" ;
	public static final String ATTRIBUTE_Y = "y" ;

	private double time;

	public Event(final double time) {
		this.time = time;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<>();
		attr.put(ATTRIBUTE_TIME, Double.toString(this.time));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		if (this instanceof HasPersonId hasPersonId && hasPersonId.getPersonId() != null) {
			attr.put(HasPersonId.ATTRIBUTE_PERSON, hasPersonId.getPersonId().toString());
			// many derived types do this by themselves, for historical reasons.  Since the information is put into a map, it still exists only once under that key.  kai,
			// mar'19
		}
		if (this instanceof HasFacilityId hasFacilityId && hasFacilityId.getFacilityId() != null) {
			attr.put(HasFacilityId.ATTRIBUTE_FACILITY, hasFacilityId.getFacilityId().toString());
		}
		if (this instanceof HasLinkId hasLinkId && hasLinkId.getLinkId() != null) {
			attr.put(HasLinkId.ATTRIBUTE_LINK, hasLinkId.getLinkId().toString());
		}
		if (this instanceof BasicLocation basicLocation && basicLocation.getCoord() != null) {
			if (((BasicLocation)this).getCoord() != null) {
				attr.put(ATTRIBUTE_X, String.valueOf(basicLocation.getCoord().getX()));
				attr.put(ATTRIBUTE_Y, String.valueOf(basicLocation.getCoord().getY()));
			}
		}
		if (this instanceof HasVehicleId hasVehicleId && hasVehicleId.getVehicleId() != null) {
			attr.put(HasVehicleId.ATTRIBUTE_VEHICLE, hasVehicleId.getVehicleId().toString());
		}
		return attr;
	}

	/** @return a unique, descriptive name for this event type, used to identify event types in files. */
	abstract public String getEventType();

	public final double getTime() {
		return this.time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public String toString() {
		Map<String,String> attr = this.getAttributes() ;
		StringBuilder eventXML = new StringBuilder("\t<event ");
		for (Map.Entry<String, String> entry : attr.entrySet()) {
			eventXML.append(entry.getKey());
			eventXML.append("=\"");
			eventXML.append(entry.getValue());
			eventXML.append("\" ");
		}
		eventXML.append(" />");
		return eventXML.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Event)) {
			return false;
		} else {
			Event other = (Event) obj;
			return time == other.time &&
					getEventType().equals(other.getEventType()) &&
					getAttributes().equals(other.getAttributes());
		}
	}

	@Override
	public int hashCode() {
		return getAttributes().hashCode(); // Two equal events must at least have the same attributes, so they will get the same hashCode like this.
	}


	/**
	 * Write the start of the xml representation and some common attributes. This method should be called first by {@link #writeAsXML(StringBuilder)}.
	 */
	protected final void writeXMLStart(StringBuilder out) {
		out.append("\t<event time=\"").append(time).append("\" type=\"");
		writeEncodedAttributeValue(out, getEventType()).append("\" ");

		if (this instanceof HasPersonId hasPersonId && hasPersonId.getPersonId() != null) {
			out.append("person=\"");
			writeEncodedAttributeValue(out, hasPersonId.getPersonId().toString()).append("\" ");
		}

		if (this instanceof HasFacilityId hasFacilityId && hasFacilityId.getFacilityId() != null) {
			out.append("facility=\"");
			writeEncodedAttributeValue(out, hasFacilityId.getFacilityId().toString()).append("\" ");
		}

		if (this instanceof HasLinkId hasLinkId && hasLinkId.getLinkId() != null) {
			out.append("link=\"");
			writeEncodedAttributeValue(out, hasLinkId.getLinkId().toString()).append("\" ");
		}

		if (this instanceof BasicLocation basicLocation && basicLocation.getCoord() != null) {
			if (basicLocation.getCoord() != null) {
				out.append("x=\"").append(basicLocation.getCoord().getX()).append("\" ");
				out.append("y=\"").append(basicLocation.getCoord().getY()).append("\" ");
			}
		}

		if (this instanceof HasVehicleId hasVehicleId && hasVehicleId.getVehicleId() != null) {
			out.append("vehicle=\"");
			writeEncodedAttributeValue(out, hasVehicleId.getVehicleId().toString()).append("\" ");
		}
	}

	/**
	 * Write the closing part of the xml tag.
	 */
	protected final void writeXMLEnd(StringBuilder out) {
		out.append(" />\n");
	}

	/**
	 * Write a xml representation of this event to the given writer.
	 * The implementation must write the whole xml element <event ... />. Starting with \t and adding a newline at the end.
	 *
	 * The provided default implementation writes the whole element based on {@link #getAttributes()}. This is slow and should be overridden.
	 * The overriding implementation must *not* call the super method.
	 */
	public void writeAsXML(StringBuilder out) {
		out.append("\t<event ");
		Map<String, String> attr = getAttributes();
		for (Map.Entry<String, String> entry : attr.entrySet()) {
			out.append(entry.getKey());
			out.append("=\"");
			out.append(XmlUtils.encodeAttributeValue(entry.getValue()));
			out.append("\" ");
		}
		out.append(" />\n");
	}

}


