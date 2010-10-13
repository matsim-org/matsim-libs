/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptiveControllHead.java
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

package playground.jbischoff.BAsignals;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalGroup;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;



public class AdaptiveControllHead {
private Map<Id,SignalSystemControllerData> sscmap;
private Map<Id,SignalSystem> signalSystemsMap;
public AdaptiveControllHead() {
sscmap = new HashMap<Id,SignalSystemControllerData>();
this.signalSystemsMap = new HashMap<Id,SignalSystem>();
}

public void addAdaptiveSignalSystem(SignalSystem system, SignalSystemControllerData asigs){
	
	this.sscmap.put(asigs.getSignalSystemId(), asigs);
	this.signalSystemsMap.put(system.getId(), system);
	
}

public Id getSignalSystemforLaneId(Id laneId){
	for (SignalSystem system : this.signalSystemsMap.values()){
		for (SignalGroup sg : system.getSignalGroups().values()){
			for (Signal sig : sg.getSignals().values()){
				if (sig.getLaneIds().contains(laneId)){
					return system.getId();
				}
			}
		}
		
	}
	return null;
}


public Map<Id, SignalSystemControllerData> getSignalSystemController() {
	
	return sscmap;
}

public void sizeDownPlans(int seconds){
	
	for  (SignalSystemControllerData sscd : this.sscmap.values()){
		for (SignalPlanData spd : sscd.getSignalPlanData().values()){
			double ratio = (double) seconds / (double) spd.getCycleTime();
			for (SignalGroupSettingsData sgsd : spd.getSignalGroupSettingsDataByGroupId().values()){
				System.out.println(sgsd.getSignalGroupId()+" old onset "+sgsd.getOnset()+" old dropping "+sgsd.getDropping());
				Long nos = Math.round(sgsd.getOnset()*ratio);
				sgsd.setOnset(nos.intValue());
				Long ndr = Math.round(sgsd.getDropping()*ratio);
				sgsd.setDropping(ndr.intValue());
				System.out.println(sgsd.getSignalGroupId()+" new onset "+sgsd.getOnset()+" new dropping "+sgsd.getDropping());
				
				
			}
		
			
		}
	}
	
}



}

