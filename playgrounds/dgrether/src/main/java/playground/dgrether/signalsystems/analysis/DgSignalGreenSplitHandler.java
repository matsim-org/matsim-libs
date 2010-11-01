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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;



/**
 * @author dgrether
 *
 */
public class DgSignalGreenSplitHandler implements SignalGroupStateChangedEventHandler{

  private static final Logger log = Logger
      .getLogger(DgSignalGreenSplitHandler.class);
  
  private Map<Id, DgSignalSystemData> systemIdDataMap = new HashMap<Id, DgSignalSystemData>();
  
  public void addSignalSystem(Id systemId){
    this.systemIdDataMap.put(systemId, new DgSignalSystemData());
  }
  
  @Override
  public void handleEvent(SignalGroupStateChangedEvent event) {
    DgSignalSystemData data = this.systemIdDataMap.get(event.getSignalSystemId());
    if (data != null){
      data.processStateChange(event);
    }
  }

  @Override
  public void reset(int iteration) {
    Set<Id> systemSet = this.systemIdDataMap.keySet();
    for (Id id : systemSet){
      this.systemIdDataMap.put(id, new DgSignalSystemData());
    }
  }
  
  public Map<Id, DgSignalSystemData> getSystemIdDataMap() {
    return systemIdDataMap;
  }

  
} 