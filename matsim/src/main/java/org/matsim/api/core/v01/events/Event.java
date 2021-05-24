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
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.api.internal.HasVehicleId;

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
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put(ATTRIBUTE_TIME, Double.toString(this.time));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		if ( this instanceof HasPersonId && ((HasPersonId) this).getPersonId()!=null) {
			attr.put( HasPersonId.ATTRIBUTE_PERSON, ((HasPersonId) this).getPersonId().toString() ) ;
			// many derived types do this by themselves, for historical reasons.  Since the information is put into a map, it still exists only once under that key.  kai,
			// mar'19
		}
		if ( this instanceof HasFacilityId && ((HasFacilityId) this).getFacilityId()!=null ) {
			attr.put( HasFacilityId.ATTRIBUTE_FACILITY, ((HasFacilityId) this).getFacilityId().toString() );
		}
		if ( this instanceof HasLinkId && ((HasLinkId) this).getLinkId()!=null) {
			attr.put( HasLinkId.ATTRIBUTE_LINK, ((HasLinkId) this).getLinkId().toString() );
		}
		if ( this instanceof BasicLocation && ((BasicLocation) this).getCoord()!=null ) {
			if ( ((BasicLocation) this).getCoord()!=null ) {
				attr.put( ATTRIBUTE_X, String.valueOf( ((BasicLocation) this).getCoord().getX() ) ) ;
				attr.put( ATTRIBUTE_Y, String.valueOf( ((BasicLocation) this).getCoord().getY() ) ) ;
			}
		}
		if ( this instanceof HasVehicleId && ((HasVehicleId) this).getVehicleId()!=null ) {
			attr.put( HasVehicleId.ATTRIBUTE_VEHICLE, ((HasVehicleId) this).getVehicleId().toString() );
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
}


