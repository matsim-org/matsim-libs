/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemStateChangedEvent
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
package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.signalsystems.control.SignalGroupState;


/**
 * Event to indicate that a signal system's signal group has changed its color.
 * @author dgrether
 *
 */
public interface SignalGroupStateChangedEvent extends Event{

	public final static String EVENT_TYPE = "signalGroupStateChangedEvent";
	
	public final static String ATTRIBUTE_SIGNALSYSTEM_ID = "signalSystemId";
	public final static String ATTRIBUTE_SIGNALGROUP_ID = "signalGroupId";
	public final static String ATTRIBUTE_SIGNALGROUP_STATE = "signalGroupState";
	
	public SignalGroupState getNewState();
	
	public Id getSignalGroupId();
	
	public Id getSignalSystemId();
	
}
