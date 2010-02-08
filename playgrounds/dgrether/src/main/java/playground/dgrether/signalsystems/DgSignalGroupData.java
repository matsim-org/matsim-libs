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
  
  private Map<SignalGroupState, Double> stateTimeMap = new HashMap<SignalGroupState, Double>();
  
  public void processStateChange(SignalGroupStateChangedEvent e) {
    if (oldState == null){
      oldState = e.getNewState();
    }
    Double stateTime = stateTimeMap.get(oldState);
    if (stateTime == null) {
      stateTime = 0.0;
    }
    stateTime = stateTime + e.getTime();
    stateTimeMap.put(oldState, stateTime);
    oldState = e.getNewState();
  }

  
  public Map<SignalGroupState, Double> getStateTimeMap() {
    return stateTimeMap;
  }
}
