/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupData
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
package playground.dgrether.signalsystems.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class DgSignalGroupAnalysisData {
  
  private SignalGroupState oldState = null;
  
  private double oldStateOnTime = 0.0;

  private final double [] stateTimeArray = new double[SignalGroupState.values().length];
  
  
  public DgSignalGroupAnalysisData(){
  }
  
  public void processStateChange(final SignalGroupStateChangedEvent e) {
  	final double time = e.getTime();
  	final SignalGroupState newState = e.getNewState();
  	if (this.oldState != null){
  		this.stateTimeArray[this.oldState.ordinal()] += (time - this.oldStateOnTime);
    }
  	this.oldStateOnTime = time;
    this.oldState = newState;
  }

  

	public Map<SignalGroupState, Double> getStateTimeMap() {
		final Map<SignalGroupState, Double> map = new HashMap<SignalGroupState, Double>();
		for (final SignalGroupState state : SignalGroupState.values()){
			map.put(state, this.stateTimeArray[state.ordinal()]);
		}
    return map;
  }
}
