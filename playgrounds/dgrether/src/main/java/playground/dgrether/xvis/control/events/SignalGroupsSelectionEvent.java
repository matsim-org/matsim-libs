/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.control.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalGroupsSelectionEvent extends SelectionEvent implements ControlEvent {

	private Map<Id, Set<Id>> selectedSignalGroupIdsBySystemId = new HashMap<Id, Set<Id>>();

	public SignalGroupsSelectionEvent(boolean overwrite) {
		super(overwrite);
	}

	public Map<Id, Set<Id>> getSelectedSignalGroupIdsBySystemId() {
		return selectedSignalGroupIdsBySystemId;
	}

	
	public void addSignalGroupIds(Id signalSystemId, Set<Id> signalGroupIds) {
		if (! this.selectedSignalGroupIdsBySystemId.containsKey(signalSystemId)){
			this.selectedSignalGroupIdsBySystemId.put(signalSystemId, new HashSet<Id>());
		}
		this.selectedSignalGroupIdsBySystemId.get(signalSystemId).addAll(signalGroupIds);
	}

	
}
