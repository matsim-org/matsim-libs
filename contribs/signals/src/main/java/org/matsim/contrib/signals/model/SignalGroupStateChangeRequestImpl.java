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
package org.matsim.contrib.signals.model;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

/**
 * @author dgrether
 *
 */
public class SignalGroupStateChangeRequestImpl implements SignalGroupStateChangeRequest {
	
	private SignalGroupState newState;
	private double timeSec;
	private Id<SignalGroup> signalGroupId;
	
	private String hashCode = null;
	
	public SignalGroupStateChangeRequestImpl(Id<SignalGroup> groupId, SignalGroupState newState, double timeSeconds){
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
	public Id<SignalGroup> getSignalGroupId() {
		return signalGroupId;
	}

	@Override
	public int compareTo(SignalGroupStateChangeRequest other) {
		int doubleCompare = Double.compare(this.getTimeOfDay(), other.getTimeOfDay());
		//sorting by time only is not unique within one timestep -> sort by id
		if (doubleCompare == 0){
			int idCompare = this.getSignalGroupId().compareTo(other.getSignalGroupId());
			if (idCompare == 0){
				return this.newState.compareTo(other.getRequestedState());
			}
			return idCompare;
		}
		return doubleCompare;
	}
	
	@Override
	public int hashCode(){
		if (this.hashCode == null){
			StringBuilder hCode = new StringBuilder();
			hCode.append(newState.toString());
			hCode.append(Double.toString(timeSec));
			hCode.append(signalGroupId.toString());
			this.hashCode = hCode.toString();
		}
		return this.hashCode.hashCode();
	}
	
	@Override
	public boolean equals(final Object other){
		if (!(other instanceof SignalGroupStateChangeRequestImpl)){
			return false;
		}
		else {
			return (this.compareTo((SignalGroupStateChangeRequestImpl)other) == 0);
		}
	}
}
