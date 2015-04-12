/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystem
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
package org.matsim.signals.model;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;



/**
 * @author dgrether
 *
 */
public interface SignalSystem extends Identifiable<SignalSystem> {

	public SignalSystemsManager getSignalSystemsManager();
	
	public void setSignalSystemsManager(SignalSystemsManager signalManager);

	public void updateState(double time_sec);

	public void setSignalSystemController(SignalController controller);

	public void addSignal(Signal signal);
	
	public Map<Id<Signal>, Signal> getSignals();

	public void addSignalGroup(SignalGroup group);
	
	public Map<Id<SignalGroup>, SignalGroup> getSignalGroups();

	public void scheduleDropping(double timeSeconds, Id<SignalGroup> signalGroupId);

	public void scheduleOnset(double timeSeconds, Id<SignalGroup> signalGroupId);

	public SignalController getSignalController();

	public void simulationInitialized(double simStartTimeSeconds);
	
	public void switchOff(double timeSeconds);
	
}
