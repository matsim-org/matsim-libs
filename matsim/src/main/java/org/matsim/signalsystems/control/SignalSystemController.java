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
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * All signal system controller implementations must implement this interface. 
 * If the specific controller is a listener of SimulationEvents it is automatically
 * attached by QSim as listener.
 * @see AbstractSignalSystemController for a default implementation of some methods
 * specified by this interface
 * @author dgrether
 *
 */
public interface SignalSystemController {
  /**
   * Get the signalGroupState for the given SignalGroupDefinition and the given time.
   * @param seconds May be ignored if the state is calculated before/after every step of the simulation
   * @return SignalGroupState
   */
  public SignalGroupState getSignalGroupState(double seconds, SignalGroupDefinition signalGroup);
	/**
	 * @param seconds the default for the cycle time
	 */
	public void setDefaultCycleTime(Double seconds);
	/**
	 * @param seconds the default for the synchronization offset
	 */
	public void setDefaultSynchronizationOffset(Double seconds);
	/**
	 * @param seconds the default for the inter-green time
	 */
	public void setDefaultInterGreenTime(Double seconds);
	/**
	 * @return a SortedMap of all SignalGroupDefinition instances controlled by this
	 * controller sorted and accessible by their Id
	 */
	public SortedMap<Id, SignalGroupDefinition> getSignalGroups();
	/**
	 * @return the SignalEngine instance controlling this controller
	 */
	public SignalEngine getSignalEngine();
	/**
	 * Setter for the SignalEngine this controller is attached to
	 * @param signalEngine 
	 */
	public void setSignalEngine(SignalEngine signalEngine);
}
