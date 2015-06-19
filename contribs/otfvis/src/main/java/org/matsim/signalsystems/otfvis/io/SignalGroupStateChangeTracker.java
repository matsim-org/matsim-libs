/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupStateChangeTracker
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
package org.matsim.signalsystems.otfvis.io;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;


/**
 * @author dgrether
 *
 */
public class SignalGroupStateChangeTracker implements SignalGroupStateChangedEventHandler{

	private List<SignalGroupStateChangedEvent> signalGroupEvents = new ArrayList<SignalGroupStateChangedEvent>();
	
	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		this.signalGroupEvents.add(event);
	}

	@Override
	public void reset(int iteration) {
		this.signalGroupEvents.clear();
	}

	public List<SignalGroupStateChangedEvent> getSignalGroupEvents() {
		return signalGroupEvents;
	}

}
