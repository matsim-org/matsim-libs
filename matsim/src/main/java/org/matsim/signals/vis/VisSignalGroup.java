/* *********************************************************************** *
 * project: org.matsim.*
 * OTFSignalGroup
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
package org.matsim.signals.vis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.signals.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class VisSignalGroup {

	private String id;
	private Map<String, VisSignal> signalPositions = new HashMap<String, VisSignal>();
	private String systemId;
	
	public VisSignalGroup(String signalSystemId, String id){
		this.systemId = signalSystemId;
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getSignalSystemId(){
		return this.systemId;
	}

	public void setState(SignalGroupState state) {
		for (VisSignal p : this.signalPositions.values()){
			p.setState(state);
		}
	}

	public void addSignal(VisSignal signal) {
		this.signalPositions.put(signal.getId(), signal);
	}

	public Map<String, VisSignal> getSignals() {
		return this.signalPositions;
	}
	
}
