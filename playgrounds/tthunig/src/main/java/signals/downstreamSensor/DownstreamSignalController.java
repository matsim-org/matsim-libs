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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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

import playground.dgrether.signalsystems.LinkSensorManager;
import playground.dgrether.signalsystems.utils.AbstractSignalController;

/**
 * @author tthunig
 *
 */
public class DownstreamSignalController extends AbstractSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(DownstreamSignalController.class);

	public final static String CONTROLLER_IDENTIFIER = "DownstreamSignalControl";

	private LinkSensorManager sensorManager;
	private SignalsData signalsData;
	private Network network;

	private Node systemNode;
	private Map<Id<Signal>, Set<Id<Link>>> signal2DownstreamLinkMap = new HashMap<>();
	private SignalPlan activePlan;
	private Set<Id<SignalGroup>> greenSignalGroupsAccordingToActivePlan = new HashSet<>();
	private Set<Id<SignalGroup>> greenSignalGroupsInSimulation = new HashSet<>();
	private Map<Id<Link>, Integer> linkMaxNoCars = new HashMap<>();

	public DownstreamSignalController(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
		this.sensorManager = sensorManager;
		this.signalsData = signalsData;
		this.network = network;
		init();
	}

	private void init() {
		initLinkMaxCars();
	}

	private void initLinkMaxCars() {
		linkMaxNoCars = new HashMap<>();
		for (Link link : network.getLinks().values()){
			linkMaxNoCars.put(link.getId(), (int) (link.getLength() / 15));
		}
	}

	@Override
	public void updateState(double timeSeconds) {
		checkForActivePlan(timeSeconds);
		// if there is an active plan, i.e. signals are switched on
		if (activePlan != null) {
			// schedule onsets and droppings of the plan; update greenSignalGroupsAccordingToActivePlan
			for (Id<SignalGroup> groupId : activePlan.getDroppings(timeSeconds)) {
				this.system.scheduleDropping(timeSeconds, groupId);
				greenSignalGroupsAccordingToActivePlan.remove(groupId);
				greenSignalGroupsInSimulation.remove(groupId);
			}
			for (Id<SignalGroup> groupId : activePlan.getOnsets(timeSeconds)) {
				this.system.scheduleOnset(timeSeconds, groupId);
				greenSignalGroupsAccordingToActivePlan.add(groupId);
				greenSignalGroupsInSimulation.add(groupId);
			}

			// for all signal groups that show green/ has to show green (according to the plan)
			for (Id<SignalGroup> groupId : greenSignalGroupsAccordingToActivePlan) {
				if (allDownstreamLinksEmpty(groupId)) {
					// all downstream links 'empty'
					if (!greenSignalGroupsInSimulation.contains(groupId)){
						// switch to green
						this.system.scheduleOnset(timeSeconds, groupId);
						greenSignalGroupsInSimulation.add(groupId);
					}
					// else: keep showing green, i.e. do nothing
				} else {
					// at least one downstream link 'occupied'
					if (greenSignalGroupsInSimulation.contains(groupId)){
						// switch to red
						this.system.scheduleDropping(timeSeconds, groupId);
						greenSignalGroupsInSimulation.remove(groupId);
					}
					// else: keep showing red, i.e. do nothing
				}
			}
		}

		// TODO baue Alternative ein: springe in plan zum naechsten switch
	}

	/**
	 * 
	 * @param signalGroupId
	 * @return true if all downstream links of all signals of the signal group are empty
	 */
	private boolean allDownstreamLinksEmpty(Id<SignalGroup> signalGroupId) {
		for (Id<Signal> signalId : this.signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(this.system.getId()).get(signalGroupId).getSignalIds()){
			for (Id<Link> downstreamLinkId : signal2DownstreamLinkMap.get(signalId)){
				// TODO test other parameters
				if (this.sensorManager.getNumberOfCarsOnLink(downstreamLinkId) > linkMaxNoCars.get(downstreamLinkId)){
					return false;
				}
			}
		}
		return true;
	}
	
	private void checkForActivePlan(double now){
		// TODO do we also need/ like to have something like signal plan validation?!
		
		// TODO does not work for all possible cases of multiple signal plans
		if (activePlan == null || activePlan.getEndTime() < now ){
			// look for the next plan
			// TODO make more efficient: sort before, see DefaultPlanbasedSignalSystemController
			for (SignalPlan signalPlan : this.signalPlans.values()){
				// TODO end time is 0 when it is 12pm, see DefaultPlanbasedSignalSystemController
				if (signalPlan.getEndTime() > now && signalPlan.getStartTime() <= now){
					activePlan = signalPlan;
					break;
				}
			}
		}
	}

	@Override
	public void reset(Integer iterationNumber) {
		// TODO nothing is to do?! init is only needed once
	}

	/**
	 * Is called when mobsim is initialized. 
	 * Installs sensors on all outgoing links of the systems node.
	 */
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		determineDownstreamLinks();
		for (Id<Link> outgoingLinkId : systemNode.getOutLinks().keySet()) {
			this.sensorManager.registerNumberOfCarsMonitoring(outgoingLinkId);
		}
	}

	private void determineDownstreamLinks() {
		SignalSystemData ssd = signalsData.getSignalSystemsData().getSignalSystemData().get(this.system.getId());
		for (SignalData sd : ssd.getSignalData().values()) {
			systemNode = this.network.getLinks().get(sd.getLinkId()).getToNode();

			this.signal2DownstreamLinkMap.put(sd.getId(), new HashSet<>());
			if (sd.getTurningMoveRestrictions() != null) {
				for (Id<Link> outgoingLinkId : sd.getTurningMoveRestrictions()) {
					this.signal2DownstreamLinkMap.get(sd.getId()).add(outgoingLinkId);
				}
			} else {
				// if no turning move restrictions are set, turning is allowed to all outgoing links
				for (Id<Link> outgoingLinkId : systemNode.getOutLinks().keySet()) {
					this.signal2DownstreamLinkMap.get(sd.getId()).add(outgoingLinkId);
				}
			}
		}
	}

}
