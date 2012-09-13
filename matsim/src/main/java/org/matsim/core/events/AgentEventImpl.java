/* *********************************************************************** *
 * project: org.matsim.*
 * AgentEvent.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;

public abstract class AgentEventImpl extends EventImpl  {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;
	private final Id linkId;
	private final String legMode;

	AgentEventImpl(final double time, final Id personId, final Id linkId, final String legMode) {
		super(time);
		this.personId = personId;
		this.linkId = linkId;
		this.legMode = legMode;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (this.legMode != null) {
			attr.put(ATTRIBUTE_LEGMODE, this.legMode);
		}
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		return attr;
	}

	public String getLegMode() {
		return this.legMode;
	}

	public Id getLinkId() {
		return this.linkId;
	}


	public Id getPersonId() {
		return this.personId;
	}
	

}
