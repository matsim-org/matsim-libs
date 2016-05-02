/* *********************************************************************** *
 * project: org.matsim.*
 * AmberLogicImpl
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
package org.matsim.contrib.signals.model;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimeData;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class AmberLogicImpl implements AmberLogic {

	private AmberTimesData data;

	public AmberLogicImpl(AmberTimesData amberTimes){
		this.data = amberTimes;
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processDropping(double timeSec, Id<SignalSystem> systemId,
			Id<SignalGroup> signalGroupId) {
		Set<SignalGroupStateChangeRequest> ret = new HashSet<SignalGroupStateChangeRequest>();
		AmberTimeData systemData = this.data.getAmberTimeDataBySystemId().get(systemId);
		Integer amber = null;
		//first check the values for the specific system
		if (systemData != null){
			amber = systemData.getAmberOfSignal(Id.create(signalGroupId, Signal.class));
		}
		//if nothing is found use the defaults if set
		if (amber == null && this.data.getDefaultAmber() != null){
			amber = this.data.getDefaultAmber();
		}
		
		if (amber != null){
			SignalGroupStateChangeRequest amberRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.YELLOW,
					timeSec);
			ret.add(amberRequest);
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.RED, 
					timeSec + amber);
			ret.add(redRequest);
		}
		//if no amber times are set use no amber
		else {
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.RED, 
					timeSec);
			ret.add(redRequest);
		}
		return ret;
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processOnsets(double timeSec, Id<SignalSystem> systemId,
			Id<SignalGroup> signalGroupId) {
		Set<SignalGroupStateChangeRequest> ret = new HashSet<SignalGroupStateChangeRequest>();
		AmberTimeData systemData = this.data.getAmberTimeDataBySystemId().get(systemId);
		Integer redAmber = null;
		//first check the values for the specific system
		if (systemData != null){
			redAmber = systemData.getRedAmberOfSignal(Id.create(signalGroupId, Signal.class));
		} 
		//if nothing is found use the defaults if set
		if (redAmber == null && this.data.getDefaultRedAmber() != null){
			redAmber = this.data.getDefaultRedAmber();
		}

		if (redAmber != null){
			SignalGroupStateChangeRequest amberRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.REDYELLOW, 
					timeSec);
			ret.add(amberRequest);
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.GREEN, 
					timeSec + redAmber);
			ret.add(redRequest);
		}
		//if no amber is set 
		else{
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.GREEN, 
					timeSec);
			ret.add(redRequest);
		}
		return ret;
	}
	
	
}
