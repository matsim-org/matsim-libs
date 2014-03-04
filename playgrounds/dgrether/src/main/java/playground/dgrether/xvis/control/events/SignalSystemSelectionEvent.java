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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 * 
 */
public class SignalSystemSelectionEvent extends SelectionEvent implements ControlEvent {

	private Set<Id> signalSystemIds = new HashSet<Id>();

	public SignalSystemSelectionEvent(boolean overwrite) {
		super(overwrite);
	}

	public Set<Id> getSignalSystemIds() {
		return this.signalSystemIds;
	}

	public void addSignalSystemId(Id signalSystemId) {
		this.signalSystemIds.add(signalSystemId);
	}

}
