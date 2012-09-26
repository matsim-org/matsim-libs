/* *********************************************************************** *
 * project: org.matsim.*
 * LinkChangeLanesEventImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class LinkChangeLanesEvent extends Event {

	public static final String EVENT_TYPE = "linkChangeLanes";
	public static final String CHANGETYPE = "changetype";
	public static final String CHANGETYPEABSOLUTE = "absolute";
	public static final String CHANGETYPEFACTOR = "factor";
	public static final String CHANGEVALUE = "changevalue";
	public static final String ATTRIBUTE_LINK = "link";
	
	private Id linkId;
	private ChangeValue changeValue;

	public LinkChangeLanesEvent(double time, Id linkId, ChangeValue changeValue) {
		super(time);
		this.linkId = linkId;
		this.changeValue = changeValue;
	}
	
	public ChangeValue getChangeValue() {
		return this.changeValue;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (changeValue.getType() == NetworkChangeEvent.ChangeType.ABSOLUTE) {
			attr.put(CHANGETYPE, CHANGETYPEABSOLUTE);
		} else if (changeValue.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
			attr.put(CHANGETYPE, CHANGETYPEFACTOR);
		}
		attr.put(CHANGEVALUE, String.valueOf(changeValue.getValue()));
		return attr;
	}
	
	public String getEventType() {
		return EVENT_TYPE;
	}

}
