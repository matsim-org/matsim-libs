/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupStateChangeRequestImpl
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
package org.matsim.signalsystems.model;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.control.SignalGroupState;

/**
 * @author dgrether
 *
 */
public class SignalGroupStateChangeRequestImpl implements SignalGroupStateChangeRequest {
	
	private SignalGroupState newState;
	private double timeSec;
	private Id signalGroupId;
	
	public SignalGroupStateChangeRequestImpl(Id groupId, SignalGroupState newState, double timeSeconds){
		this.signalGroupId = groupId;
		this.newState = newState;
		this.timeSec = timeSeconds;
	}
	
	@Override
	public SignalGroupState getRequestedState() {
		return this.newState;
	}

	@Override
	public double getTimeOfDay() {
		return this.timeSec;
	}

	@Override
	public Id getSignalGroupId() {
		return signalGroupId;
	}

	@Override
	public int compareTo(SignalGroupStateChangeRequest other) {
		int doubleCompare = Double.compare(this.getTimeOfDay(), other.getTimeOfDay());
		//sorting by time only is not unique within one timestep -> sort by id
		if (doubleCompare == 0){
			return this.getSignalGroupId().compareTo(other.getSignalGroupId());
		}
		return doubleCompare;
	}
}
