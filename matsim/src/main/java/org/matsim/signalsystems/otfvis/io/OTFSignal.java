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
package org.matsim.signalsystems.otfvis.io;

import java.util.ArrayList;
import java.util.List;

import org.matsim.lanes.otfvis.io.OTFLinkWLanes;
import org.matsim.signalsystems.control.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class OTFSignal {

	private String id;
	private SignalGroupState state;
	private List<OTFLinkWLanes> turningMoveRestrictions = null;
	
	public OTFSignal(String signalId) {
		this.id = signalId;
	}

	public void setState(SignalGroupState state){
		this.state = state;
	}
	
	public SignalGroupState getSignalGroupState(){
		return this.state;
	}
	
	public List<OTFLinkWLanes> getTurningMoveRestrictions(){
		return this.turningMoveRestrictions;
	}

	public void addTurningMoveRestriction(OTFLinkWLanes toLink) {
		if (this.turningMoveRestrictions == null){
			this.turningMoveRestrictions = new ArrayList<OTFLinkWLanes>();
		}
		this.turningMoveRestrictions.add(toLink);
	}

}
