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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkChangeLanesEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class LinkChangeLanesEventImpl extends LinkChangeEventImpl implements LinkChangeLanesEvent {
	
	LinkChangeLanesEventImpl(double time, Id linkId, ChangeValue changeValue) {
		super(time, linkId, changeValue);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
