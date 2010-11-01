/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemData
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;


/**
 * @author dgrether
 *
 */
public class DgSignalSystemData {

  private Map<Id, DgSignalGroupData> systemGroupDataMap = new HashMap<Id, DgSignalGroupData>();
  
  public void processStateChange(SignalGroupStateChangedEvent e) {
    DgSignalGroupData groupData = this.systemGroupDataMap.get(e.getSignalGroupId());
    if (groupData == null) {
      groupData = new DgSignalGroupData();
      this.systemGroupDataMap.put(e.getSignalGroupId(), groupData);
    }
    groupData.processStateChange(e);
  }

  
  public Map<Id, DgSignalGroupData> getSystemGroupDataMap() {
    return systemGroupDataMap;
  }
}
