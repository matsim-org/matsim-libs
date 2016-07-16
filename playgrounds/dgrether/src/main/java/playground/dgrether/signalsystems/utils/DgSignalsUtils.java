/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalsUtils
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
package playground.dgrether.signalsystems.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalSystemControllerDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class DgSignalsUtils {
	
	public static SignalPlanData copySignalPlanData(final SignalPlanData signalPlan, SignalControlDataFactory factory) {
		return copySignalPlanData(signalPlan, signalPlan.getId(), factory);
	}
	
	
	public static SignalPlanData copySignalPlanData(final SignalPlanData signalPlan, Id<SignalPlan> planId, SignalControlDataFactory factory) {
		SignalPlanData newPlan = factory.createSignalPlanData(planId);
		newPlan.setCycleTime(signalPlan.getCycleTime());
		newPlan.setEndTime(signalPlan.getEndTime());
		newPlan.setStartTime(signalPlan.getStartTime());
		newPlan.setOffset(signalPlan.getOffset());
		for (SignalGroupSettingsData settings : signalPlan.getSignalGroupSettingsDataByGroupId().values()) {
			SignalGroupSettingsData newSettings = factory.createSignalGroupSettingsData(settings.getSignalGroupId());
			newSettings.setDropping(settings.getDropping());
			newSettings.setOnset(settings.getOnset());
			newPlan.addSignalGroupSettings(newSettings);
		}
		return newPlan;
	}

	
	/**
	 * @return Map with the signal system Id as key and a List with Node Ids as values
	 */
	public static Map<Id<SignalSystem>, Set<Id<Node>>>calculateSignalizedNodesPerSystem(SignalSystemsData signalSystemsData, Network net){
		Map<Id<SignalSystem>, Set<Id<Node>>> signalizedNodesPerSystem = new HashMap<>();
		Set<Id<Node>> nodes;
		Link link;
		Node node;
		for (SignalSystemData ss : signalSystemsData.getSignalSystemData().values()){
			nodes = new HashSet<>();
			signalizedNodesPerSystem.put(ss.getId(), nodes);
			for (SignalData signal : ss.getSignalData().values()){
				Id<Link> linkId = signal.getLinkId();
				link = net.getLinks().get(linkId);
				node = link.getToNode();
				nodes.add(node.getId());
			}
		}
		return signalizedNodesPerSystem;
	}
	
	/**
	 * @return a Set containing the Ids of all signalized Nodes in the network
	 */
	public static Set<Id<Node>> calculateSignalizedNodes(SignalSystemsData signalSystemsData, Network net) {
		Set<Id<Node>> signalizedNodeIds = new HashSet<>();
		for (Set<Id<Node>> set : DgSignalsUtils.calculateSignalizedNodesPerSystem(signalSystemsData, net).values()){
			signalizedNodeIds.addAll(set);
		}
		return signalizedNodeIds;
	}

	/**
	 * @param signalSystemsData 
	 * @return Map with the signal system Id as key and a List with Node Ids as values
	 */
	public static Set<Node> calculateSignalizedNodes4System(SignalSystemData ss, Network net){
		Set<Node> nodes = new HashSet<Node>();
		Link link;
		Node node;
		for (SignalData signal : ss.getSignalData().values()){
			Id<Link> linkId = signal.getLinkId();
			link = net.getLinks().get(linkId);
			node = link.getToNode();
			nodes.add(node);
		}
		return nodes;
	}

	public static SignalGroupSettingsData copySignalGroupSettingsData(final SignalGroupSettingsData signalGroupSettings, final SignalControlDataFactory fac){
		SignalGroupSettingsData newSettings = fac.createSignalGroupSettingsData(signalGroupSettings.getSignalGroupId());
		newSettings.setOnset(signalGroupSettings.getOnset());
		newSettings.setDropping(signalGroupSettings.getDropping());
		return newSettings;
	}

	
	
	/**
	 * 
	 * @return null if the signal is not attached to a group
	 */
	public static SignalGroupData getSignalGroup4SignalId(Id<SignalSystem> signalSystemId, Id<Signal> signalId, SignalGroupsData signalGroups){
		Map<Id<SignalGroup>, SignalGroupData> signalGroups4System = signalGroups.getSignalGroupDataBySystemId(signalSystemId);
		for (SignalGroupData group : signalGroups4System.values()){
			if (group.getSignalIds().contains(signalId)){
				return group;
			}
		}
		return null; 
	}
	
	public static Set<SignalData> getSignalDataOfSignalGroup(SignalSystemData signalSystem, SignalGroupData signalGroup) {
		Set<SignalData> signalSet = new HashSet<SignalData>();
		if (!signalSystem.getId().equals(signalGroup.getSignalSystemId())){
			throw new IllegalArgumentException("System Id: " + signalSystem.getId() + " is not equal to signal group Id: " + signalGroup.getId());
		}
		for (Id<Signal> signalId : signalGroup.getSignalIds()){
			SignalData signal = signalSystem.getSignalData().get(signalId);
			signalSet.add(signal);
		}
		return signalSet;
	}
	
	/**
	 * @param signalSystemsData 
	 * @return Map with the signal system Id as key and a List with Node Ids as values
	 */
	public static Map<Id<SignalSystem>, Set<Id<Link>>>calculateSignalizedLinksPerSystem(SignalSystemsData signalSystemsData){
		Map<Id<SignalSystem>, Set<Id<Link>>> signalizedLinksPerSystem = new HashMap<>();
		Set<Id<Link>> links;
		for (SignalSystemData ss : signalSystemsData.getSignalSystemData().values()){
			links = new HashSet<>();
			signalizedLinksPerSystem.put(ss.getId(), links);
			for (SignalData signal : ss.getSignalData().values()){
				Id<Link> linkId = signal.getLinkId();
				links.add(linkId);
			}
		}
		return signalizedLinksPerSystem;
	}
	
	public static Set<Id<Link>> calculateSignalizedLinkIds4SignalGroup(SignalSystemData system, SignalGroupData signalGroup){
		Set<Id<Link>> linkIds = new HashSet<>();
		if (!system.getId().equals(signalGroup.getSignalSystemId())){
			throw new IllegalArgumentException("System Id: " + system.getId() + " is not equal to signal group Id: " + signalGroup.getId());
		}
		for (Id<Signal> signalId : signalGroup.getSignalIds()){
			SignalData signal = system.getSignalData().get(signalId);
			linkIds.add(signal.getLinkId());
		}
		return linkIds;
	}

	
	
	public static boolean signalSystemHasLanes(SignalSystemData signalSystem){
		for (SignalData signal : signalSystem.getSignalData().values()){
			if (!(signal.getLaneIds() == null || signal.getLaneIds().isEmpty())){
				return false;
			}
		}
		return true;
	}
	
	public static int calculateGreenTimeSeconds(SignalGroupSettingsData settings, int cylceTimeSeconds){
		int on = settings.getOnset();
		int off = settings.getDropping();
		int green = 0;
		if (on < off){
			green = off - on;
		}
		else {
			green = off + cylceTimeSeconds - on;
		}
		return green;
	}
	
	public static SignalControlData copySignalControlData(SignalControlData signalControlData){
		
		SignalControlData newSignalControl = new SignalControlDataImpl();
		
		for (SignalSystemControllerData oldSignalSystemControl : signalControlData.getSignalSystemControllerDataBySystemId().values()){
			SignalSystemControllerData newSignalSystemControl = new SignalSystemControllerDataImpl(oldSignalSystemControl.getSignalSystemId());
			newSignalSystemControl.setControllerIdentifier(oldSignalSystemControl.getControllerIdentifier());
			
			for (SignalPlanData oldSignalPlan : oldSignalSystemControl.getSignalPlanData().values()){
				SignalPlanData newSignalPlan = copySignalPlanData(oldSignalPlan, signalControlData.getFactory());
				newSignalSystemControl.addSignalPlanData(newSignalPlan);
			}
		}
		
		return newSignalControl;
	}
	
}
