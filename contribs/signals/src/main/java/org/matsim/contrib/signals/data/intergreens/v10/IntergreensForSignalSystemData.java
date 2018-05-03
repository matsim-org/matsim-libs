/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreensForSignalSystemDataImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.intergreens.v10;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public interface IntergreensForSignalSystemData {

	public Id<SignalSystem> getSignalSystemId();
	
	public Integer getIntergreenTime(Id<SignalGroup> endingSignalGroupId, Id<SignalGroup> beginningSignalGroupId);

	public void setIntergreenTime(Integer timeSeconds, Id<SignalGroup> endingSignalGroupId, Id<SignalGroup> beginningSignalGroupId);

	public List<Tuple<Id<SignalGroup>, Id<SignalGroup>>> getEndingBeginningSignalGroupKeys();

	public Map<Id<SignalGroup>, Integer> getEndSignalGroupTimesForBeginningGroup(Id<SignalGroup> id);
	
}
