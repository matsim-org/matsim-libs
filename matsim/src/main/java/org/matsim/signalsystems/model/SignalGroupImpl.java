/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupImpl
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalGroupImpl implements SignalGroup {
	
	private Id id;
	private Map<Id, Signal> signals = new HashMap<Id, Signal>();
	private SignalGroupState state;

	public SignalGroupImpl(Id id) {
		this.id = id;
	}
	
	public Id getId(){
		return this.id;
	}

	@Override
	public void setState(SignalGroupState state) {
		this.state = state;
		for (Signal s : this.signals.values()){
			s.setState(state);
		}
	}

	@Override
	public SignalGroupState getState() {
		return this.state;
	}

	@Override
	public void addSignal(Signal signal) {
		this.signals.put(signal.getId(), signal);
	}

	@Override
	public Map<Id, Signal> getSignals() {
		return this.signals;
	}

}
