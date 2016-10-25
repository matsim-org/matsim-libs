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
 * AmberLogic for a simulation with amber times (i.e. a simulation where isUseAmbertimes in the signals module is enabled).
 * It uses amber times defined in the amber times file (signal specific or global defaults, respectively).
 * 
 * @author dgrether
 *
 */
public class AmberLogicImpl implements AmberLogic {

	private AmberTimesData data;

	public AmberLogicImpl(AmberTimesData amberTimes){
		this.data = amberTimes;
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processDropping(double now, Id<SignalSystem> systemId, Id<SignalGroup> signalGroupId) {
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
			// process amber signal before red
			SignalGroupStateChangeRequest amberRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.YELLOW, now);
			ret.add(amberRequest);
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.RED, now + amber);
			ret.add(redRequest);
		}
		else { // no amber times set. directly process red signal
			SignalGroupStateChangeRequest redRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.RED, now);
			ret.add(redRequest);
		}
		return ret;
	}

	@Override
	public Set<SignalGroupStateChangeRequest> processOnsets(double now, Id<SignalSystem> systemId, Id<SignalGroup> signalGroupId) {
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
			// process amber signal before green
			SignalGroupStateChangeRequest amberRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.REDYELLOW, now);
			ret.add(amberRequest);
			SignalGroupStateChangeRequest greenRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.GREEN, now + redAmber);
			ret.add(greenRequest);
		}
		else{ // no amber times set. directly process green signal
			SignalGroupStateChangeRequest greenRequest = new SignalGroupStateChangeRequestImpl(signalGroupId, SignalGroupState.GREEN, now);
			ret.add(greenRequest);
		}
		return ret;
	}	
}
