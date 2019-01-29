/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Wurst.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.signals.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;

public class SignalGroupStateChangedEventMapper implements EventsReaderXMLv1.CustomEventMapper {
	@Override
	public Event apply(GenericEvent event) {
		Id<SignalSystem> systemId = Id.create(event.getAttributes().get(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALSYSTEM_ID), SignalSystem.class);
		Id<SignalGroup> groupId = Id.create(event.getAttributes().get(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_ID), SignalGroup.class);
		String state = event.getAttributes().get(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_STATE);
		SignalGroupState newState = SignalGroupState.valueOf(state);
		return new SignalGroupStateChangedEvent(event.getTime(), systemId, groupId, newState);
	}
}
