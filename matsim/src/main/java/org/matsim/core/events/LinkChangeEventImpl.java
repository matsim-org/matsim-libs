/* *********************************************************************** *
 * project: org.matsim.*
 * LinkChangeEventImpl.java
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

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public abstract class LinkChangeEventImpl extends EventImpl implements LinkChangeEvent {

	private Id linkId;
	private ChangeValue changeValue;
	
	LinkChangeEventImpl(double time, Id linkId, ChangeValue changeValue) {
		super(time);
		this.linkId = linkId;
		this.changeValue = changeValue;
	}
	
	@Override
	public ChangeValue getChangeValue() {
		return this.changeValue;
	}

	@Override
	public Id getLinkId() {
		return this.linkId;
	}

	@Override
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

}
