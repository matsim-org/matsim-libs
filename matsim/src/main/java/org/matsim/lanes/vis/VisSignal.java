/* *********************************************************************** *
 * project: org.matsim.*
 * OTFSignalPosition
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
package org.matsim.lanes.vis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class VisSignal implements Serializable {

	private String id;
	private SignalGroupState state;
	private List<VisLinkWLanes> turningMoveRestrictions = null;
	private String systemId;
	
	public VisSignal(String systemId, String signalId) {
		this.systemId = systemId;
		this.id = signalId;
	}

	public String getId(){
		return this.id;
	}

	public String getSignalSystemId(){
		return this.systemId;
	}
	
	public void setState(SignalGroupState state){
		this.state = state;
	}
	
	public SignalGroupState getSignalGroupState(){
		return this.state;
	}
	
	public List<VisLinkWLanes> getTurningMoveRestrictions(){
		return this.turningMoveRestrictions;
	}

	public void addTurningMoveRestriction(VisLinkWLanes toLink) {
		if (this.turningMoveRestrictions == null){
			this.turningMoveRestrictions = new ArrayList<VisLinkWLanes>();
		}
		this.turningMoveRestrictions.add(toLink);
	}

}
