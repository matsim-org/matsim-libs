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
package org.matsim.contrib.signals.analysis;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author tthunig
 *
 */
@Singleton
public class SignalAnalysisTool implements SignalGroupStateChangedEventHandler, AfterMobsimListener, ActivityStartEventHandler, ActivityEndEventHandler{

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
	
	public SignalAnalysisTool() {
	}
	
	@Inject
	public SignalAnalysisTool(EventsManager em, ControlerListenerManager clm) {
		em.addHandler(this);
		clm.addControlerListener(this);
	}
	
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
			doBygoneGreenTimeAnalysis(event, lastSwitchToGreen);
			break;
		case GREEN:
			// remember green switch
			lastSwitchesToGreen.put(event.getSignalGroupId(), event.getTime());
			
			doCycleAnalysis(event);
			
			Double lastSwitchToRed = lastSwitchesToRed.remove(event.getSignalGroupId());
			doBygoneGreenTimeAnalysis(event, lastSwitchToRed);
			break;
		default:
			break;
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// take the last activity start time as end time of the simulation
		double simEndTime = lastActStartTime;
		// fill total green time and summed bygone green time correctly for all last signal phases
		for (Id<SignalGroup> signalGroupId : lastSwitchesToGreen.keySet()){
			calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(signalGroupId, simEndTime, lastSwitchesToGreen.get(signalGroupId));
			fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(signalGroupId, simEndTime, lastSwitchesToGreen.get(signalGroupId), 1);
		}
		lastSwitchesToGreen.clear();
		for (Id<SignalGroup> signalGroupId : lastSwitchesToRed.keySet()){
			fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(signalGroupId, simEndTime, lastSwitchesToRed.get(signalGroupId), 0);
		}
		lastSwitchesToRed.clear();
		// add last cycle time for all systems
		for (Id<SignalSystem> signalSystemId : lastCycleStartPerSystem.keySet()){
			addLastSystemCycleTime(signalSystemId, simEndTime);
		}
	}

	private void doBygoneGreenTimeAnalysis(SignalGroupStateChangedEvent event, Double lastSwitch) {
		if (lastSwitch == null){
			// this is the first switch of the signal group. only initialize it
			if (!summedBygoneSignalGreenTimesPerSecond.containsKey(event.getTime())){
				summedBygoneSignalGreenTimesPerSecond.put(event.getTime(), new HashMap<>());
			}
			summedBygoneSignalGreenTimesPerSecond.get(event.getTime()).put(event.getSignalGroupId(), 0.);
		} 
		else {
			// this is at least the second switch of the signal group.
			
			int increment = 0; // 0 for green switch (bygone red time)
			if (event.getNewState().equals(SignalGroupState.RED)){
				increment = 1; // 1 for red switch (bygone green time)
				calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(event.getSignalGroupId(), event.getTime(), lastSwitch);
			}
			fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(event.getSignalGroupId(), event.getTime(), lastSwitch, increment);
		}
	}

	private void fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(Id<SignalGroup> signalGroupId, double thisSwitch, double lastSwitch, int increment) {
		double lastBygoneSignalGreenTimeInsideMap = summedBygoneSignalGreenTimesPerSecond.get(lastSwitch).get(signalGroupId);
		double time = lastSwitch + 1;
		while (time <= thisSwitch){
			if (!summedBygoneSignalGreenTimesPerSecond.containsKey(time)) {
				summedBygoneSignalGreenTimesPerSecond.put(time, new HashMap<>());
			}
			lastBygoneSignalGreenTimeInsideMap += increment;
			summedBygoneSignalGreenTimesPerSecond.get(time).put(signalGroupId, lastBygoneSignalGreenTimeInsideMap);
			time++;
		}
	}

	private void calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(Id<SignalGroup> signalGroupId, double redSwitch, double lastGreenSwitch) {
		if (!totalSignalGreenTime.containsKey(signalGroupId)){
			totalSignalGreenTime.put(signalGroupId, 0.);
		}
		double greenTime = redSwitch - lastGreenSwitch;
		totalSignalGreenTime.put(signalGroupId, totalSignalGreenTime.get(signalGroupId) + greenTime);
	}

	private void doCycleAnalysis(SignalGroupStateChangedEvent event) {
		// TODO so far, this only works for fixed signals. instead: collect green switches and 'start' a new cycle, when any group switches again to green (empty the list at this moment and start collecting again)
		if (!firstSignalGroupOfSignalSystem.containsKey(event.getSignalSystemId())){
			// it is the first time that a signal group of this system switches to green.
			// remember first signal group of the system
			firstSignalGroupOfSignalSystem.put(event.getSignalSystemId(), event.getSignalGroupId());
			// initialize cycle counter
			numberOfCyclesPerSystem.put(event.getSignalSystemId(), 0);
		}
		// count number of cycles per system
		if (event.getSignalGroupId().equals(firstSignalGroupOfSignalSystem.get(event.getSignalSystemId()))){
			// increase counter if first signal group of the system gets green
			numberOfCyclesPerSystem.put(event.getSignalSystemId(), numberOfCyclesPerSystem.get(event.getSignalSystemId()) + 1);
			
			// add last cycle time except for the first green switch where no last cycle exists
			if (lastCycleStartPerSystem.containsKey(event.getSignalSystemId())) {
				addLastSystemCycleTime(event.getSignalSystemId(), event.getTime());
			}
			lastCycleStartPerSystem.put(event.getSignalSystemId(), event.getTime());
		}
	}

	/* has to be called again at the end of the simulation such that the cycle time of the last cycle can also be added */
	private void addLastSystemCycleTime(Id<SignalSystem> signalSystemId, double cycleStartTime) {
		if (!sumOfSystemCycleTimes.containsKey(signalSystemId)) {
			sumOfSystemCycleTimes.put(signalSystemId, 0.);
		}
		double lastCycleTime = cycleStartTime - lastCycleStartPerSystem.get(signalSystemId);
		sumOfSystemCycleTimes.put(signalSystemId, sumOfSystemCycleTimes.get(signalSystemId) + lastCycleTime);
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
