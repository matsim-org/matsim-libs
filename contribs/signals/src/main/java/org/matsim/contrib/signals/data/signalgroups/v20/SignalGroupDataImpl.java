/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupDataImpl
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
package org.matsim.contrib.signals.data.signalgroups.v20;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
/**
 * @author jbischoff
 * @author dgrether
 */
public class SignalGroupDataImpl implements SignalGroupData {

	Set<Id<Signal>> signalIds = new TreeSet<>();
	Id<SignalGroup> signalGroupId;
	Id<SignalSystem> signalSystemId;

	SignalGroupDataImpl(Id<SignalSystem> signalSystemId, Id<SignalGroup> signalGroupId) {
		this.signalSystemId = signalSystemId;
		this.signalGroupId = signalGroupId;
	}

	@Override
	public void addSignalId(Id<Signal> signalId) {
		signalIds.add(signalId);
	}

	@Override
	public Id<SignalGroup> getId() {
		return this.signalGroupId;
	}

	@Override
	public Set<Id<Signal>> getSignalIds() {
		return this.signalIds;
	}

	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return this.signalSystemId;
	}
}
