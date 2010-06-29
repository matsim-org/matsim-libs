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
package playground.dgrether.signalsystems;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.signalsystems.control.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class DgSignalGroupData {
  
  private SignalGroupState oldState = null;
  
  private double oldStateOnTime = 0.0;
  
  private Map<SignalGroupState, Double> stateTimeMap = new HashMap<SignalGroupState, Double>();
  
  public DgSignalGroupData(){
  	for (SignalGroupState state : SignalGroupState.values()){
  		this.stateTimeMap.put(state, 0.0);
  	}
  }
  
  public void processStateChange(SignalGroupStateChangedEvent e) {
  	Double stateTimeSum = null;
  	double time = e.getTime();
  	SignalGroupState newState = e.getNewState();
  	if (oldState != null){
  		stateTimeSum = stateTimeMap.get(oldState);
  		stateTimeSum = stateTimeSum + (e.getTime() - oldStateOnTime);
  		stateTimeMap.put(oldState, stateTimeSum);
    }
  	oldStateOnTime = time;
    oldState = newState;
  }

  
  public Map<SignalGroupState, Double> getStateTimeMap() {
    return stateTimeMap;
  }
}
