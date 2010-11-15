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

import java.util.ArrayList;
import java.util.List;

import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class OTFSignalGroup {

	private String id;
	private List<OTFSignal> signalPositions = new ArrayList<OTFSignal>();
	
	OTFSignalGroup(String id){
		this.id = id;
	}
	
	String getId() {
		return this.id;
	}

	public void setState(SignalGroupState state) {
		for (OTFSignal p : this.signalPositions){
			p.setState(state);
		}
	}

	public void addSignal(OTFSignal pos) {
		this.signalPositions.add(pos);
	}

	
	
}
