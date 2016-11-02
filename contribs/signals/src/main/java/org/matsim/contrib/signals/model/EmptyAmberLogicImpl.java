/* *********************************************************************** *
 * project: org.matsim.*
 * EmptyAmberLogicImpl
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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;


/**
 * Default AmberLogic that switches to green or red without any amber signal in between.
 * 
 * @author dgrether
 *
 */
public class EmptyAmberLogicImpl implements AmberLogic {

	public EmptyAmberLogicImpl() {
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processDropping(double now, Id<SignalSystem> systemId, Id<SignalGroup> signalGroupId) {
		Set<SignalGroupStateChangeRequest> ret = new HashSet<SignalGroupStateChangeRequest>();
		SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.RED, now);
		ret.add(redRequest);
		return ret;
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processOnsets(double now, Id<SignalSystem> systemId, Id<SignalGroup> signalGroupId) {
		Set<SignalGroupStateChangeRequest> ret = new HashSet<SignalGroupStateChangeRequest>();
		SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.GREEN, now);
		ret.add(redRequest);
		return ret;
	}
}
