/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreensLogicImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreensForSignalSystemData;


/**
 * @author dgrether
 *
 */
public class IntergreensLogicImpl implements IntergreensLogic {

private static final Logger log = Logger.getLogger(IntergreensLogicImpl.class);

	private final IntergreenTimesData intergreensData;
	private final SignalSystemsConfigGroup signalsConfig;
	private final Map<Id<SignalSystem>, Map<Id<SignalGroup>, Double>> signalSystemIdSignalGroupMap = new HashMap<>();

	public IntergreensLogicImpl(IntergreenTimesData intergreenTimesData, SignalSystemsConfigGroup signalSystemsConfigGroup) {
		this.intergreensData = intergreenTimesData;
		this.signalsConfig = signalSystemsConfigGroup;
	}

	@Override
	public void reset(int iteration) {
		this.signalSystemIdSignalGroupMap.clear();
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		if (SignalGroupState.GREEN.equals(event.getNewState())){
			this.checkAndHandleGreenAllowed(event);
		}
		else if (SignalGroupState.YELLOW.equals(event.getNewState())) {
			this.handleRedOrSimilarStateChange(event);
		}
		else if (SignalGroupState.RED.equals(event.getNewState())){
			this.handleRedOrSimilarStateChange(event);
		}
//		else if (SignalGroupState.REDYELLOW.equals(event.getNewState())){
//		}
		else if (SignalGroupState.OFF.equals(event.getNewState())){
			this.handleRedOrSimilarStateChange(event);
		}
	}

	private void handleRedOrSimilarStateChange(SignalGroupStateChangedEvent event){
		Map<Id<SignalGroup>, Double> signalGroupOffTimeMap = this.signalSystemIdSignalGroupMap.get(event.getSignalSystemId());
		if (signalGroupOffTimeMap == null){
			this.signalSystemIdSignalGroupMap.put(event.getSignalSystemId(), new HashMap<Id<SignalGroup>, Double>());
		}
		this.signalSystemIdSignalGroupMap.get(event.getSignalSystemId()).put(event.getSignalGroupId(), event.getTime());
	}
	
	private void checkAndHandleGreenAllowed(SignalGroupStateChangedEvent event) {
			IntergreensForSignalSystemData greensData4System = this.intergreensData.getIntergreensForSignalSystemDataMap().get(event.getSignalSystemId());
			if (greensData4System != null){
				Map<Id<SignalGroup>, Integer> endGroupTimes4BeginningGroup = greensData4System.getEndSignalGroupTimesForBeginningGroup((event.getSignalGroupId()));
				if (endGroupTimes4BeginningGroup != null){
						for (Entry<Id<SignalGroup>, Integer> intergreens4BeginningGroup : endGroupTimes4BeginningGroup.entrySet()){
							int time = (int) event.getTime();
							Map<Id<SignalGroup>, Double> droppingTimes4System =  this.signalSystemIdSignalGroupMap.get(event.getSignalSystemId());
							if (droppingTimes4System != null){
								Double lastDropTime =  droppingTimes4System.get(intergreens4BeginningGroup.getKey());
								if (lastDropTime != null){
									double realIntergreen = time - lastDropTime;
									if (intergreens4BeginningGroup.getValue() < realIntergreen) {
										StringBuilder intergreenViolation = new StringBuilder();
										intergreenViolation.append("SignalSystem Id ");
										intergreenViolation.append(event.getSignalSystemId());
										intergreenViolation.append(" SignalGroup Id ");
										intergreenViolation.append(event.getSignalGroupId());
										intergreenViolation.append(" is switched to green at second ");
										intergreenViolation.append(event.getTime());
										intergreenViolation.append(" . This is a intergreen conflict with SignalGroup Id ");
										intergreenViolation.append(intergreens4BeginningGroup.getKey());
										intergreenViolation.append(" switched red/yellow/off at second ");
										intergreenViolation.append(lastDropTime);
										intergreenViolation.append(" i.e. the intergreen lasts ");
										intergreenViolation.append(realIntergreen);
										intergreenViolation.append(" seconds. ");
										intergreenViolation.append(" The minimal intergreen required is, however, ");
										intergreenViolation.append(intergreens4BeginningGroup.getValue());
										intergreenViolation.append(" seconds.");
										if (this.signalsConfig.getActionOnIntergreenViolation().equals(SignalSystemsConfigGroup.WARN_ON_INTERGREEN_VIOLATION)){
											log.warn(intergreenViolation.toString());
										}
										else if (this.signalsConfig.getActionOnIntergreenViolation().equals(SignalSystemsConfigGroup.EXCEPTION_ON_INTERGREEN_VIOLATION)) {
											throw new RuntimeException(intergreenViolation.toString());
										}
									}
								}
							}
						}
				}
			}
		}
	}
