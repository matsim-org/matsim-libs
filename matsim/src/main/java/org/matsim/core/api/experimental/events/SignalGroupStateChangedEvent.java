/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemStateChangedEventImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class SignalGroupStateChangedEvent extends Event {
	
	public final static String EVENT_TYPE = "signalGroupStateChangedEvent";
	
	public final static String ATTRIBUTE_SIGNALSYSTEM_ID = "signalSystemId";
	public final static String ATTRIBUTE_SIGNALGROUP_ID = "signalGroupId";
	public final static String ATTRIBUTE_SIGNALGROUP_STATE = "signalGroupState";
	
	private SignalGroupState newState;
	private Id signalGroupId;
	private Id signalSystemId;

	public SignalGroupStateChangedEvent(double time, Id systemId, Id groupId, SignalGroupState newState) {
		super(time);
		this.signalSystemId = systemId;
		this.signalGroupId = groupId;
		this.newState = newState;
	}

	public SignalGroupState getNewState() {
		return this.newState;
	}
	
	public Map<String, String> getAttributes() {
		Map<String, String> m = super.getAttributes();
		m.put(ATTRIBUTE_SIGNALSYSTEM_ID, this.signalSystemId.toString());
		m.put(ATTRIBUTE_SIGNALGROUP_ID, this.signalGroupId.toString());
		m.put(ATTRIBUTE_SIGNALGROUP_STATE, this.newState.toString());
		return m;
	}

	public String getEventType() {
		return SignalGroupStateChangedEvent.EVENT_TYPE;
	}

	public Id getSignalGroupId() {
		return signalGroupId;
	}

	public Id getSignalSystemId() {
		return signalSystemId;
	}
}
