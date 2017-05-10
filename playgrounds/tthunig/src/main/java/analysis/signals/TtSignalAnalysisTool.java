/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis.signals;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

/**
 * @author tthunig
 *
 */
public class TtSignalAnalysisTool implements SignalGroupStateChangedEventHandler, AfterMobsimListener, ActivityStartEventHandler, ActivityEndEventHandler{

	private Map<Id<SignalGroup>, Double> totalSignalGreenTime;
	private Map<Id<SignalSystem>, Integer> numberOfCyclesPerSystem;
	private Map<Id<SignalSystem>, Double> sumOfSystemCycleTimes;
	private Map<Double, Map<Id<SignalGroup>, Double>> summedBygoneSignalGreenTimesPerSecond;
	
	private Map<Id<SignalGroup>, Double> lastSwitchesToGreen;
	private Map<Id<SignalGroup>, Double> lastSwitchesToRed;
	private Map<Id<SignalSystem>, Double> lastCycleStartPerSystem;
	
	private Map<Id<SignalGroup>, Id<SignalSystem>> signalGroup2signalSystemId;
	private Map<Id<SignalSystem>, Id<SignalGroup>> firstSignalGroupOfSignalSystem;
	private double lastActStartTime;
	private Double firstActEndTime;
	
	@Override
	public void reset(int iteration) {
		totalSignalGreenTime = new HashMap<>();
		numberOfCyclesPerSystem = new HashMap<>();
		signalGroup2signalSystemId = new HashMap<>();
		firstSignalGroupOfSignalSystem = new HashMap<>();
		sumOfSystemCycleTimes = new HashMap<>();
		summedBygoneSignalGreenTimesPerSecond = new TreeMap<>();
		lastSwitchesToGreen = new HashMap<>();
		lastSwitchesToRed = new HashMap<>();
		lastCycleStartPerSystem = new HashMap<>();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		lastActStartTime = event.getTime();
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (firstActEndTime == null) 
			firstActEndTime = event.getTime();
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		if (!signalGroup2signalSystemId.containsKey(event.getSignalGroupId())){
			signalGroup2signalSystemId.put(event.getSignalGroupId(), event.getSignalSystemId());
		}
		
		// assumption: there is an SignalGroupStateChangedEvent for every signal group at the first second of the simulation
		
		switch(event.getNewState()){
		case RED:
			// remember red switch
			lastSwitchesToRed.put(event.getSignalGroupId(), event.getTime());
			
			Double lastSwitchToGreen = lastSwitchesToGreen.remove(event.getSignalGroupId());
			doBygoneGreenTimeAnalysis(event, lastSwitchToGreen, 1);
			break;
		case GREEN:
			// remember green switch
			lastSwitchesToGreen.put(event.getSignalGroupId(), event.getTime());
			
			doCycleAnalysis(event);
			
			Double lastSwitchToRed = lastSwitchesToRed.remove(event.getSignalGroupId());
			doBygoneGreenTimeAnalysis(event, lastSwitchToRed, 0);
			break;
		default:
			break;
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// take the last activity start time as end time of the simulation
		double simEndTime = lastActStartTime;
		// imitate a last switch for all signals to fill total green time and summed bygone green time correctly
		for (Id<SignalGroup> signalGroupId : lastSwitchesToGreen.keySet()){
			SignalGroupStateChangedEvent imitatedRedSwitch = new SignalGroupStateChangedEvent(simEndTime, signalGroup2signalSystemId.get(signalGroupId), signalGroupId, SignalGroupState.RED);
			doBygoneGreenTimeAnalysis(imitatedRedSwitch, lastSwitchesToGreen.get(signalGroupId), 1);
		}
		lastSwitchesToGreen.clear();
		for (Id<SignalGroup> signalGroupId : lastSwitchesToRed.keySet()){
			SignalGroupStateChangedEvent imitatedGreenSwitch = new SignalGroupStateChangedEvent(simEndTime, signalGroup2signalSystemId.get(signalGroupId), signalGroupId, SignalGroupState.GREEN);
			doBygoneGreenTimeAnalysis(imitatedGreenSwitch, lastSwitchesToRed.get(signalGroupId), 0);
		}
		lastSwitchesToRed.clear();
	}

	private void doBygoneGreenTimeAnalysis(SignalGroupStateChangedEvent event, Double lastSwitch, int increment) {
		if (lastSwitch == null){
			// this is the first switch of the signal group. only initialize it
			if (!summedBygoneSignalGreenTimesPerSecond.containsKey(event.getTime())){
				summedBygoneSignalGreenTimesPerSecond.put(event.getTime(), new HashMap<>());
			}
			summedBygoneSignalGreenTimesPerSecond.get(event.getTime()).put(event.getSignalGroupId(), 0.);
		} 
		else {
			// this is at least the second switch of the signal group.
			
			if (event.getNewState().equals(SignalGroupState.RED)){
				// calculate last green time and add it to the total green time
				if (!totalSignalGreenTime.containsKey(event.getSignalGroupId())){
					totalSignalGreenTime.put(event.getSignalGroupId(), 0.);
				}
				double greenTime = event.getTime() - lastSwitch;
				totalSignalGreenTime.put(event.getSignalGroupId(), totalSignalGreenTime.get(event.getSignalGroupId()) + greenTime);
			}

			// fill summedBygoneSignalGreenTimesPerSecond for every second since the last switch
			double lastBygoneSignalGreenTimeInsideMap = summedBygoneSignalGreenTimesPerSecond.get(lastSwitch).get(event.getSignalGroupId());
			double time = lastSwitch + 1;
			while (time <= event.getTime()){
				if (!summedBygoneSignalGreenTimesPerSecond.containsKey(time)) {
					summedBygoneSignalGreenTimesPerSecond.put(time, new HashMap<>());
				}
				lastBygoneSignalGreenTimeInsideMap += increment;
				summedBygoneSignalGreenTimesPerSecond.get(time).put(event.getSignalGroupId(), lastBygoneSignalGreenTimeInsideMap);
				time++;
			}
		}
	}

	private void doCycleAnalysis(SignalGroupStateChangedEvent event) {
		if (!firstSignalGroupOfSignalSystem.containsKey(event.getSignalSystemId())){
			// it is the first time that a signal group of this system switches to green.
			// remember first signal group of the system
			firstSignalGroupOfSignalSystem.put(event.getSignalSystemId(), event.getSignalGroupId());
			// initialize cycle counter
			numberOfCyclesPerSystem.put(event.getSignalSystemId(), -1);
		}
		// count number of cycles per system
		if (event.getSignalGroupId().equals(firstSignalGroupOfSignalSystem.get(event.getSignalSystemId()))){
			// increase counter if first signal group of the system gets green
			numberOfCyclesPerSystem.put(event.getSignalSystemId(), numberOfCyclesPerSystem.get(event.getSignalSystemId()) + 1);
			
			// sum up cycle times of the system
			if (lastCycleStartPerSystem.containsKey(event.getSignalSystemId())){
				// add last cycle time except for the first green switch where no last cycle exists
				if (!sumOfSystemCycleTimes.containsKey(event.getSignalSystemId())){
					sumOfSystemCycleTimes.put(event.getSignalSystemId(), 0.);
				}
				double lastCycleTime = event.getTime() - lastCycleStartPerSystem.get(event.getSignalSystemId());
				sumOfSystemCycleTimes.put(event.getSignalSystemId(), sumOfSystemCycleTimes.get(event.getSignalSystemId()) + lastCycleTime);
			}
			lastCycleStartPerSystem.put(event.getSignalSystemId(), event.getTime());
		}
	}

	public Map<Id<SignalGroup>, Double> getTotalSignalGreenTime() {
		return totalSignalGreenTime;
	}

	public Map<Id<SignalGroup>, Double> calculateAvgSignalGreenTimePerFlexibleCycle(){
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = new HashMap<>();
		for (Id<SignalGroup> signalGroupId : totalSignalGreenTime.keySet()){
			Id<SignalSystem> signalSystemId = signalGroup2signalSystemId.get(signalGroupId);
			double avgSignalGreenTime = totalSignalGreenTime.get(signalGroupId) / numberOfCyclesPerSystem.get(signalSystemId);
			avgSignalGreenTimePerCycle.put(signalGroupId, avgSignalGreenTime);
		}
		return avgSignalGreenTimePerCycle;
	}

	public Map<Id<SignalSystem>, Double> calculateAvgFlexibleCycleTimePerSignalSystem(){
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = new HashMap<>();
		for (Id<SignalSystem> signalSystemId : sumOfSystemCycleTimes.keySet()){
			double avgSystemCylceTime = sumOfSystemCycleTimes.get(signalSystemId) / numberOfCyclesPerSystem.get(signalSystemId);
			avgCycleTimePerSystem.put(signalSystemId, avgSystemCylceTime);
		}
		return avgCycleTimePerSystem;
	}
	
	public Map<Double, Map<Id<SignalGroup>, Double>> getSumOfBygoneSignalGreenTime(){
		return summedBygoneSignalGreenTimesPerSecond;
	}
	
	/**
	 * can be used for fixed cycle times with repeating signal groups per cycle too (e.g. for downstream signal)
	 * @return
	 */
	public Map<Id<SignalGroup>, Double> calculateSignalGreenTimeRatios(){
		Map<Id<SignalGroup>, Double> signalGreenTimeRatios = new HashMap<>();
		for (Id<SignalGroup> signalGroupId : totalSignalGreenTime.keySet()){
			double avgSignalGreenTime = totalSignalGreenTime.get(signalGroupId) / (this.lastActStartTime - this.firstActEndTime);
			signalGreenTimeRatios.put(signalGroupId, avgSignalGreenTime);
		}
		return signalGreenTimeRatios;
	}

}
