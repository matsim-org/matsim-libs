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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.controller.AbstractSignalController;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController.FixedTimeFactory;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.ControlerListenerManager;
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
public class SimpleResponsiveSignal extends AbstractSignalController implements AfterMobsimListener {

	private static final Logger LOG = LogManager.getLogger(SimpleResponsiveSignal.class);
	public static final String IDENTIFIER = "SimpleResponsiveSignalControl";

	// increase this if agents should have "time" (iterations) to react to the changed signal control
	private static final int INTERVAL = 1;

	private Map<Id<Link>, Double> link2avgDelay = new HashMap<>();

	private Scenario scenario;
	private SignalModelFactory factory;

	private SignalController delegatePlanbasedSignalController;

	public final static class SimpleResponsiveSignalFactory implements SignalControllerFactory {

		@Inject ControlerListenerManager manager;
		@Inject Scenario scenario;
		@Inject SignalModelFactory factory;

		@Override
		public SignalController createSignalSystemController(SignalSystem signalSystem) {
			SimpleResponsiveSignal controller = new SimpleResponsiveSignal();
			controller.setSignalSystem(signalSystem);
			controller.setFactory(factory);
			controller.setScenario(scenario);

			/* add the responsive signal as a controler listener to be able to listen to
			 * AfterMobsimEvents (which provide the TravelTime object): */
			manager.addControlerListener(controller);

			return controller;
		}
	}

	private SimpleResponsiveSignal() {
		super();
	}

	void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	void setFactory(SignalModelFactory factory) {
		this.factory = factory;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// change signal green split every INTERVAL iteration
		if (event.getIteration() % INTERVAL == 0 && event.getIteration() != scenario.getConfig().controller().getFirstIteration()){
			computeDelays(event);
			LOG.info("+++ Iteration " + event.getIteration() + ". Update signal green split...");
			updateSignals();
		}
	}

	private void computeDelays(AfterMobsimEvent event) {
		TravelTime travelTime = event.getServices().getLinkTravelTimes();
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();

		for (Link link : scenario.getNetwork().getLinks().values()){
			double freespeedTT = link.getLength() / link.getFreespeed();

			int timeBinCounter = 0;
			double summedDelay = 0.0;
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				double avgDelay = travelTime.getLinkTravelTime(link, (endTime - timeBinSize/2.), null, null) - freespeedTT;
				summedDelay += avgDelay;
				timeBinCounter++;
			}
			link2avgDelay.put(link.getId(), summedDelay/timeBinCounter);
			LOG.info("Link id: " + link.getId() + ", avg delay: " + summedDelay/timeBinCounter);
		}

	}

	/**
	 * create a new signal plan with shifted green times and save it in field signalPlans directly
	 */
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
			/* the new plan needs to be added to the signal control since it was made persistent over the iterations
			 * and is not built newly each iteration from the data. theresa, jan'20 */
			addPlan(this.factory.createSignalPlan(signalPlan));
		} else {
			// do nothing
			LOG.info("Signal control unchanged.");
		}
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		super.setSignalSystem(signalSystem);
		delegatePlanbasedSignalController = new FixedTimeFactory().createSignalSystemController(signalSystem);
	}

	@Override
	public void addPlan(SignalPlan plan) {
		super.addPlan(plan);
		delegatePlanbasedSignalController.addPlan(plan);
	}

	@Override
	public void updateState(double timeSeconds) {
		delegatePlanbasedSignalController.updateState(timeSeconds);
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		delegatePlanbasedSignalController.simulationInitialized(simStartTimeSeconds);
	}

}
