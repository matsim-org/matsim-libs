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
package signals.downstreamSensor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;

import com.google.inject.Provider;

import signals.sensor.LinkSensorManager;

/**
 * @author tthunig
 *
 */
public class DownstreamPlanbasedSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(DownstreamPlanbasedSignalController.class);

	public final static String IDENTIFIER = "DownstreamSignalControl";

	public final static class SignalControlProvider implements Provider<SignalController> {
		private final LinkSensorManager sensorManager;
		private final DownstreamSensor downstreamSensor;
		private final Scenario scenario;

		public SignalControlProvider(LinkSensorManager sensorManager, DownstreamSensor downstreamSensor, Scenario scenario) {
			this.sensorManager = sensorManager;
			this.downstreamSensor = downstreamSensor;
			this.scenario = scenario;
		}

		@Override
		public DownstreamPlanbasedSignalController get() {
			return new DownstreamPlanbasedSignalController(sensorManager, downstreamSensor, scenario);
		}
	}

	private final SignalsData signalsData;
	private final Network network;
	private final DownstreamSensor downstreamSensor;
	private final LinkSensorManager sensorManager;

	private SignalSystem system;
	private Node systemNode;
	private Map<Id<Signal>, Set<Id<Link>>> signal2DownstreamLinkMap = new HashMap<>();
	private SignalPlan signalPlan;
	private Set<Id<SignalGroup>> greenGroupsAccordingToPlan = new HashSet<>();
	private Set<Id<SignalGroup>> greenGroupsInSimulation = new HashSet<>();
	

	private DownstreamPlanbasedSignalController(LinkSensorManager sensorManager, DownstreamSensor downstreamSensor, Scenario scenario) {
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.network = scenario.getNetwork();
		this.downstreamSensor = downstreamSensor;
		this.sensorManager = sensorManager;
	}

	@Override
	public void addPlan(SignalPlan plan) {
		if (signalPlan != null || plan.getStartTime() != 0.0 || plan.getEndTime() != 0.0) {
			throw new UnsupportedOperationException("This downstream signal algorithm only works with one single signal plan for each signal system that is active all day.");
		}
		this.signalPlan = plan;
	}

	@Override
	public void updateState(double timeSeconds) {
		// schedule onsets and droppings of the plan; update greenGroupsAccordingToPlan
		{
			List<Id<SignalGroup>> droppingsNow = signalPlan.getDroppings(timeSeconds);
			if (droppingsNow != null) {
				for (Id<SignalGroup> groupId : droppingsNow) {
					greenGroupsAccordingToPlan.remove(groupId);
					// schedule dropping only if signal group still shows green, i.e. has not already switched off by the downstream logic
					if (greenGroupsInSimulation.contains(groupId)) { // i.e. signal group shows green in simulation
						// switch to red
						this.system.scheduleDropping(timeSeconds, groupId);
						greenGroupsInSimulation.remove(groupId);
						/* greenSignalGroupsInSimulation as data container needed because it also contains requested state changes which are needed for the downstream logic below, see (*). 
						 * signal group states in simulation would only switch after this updateState method has finished. theresa jan'17 */
					}
					// else: keep showing red, i.e. do nothing
				}
			}
			if (signalPlan.getOnsets(timeSeconds) != null) {
				for (Id<SignalGroup> groupId : signalPlan.getOnsets(timeSeconds)) {
					// switch to green
					this.system.scheduleOnset(timeSeconds, groupId);
					greenGroupsAccordingToPlan.add(groupId);
					greenGroupsInSimulation.add(groupId);
				}
			}
		}

		/* downstream logic: switch red if downstream link occupied. (*) */
		{
			// check downstream links for all signal groups that show green/ has to show green (according to the plan)
			for (Id<SignalGroup> groupId : greenGroupsAccordingToPlan) {
				if (allDownstreamLinksEmpty(groupId)) {
					// all downstream links 'empty'
					if (!greenGroupsInSimulation.contains(groupId)) {
						// switch to green
						this.system.scheduleOnset(timeSeconds, groupId);
						greenGroupsInSimulation.add(groupId);
					}
					// else: keep showing green, i.e. do nothing
				} else {
					// at least one downstream link 'occupied'
					if (greenGroupsInSimulation.contains(groupId)) {
						// switch to red
						this.system.scheduleDropping(timeSeconds, groupId);
						greenGroupsInSimulation.remove(groupId);
					}
					// else: keep showing red, i.e. do nothing
				}
			}
		}

		// TODO try alternatives: e.g. switch to next phase when downstream link occupied
	}

	/**
	 * 
	 * @param signalGroupId
	 * @return true if all downstream links of all signals of the signal group are empty
	 */
	private boolean allDownstreamLinksEmpty(Id<SignalGroup> signalGroupId) {
		for (Id<Signal> signalId : this.signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(this.system.getId()).get(signalGroupId).getSignalIds()) {
			for (Id<Link> downstreamLinkId : signal2DownstreamLinkMap.get(signalId)) {
				if (!downstreamSensor.linkEmpty(downstreamLinkId)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void reset(Integer iterationNumber) {
		// nothing is to do. init() is only needed once
	}

	/**
	 * Is called when mobsim is initialized. Installs sensors on all outgoing links of the systems node.
	 */
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		determineDownstreamLinks();
		for (Id<Link> outgoingLinkId : systemNode.getOutLinks().keySet()) {
			this.sensorManager.registerNumberOfCarsMonitoring(outgoingLinkId);
		}
	}

	private void determineDownstreamLinks() {
		SignalSystemData signalSystem = signalsData.getSignalSystemsData().getSignalSystemData().get(this.system.getId());
		for (SignalData signal : signalSystem.getSignalData().values()) {
			systemNode = this.network.getLinks().get(signal.getLinkId()).getToNode();

			this.signal2DownstreamLinkMap.put(signal.getId(), new HashSet<>());
			if (signal.getTurningMoveRestrictions() != null) {
				this.signal2DownstreamLinkMap.get(signal.getId()).addAll(signal.getTurningMoveRestrictions());
			} else {
				// if no turning move restrictions are set, turning is allowed to all outgoing links
				this.signal2DownstreamLinkMap.get(signal.getId()).addAll(systemNode.getOutLinks().keySet());
			}
		}
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem;
	}

}
