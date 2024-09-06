/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystems20ConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.consistency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;


/**
 * @author dgrether, tthunig
 */
public final class SignalSystemsDataConsistencyChecker {

	
	private static final Logger log = LogManager.getLogger(SignalSystemsDataConsistencyChecker.class);
	
	private SignalsData signalsData;

	private Lanes lanes;

	private Network network;

	private List<Tuple<Id<Signal>, Id<SignalSystem>>> malformedSignals = new LinkedList<>();

	public SignalSystemsDataConsistencyChecker(Network network, Lanes lanes, SignalsData signalsData) {
		this.network = network;
		this.signalsData = signalsData;
		this.lanes = lanes;
	}
	
	public void checkConsistency() {
		log.info("Checking consistency of signal data...");
		if (this.signalsData == null) {
			log.error("No SignalsData instance found as ScenarioElement of Scenario instance!");
			log.error("Nothing to check, aborting!");
			return;
		}
		
		this.checkSignalToLinkMatching();
		this.checkSignalToLaneMatching();
		this.checkWhetherTwoSignalsAreIdentical();
		this.removeMalformedSignalSystems();
	}

	private void checkWhetherTwoSignalsAreIdentical() {
		SignalSystemsData signalSystems = this.signalsData.getSignalSystemsData();
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			Map<Id<Link>, Set<SignalData>> linkToSignalMatching = new HashMap<>();
			for (SignalData signal : system.getSignalData().values()) {
				if (!linkToSignalMatching.containsKey(signal.getLinkId())) {
					linkToSignalMatching.put(signal.getLinkId(), new HashSet<>());
				}
				linkToSignalMatching.get(signal.getLinkId()).add(signal);
			}
			// TODO note, that if a signal gets removed from the signalsData, it also has to be removed from the groups and from the control. 
			// Also note, that removing a signal can mess up the control of the whole system
			for (Id<Link> linkId : linkToSignalMatching.keySet()) {
				// if only one signal exists for the link, nothing is to do
				if (linkToSignalMatching.get(linkId).size() > 1) {
					// if this link does not have lanes, not more than one signal should exist
					if (!lanes.getLanesToLinkAssignments().containsKey(linkId)) {
						log.warn("Link " + linkId + " does not have lanes but more then one signal. This does not make sense since all vehicles will queue in one queue anyway.");
						// TODO remove all except one signal of the link??
					} else {
						// the link has lanes and multiple signals
						for (SignalData signal : linkToSignalMatching.get(linkId)) {
							if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
								log.warn("Link " + linkId + " has lanes and multiple signals but signal " + signal.getId() + " controls the whole link.");
								// TODO remove the signal. but do not remove the last one of the link
							} else {
								// check whether another signal controls exactly the same lanes
									for (SignalData otherSignal : linkToSignalMatching.get(linkId)) {
										if (!otherSignal.equals(signal) && otherSignal.getLaneIds() != null) {
											if (signal.getLaneIds().equals(otherSignal.getLaneIds())) {
												log.warn("Signal " + signal.getId() + " on link " + linkId + " controls exactly the same lanes as signal " + otherSignal.getId() + ", i.e. they are identical.");
												// TODO remove remove otherSignal
											}
											/* note, that we do not want to remove signals that control the same lane, if they also control other lanes separately, 
											 * because this is allowed e.g. if there is a signal for right turns and a signal for all directions. */
										}
									}
							}
						}
					
					}
				}
			}
		}
	}

	private void removeMalformedSignalSystems() {

		for (Tuple<Id<Signal>, Id<SignalSystem>> tuple : malformedSignals){
			signalsData.getSignalSystemsData().getSignalSystemData().get(tuple.getSecond()).getSignalData().remove(tuple.getFirst());
			for(SignalGroupData sigGroup : this.signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(tuple.getSecond()).values()){
				if(sigGroup.getSignalIds().contains(tuple.getFirst())){
					sigGroup.getSignalIds().remove(tuple.getFirst());
				}
			}
		}
		
		removeEmptySystems();
		removeEmptyGroups();
		removeEmptyControlSettings();
	}

	private void removeEmptySystems() {
		List<Id<SignalSystem>> emptySystems = new LinkedList<>();
		for(SignalSystemData system : signalsData.getSignalSystemsData().getSignalSystemData().values()){
			if (system.getSignalData() == null || system.getSignalData().isEmpty()) {
				emptySystems.add(system.getId());
				if (!signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(system.getId()).isEmpty()){
					log.warn("a system contains no signals but groups");
				}
			}
		}
		for(Id<SignalSystem> systemId : emptySystems){
			signalsData.getSignalSystemsData().getSignalSystemData().remove(systemId);
			signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().remove(systemId);
			signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().remove(systemId);
		}
	}

	private void removeEmptyGroups() {
		List<Tuple<Id<SignalGroup>, Id<SignalSystem>>> emptyGroups = new LinkedList<>();
		for (Map<Id<SignalGroup>, SignalGroupData> sigGroups : this.signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().values()) {
			for(SignalGroupData sigGroup : sigGroups.values()){
				if(sigGroup.getSignalIds().isEmpty()){
					emptyGroups.add(new Tuple<> (sigGroup.getId(), sigGroup.getSignalSystemId()));
				}
			}
		}
		for (Tuple<Id<SignalGroup>, Id<SignalSystem>> groupSystemTuple : emptyGroups){
			signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(groupSystemTuple.getSecond()).remove(groupSystemTuple.getFirst());
			for(SignalPlanData sigPlanData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(groupSystemTuple.getSecond()).getSignalPlanData().values()){
				sigPlanData.getSignalGroupSettingsDataByGroupId().remove(groupSystemTuple.getFirst());
			}
		}
	}

	private void removeEmptyControlSettings() {
		SignalControlData control = this.signalsData.getSignalControlData();
		for (SignalSystemControllerData  controller : control.getSignalSystemControllerDataBySystemId().values()) {
			Map<Id<SignalGroup>, SignalGroupData> signalGroups = this.signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(controller.getSignalSystemId());
			Iterator<Entry<Id<SignalPlan>, SignalPlanData>> planIterator = controller.getSignalPlanData().entrySet().iterator();
			while (planIterator.hasNext()) {
				SignalPlanData plan = planIterator.next().getValue();
				if (plan.getSignalGroupSettingsDataByGroupId() != null) {
					Iterator<Entry<Id<SignalGroup>, SignalGroupSettingsData>> planSettingIterator = plan.getSignalGroupSettingsDataByGroupId().entrySet().iterator();
					while (planSettingIterator.hasNext()) {
						SignalGroupSettingsData settings = planSettingIterator.next().getValue();
						if (!signalGroups.containsKey(settings.getSignalGroupId())) {
							// remove this setting
							planSettingIterator.remove();
						}
					}
				}
				if (plan.getSignalGroupSettingsDataByGroupId() == null) {
					// this check against null is again necessary, because by the step above plan could have become empty
					// remove this plan
					planIterator.remove();
				}
				
			}
		}
	}


	private void checkSignalToLinkMatching() {
		SignalSystemsData signalSystems = this.signalsData.getSignalSystemsData();
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				if (! this.network.getLinks().containsKey(signal.getLinkId())){
					log.error("Error: No Link for Signal: "); 
					log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
							+ " is located at Link Id: " + signal.getLinkId() + " but this link is not existing in the network!");
					this.malformedSignals.add(new Tuple<>(signal.getId(), system.getId()));
				}
			}
		}
		
	}

	private void checkSignalToLaneMatching() {
		SignalSystemsData signalSystems = this.signalsData.getSignalSystemsData();
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				if (signal.getLaneIds() != null && ! signal.getLaneIds().isEmpty()){
					if (! this.lanes.getLanesToLinkAssignments().containsKey(signal.getLinkId())){
						log.error("Error: No LanesToLinkAssignment for Signals:");
						log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
							+ " is located at some lanes of Link Id: " + signal.getLinkId() + " but there is no LanesToLinkAssignemt existing in the LaneDefinitions.");
						malformedSignals.add(new Tuple<>(signal.getId(), system.getId()));
					}
					else {
						LanesToLinkAssignment l2l = this.lanes.getLanesToLinkAssignments().get(signal.getLinkId());
						Iterator<Id<Lane>> signalLaneIterator = signal.getLaneIds().iterator();
						while (signalLaneIterator.hasNext()) {
							Id<Lane> laneId = signalLaneIterator.next();
							if (! l2l.getLanes().containsKey(laneId)) {
								log.error("Error: No Lane for Signal: "); 
								log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
										+ " is located at Link Id: " + signal.getLinkId() + " at Lane Id: " + laneId + " but this link is not existing in the network!");
								signalLaneIterator.remove();
							}
						}
						// TODO jetzt könnten mehrere signals für einen link ohne lanes existieren - was dann?
					}
				}
			}
		}
	}

}
