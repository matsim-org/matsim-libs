/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalGreenSplitHandler
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
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.SignalSystem;



/**
 * @author dgrether
 *
 */
public class DgSignalGreenSplitHandler implements SignalGroupStateChangedEventHandler{

  private Map<Id<SignalSystem>, DgSignalSystemAnalysisData> systemIdDataMap = new HashMap<>();
  
  public void addSignalSystem(Id<SignalSystem> systemId){
    this.systemIdDataMap.put(systemId, new DgSignalSystemAnalysisData());
  }
  
  @Override
  public void handleEvent(SignalGroupStateChangedEvent event) {
    DgSignalSystemAnalysisData data = this.systemIdDataMap.get(event.getSignalSystemId());
    if (data != null){
      data.processStateChange(event);
    }
  }

  @Override
  public void reset(int iteration) {
    Set<Id<SignalSystem>> systemSet = this.systemIdDataMap.keySet();
    for (Id<SignalSystem> id : systemSet){
      this.systemIdDataMap.put(id, new DgSignalSystemAnalysisData());
    }
  }
  
  public Map<Id<SignalSystem>, DgSignalSystemAnalysisData> getSystemIdAnalysisDataMap() {
    return systemIdDataMap;
  }

  
} 