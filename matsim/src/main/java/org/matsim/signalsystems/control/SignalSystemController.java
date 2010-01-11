/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.control;

import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.SignalEngine;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * @author dgrether
 *
 */
public interface SignalSystemController {

	public boolean givenSignalGroupIsGreen(double time, SignalGroupDefinition signalGroup);
	
	public void setDefaultCycleTime(Double seconds);
	
	public void setDefaultSynchronizationOffset(Double seconds);
	
	public void setDefaultInterGreenTime(Double seconds);
	
	public SortedMap<Id, SignalGroupDefinition> getSignalGroups();

	public SignalEngine getSignalEngine();
	
	public void setSignalEngine(SignalEngine signalEngine);
}
