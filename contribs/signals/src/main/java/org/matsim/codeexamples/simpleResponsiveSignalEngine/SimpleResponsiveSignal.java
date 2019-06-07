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
package org.matsim.codeexamples.simpleResponsiveSignalEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/**
 * Example for a responsive signal.
 * 
 * It updates the fixed signal control depending on the average delay every i-th iteration, 
 * whereby i is defined in the variable INTERVAL.
 * 
 * @author tthunig
 *
 */
public class SimpleResponsiveSignal implements AfterMobsimListener{

	private static final Logger LOG = Logger.getLogger(SimpleResponsiveSignal.class);
	
	// increase this if agents should have "time" (iterations) to react to the changed signal control
	private static final int INTERVAL = 1;
	
	private Map<Id<Link>, Double> link2avgDelay = new HashMap<>();
	
	@Inject Scenario scenario;
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// change signal green split every INTERVAL iteration
		if (event.getIteration() % INTERVAL == 0 && event.getIteration() != scenario.getConfig().controler().getFirstIteration()){
			computeDelays(event);
			LOG.info("+++ Iteration " + event.getIteration() + ". Update signal green split...");
			updateSignals();
		}
	}

	private void computeDelays(AfterMobsimEvent event) {
		TravelTime travelTime = event.getServices().getLinkTravelTimes();
		int timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		
		for (Link link : scenario.getNetwork().getLinks().values()){	
			double freespeedTT = link.getLength() / link.getFreespeed();
			
			int timeBinCounter = 0;
			double summedDelay = 0.0;
			for (int endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				double avgDelay = travelTime.getLinkTravelTime(link, (endTime - timeBinSize/2.), null, null) - freespeedTT;
				summedDelay += avgDelay;
				timeBinCounter++;
			}
			link2avgDelay.put(link.getId(), summedDelay/timeBinCounter);
			LOG.info("Link id: " + link.getId() + ", avg delay: " + summedDelay/timeBinCounter);
		}
		
	}

	private void updateSignals() {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData signalControl = signalsData.getSignalControlData();
		
		SignalSystemControllerData signalControlSystem1 = signalControl.getSignalSystemControllerDataBySystemId().get(Id.create("SignalSystem1", SignalSystem.class));
		SignalPlanData signalPlan = signalControlSystem1.getSignalPlanData().get(Id.create("SignalPlan1", SignalPlan.class));
		SortedMap<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettings = signalPlan.getSignalGroupSettingsDataByGroupId();
		SignalGroupSettingsData group1Setting = signalGroupSettings.get(Id.create("SignalGroup1", SignalGroup.class));
		SignalGroupSettingsData group2Setting = signalGroupSettings.get(Id.create("SignalGroup2", SignalGroup.class));
		
		// shift green time by one second depending on which delay is higher
		double delaySignalGroup1 = link2avgDelay.get(Id.createLinkId("2_3")) + link2avgDelay.get(Id.createLinkId("4_3"));
		double delaySignalGroup2 = link2avgDelay.get(Id.createLinkId("7_3")) + link2avgDelay.get(Id.createLinkId("8_3"));
		int greenTimeShift = (int) Math.signum(delaySignalGroup1 - delaySignalGroup2);

		// group1 onset = 0, group2 dropping = 55. signal switch should stay inside this interval
		if (greenTimeShift != 0 && group1Setting.getDropping() + greenTimeShift > 0 && group2Setting.getOnset() + greenTimeShift < 55){
			group1Setting.setDropping(group1Setting.getDropping() + greenTimeShift);
			group2Setting.setOnset(group2Setting.getOnset() + greenTimeShift);
			LOG.info("SignalGroup1: onset " + group1Setting.getOnset() + ", dropping " + group1Setting.getDropping());
			LOG.info("SignalGroup2: onset " + group2Setting.getOnset() + ", dropping " + group2Setting.getDropping());
		} else {
			// do nothing
			LOG.info("Signal control unchanged.");
		}
	}

}
