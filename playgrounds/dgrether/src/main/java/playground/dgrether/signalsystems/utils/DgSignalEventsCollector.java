/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalEventsCollector
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
package playground.dgrether.signalsystems.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;


/**
 * @author dgrether
 *
 */
public class DgSignalEventsCollector implements SignalGroupStateChangedEventHandler {

	private Map<Id, SortedSet<SignalGroupStateChangedEvent>> eventsBySystemId = new HashMap<Id, SortedSet<SignalGroupStateChangedEvent>>();

	@Override
	public void handleEvent(SignalGroupStateChangedEvent e) {
		if (! this.eventsBySystemId.containsKey(e.getSignalSystemId())){
			this.eventsBySystemId.put(e.getSignalSystemId(), new TreeSet<SignalGroupStateChangedEvent>(new SignalGroupStateChangedEventComparator()));
		}
		this.eventsBySystemId.get(e.getSignalSystemId()).add(e);
	}

	public Map<Id, SortedSet<SignalGroupStateChangedEvent>> getSignalGroupEventsBySystemIdMap(){
		return this.eventsBySystemId;
	}
	
	@Override
	public void reset(int iteration) {
		this.eventsBySystemId.clear();
	}
	
	private class SignalGroupStateChangedEventComparator implements Comparator<SignalGroupStateChangedEvent>{

		@Override
		public int compare(SignalGroupStateChangedEvent e1, SignalGroupStateChangedEvent e2) {
			if (e1.getTime() != e2.getTime()){
				return Double.compare(e1.getTime(), e2.getTime());
			}
			else {
				return e1.toString().compareTo(e2.toString());
			}
		}
	}
}
