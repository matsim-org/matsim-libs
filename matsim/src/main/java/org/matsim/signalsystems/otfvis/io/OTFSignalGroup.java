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
package org.matsim.signalsystems.otfvis.io;

import java.util.HashMap;
import java.util.Map;

import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class OTFSignalGroup {

	private String id;
	private Map<String, OTFSignal> signalPositions = new HashMap<String, OTFSignal>();
	private String systemId;
	
	public OTFSignalGroup(String signalSystemId, String id){
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
		for (OTFSignal p : this.signalPositions.values()){
			p.setState(state);
		}
	}

	public void addSignal(OTFSignal pos) {
		this.signalPositions.put(pos.getId(), pos);
	}

	public Map<String, OTFSignal> getSignals() {
		return this.signalPositions;
	}
	
}
